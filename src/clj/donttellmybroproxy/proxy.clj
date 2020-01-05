(ns donttellmybroproxy.proxy
  (:import
    [java.net URI] )
  (:require
    [clj-http.client         :refer [request]]
    [clojure.string          :as clj-str]
    [ring.adapter.jetty      :refer [run-jetty]]
    [clj-http.cookies        :refer [wrap-cookies]]
    [ring.middleware.reload :refer [wrap-reload]]
    )
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))

(defn prepare-cookies
  "Removes the :domain and :secure keys and converts the :expires key (a Date)
  to a string in the ring response map resp. Returns resp with cookies properly
  munged."
  [resp]
  (let [prepare #(-> (update-in % [1 :expires] str)
                     (update-in [1] dissoc :domain :secure))]
    (assoc resp :cookies (into {} (map prepare (:cookies resp))))))

(defn slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.read rdr buf)
      buf)))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))

(defn debug-interceptor [param]
  (clojure.pprint/pprint param)
  param)

(defn apply-response-args [response config]
  (merge-with into response config))

(defn get-matchers-matching-url [url interceptors]
  "Returns a vector with matching interceptors"
  (filter #(re-matches (re-pattern (key %)) url) interceptors))

(defn apply-interceptor [response interceptor]
           (cond-> response
                   (contains? interceptor :headers) (assoc :headers (merge (:headers response) (get interceptor :headers)))
                   (contains? interceptor :body) (merge {:body (ByteArrayInputStream. (condp = (type (get interceptor :body))
                                                                                        java.lang.String  (.getBytes (get interceptor :body))
                                                                                        (get interceptor :body))
                                                                 )})))

(defn extract-interceptor-for-type [interceptors type]
  "Will return interceptor without the matching key"
  (map (fn [el] (get (val el) type)) interceptors))

(defn apply-interceptors [response interceptors]
  "Takes a request and a list of interceptors and apply them in order"
  (reduce apply-interceptor response interceptors))

(defn apply-merge-in-body [response body-param]
  (merge response body-param))

(def recordings (atom {}))

(defn current-recordings [proxy-path]
  (get-in @recordings [:recordings proxy-path :recorded] []))

(defn add-recording! [recording proxy-path base-url]
  (swap! recordings assoc-in [:recordings proxy-path] {:base-url base-url
                                                       :recorded (conj (current-recordings proxy-path) recording)}))

(defn toInterceptor [recordedElement elementIdx]
  (let [element-to-interceptor (nth (get recordedElement :recorded) elementIdx)
        url-difference (clj-str/replace (:url element-to-interceptor) (get recordedElement :base-url) "")]
    {(str ".*" url-difference) {:response (:response element-to-interceptor)}}))

(defn record! [response request proxy-path base-url record?]
  (if record?
    (let [response-body (slurp-bytes (:body response))
          url (:url request)
          method (:method request)
          body (:body request)]
      (add-recording! {
                       :url url
                       :method method
                       :body body
                       :response (select-keys (merge response {:body response-body})
                                              [:cached :request-time :repeatable? :protocol-version :chunked? :cookies :reason-phrase :headers :status :length :body])
                       } proxy-path base-url)
      (merge response {:body (ByteArrayInputStream. response-body)}) )
    response))

(defn wrap-proxy
  "Proxies requests from proxied-path, a local URI, to the remote URI at
  remote-base-uri, also a string."
  [handler ^String proxied-path remote-base-uri & [http-opts]]
  (wrap-cookies
    (fn [req]
      (if (.startsWith ^String (:uri req) proxied-path)
        (let [rmt-full   (URI. (str remote-base-uri "/"))
              rmt-path   (URI. (.getScheme    rmt-full)
                               (.getAuthority rmt-full)
                               (.getPath      rmt-full) nil nil)
              lcl-path   (URI. (subs (:uri req) (.length proxied-path)))
              remote-uri (.resolve rmt-path lcl-path)
              url (str remote-uri (if (:query-string req) (str "?" (:query-string req)) ""))
              original-request {:method (:request-method req)
                                :url url
                                :headers (dissoc (:headers req) "host" "content-length")
                                :body (if-let [len (get-in req [:headers "content-length"])]
                                        (slurp-binary (:body req) (Integer/parseInt len)))
                                :follow-redirects true
                                :throw-exceptions false
                                :as :stream}
              ]
              (-> original-request
                  (apply-interceptors (extract-interceptor-for-type (get-matchers-matching-url url (get http-opts :interceptors)) :request))
                  request
                  (record! original-request proxied-path remote-base-uri (:record? http-opts))
                  (apply-interceptors
                    (extract-interceptor-for-type
                      (get-matchers-matching-url url (get http-opts :interceptors)) :response))
                  ;debug-interceptor
                       prepare-cookies))
        (handler req)))))

(def registered-proxies (atom {}))

(defn params-to-args []
  (map vals (vals @registered-proxies)))

(defn wrap-dynamic [handler]
(fn [req]
  ((->>
     (params-to-args)
     (reduce #(apply wrap-proxy %1 %2) handler)) req)))

(defn prepare-default-args [proxy-configuration]
  (assoc-in proxy-configuration [:args] {:request {}
                     :response {}}))

(defn add-proxy [key & args]
  (swap! registered-proxies assoc key (->> args
                                          (zipmap [:route :url :args])
                                          prepare-default-args)))
(defn remove-proxy [key]
  (swap! registered-proxies dissoc key))

(defn clear-proxies []
  (reset! registered-proxies {}))

(defn list-proxies []
  @registered-proxies)

(defn extract-existing-interceptors [current key type matcher]
  "Returns a request/response(type) interceptor for a given matcher matcher"
  (get-in current [key :args :interceptors matcher type]))

(defn existing-interceptors [key type matcher]
  "Returns a map with existing headers for proxy [key] for type [request|response]"
  (extract-existing-interceptors (list-proxies) key type matcher))

(defn route-by-id [id]
  "Given an id returns the route"
  (get-in @registered-proxies [id :route]))

(defn recordings-by-route [route]
  "Takes a route string returns recordings vector"
  (get-in @recordings [:recordings route :recorded]))

(defn recordings-by-id [id]
  "Takes an id returns recordings vector"
  (-> id
      route-by-id
      recordings-by-route))

(defn is-recording? [id]
  (get-in @registered-proxies [id :args :record?]))

(defn remove-header-from-map [current key type matcher header-key]
  (update-in current [key :args :interceptors matcher type :headers] dissoc header-key))

(defn remove-header! [key type matcher header-key]
  "Removed the header from state"
  (swap! registered-proxies remove-header-from-map key type matcher header-key))

(defn update-type-interceptors! [type key interceptor-args matcher]
  (swap! registered-proxies assoc-in
         [key :args :interceptors matcher type]
         (merge-with into (existing-interceptors key type matcher) interceptor-args)))

(defn start-recording! [key]
  (swap! registered-proxies assoc-in [key :args :record?] true))

(defn stop-recording! [key]
  (swap! registered-proxies assoc-in [key :args :record?] false))

(def myapp
  (-> (constantly {:status 404 :headers {} :body "404 - not found"})
      wrap-dynamic
      wrap-reload))

(defn server ([] (server 3000))
  ([port] (let [running-server (run-jetty
                                 #'myapp                      ; Just changes here
                                 {:port port :join? false})]
            running-server)))
