(ns donttellmybroproxy.proxy
  (:import
    [java.net URI])
  (:require
    [clj-http.client :refer [request]]
    [clojure.string :as clj-str]
    [ring.adapter.jetty :refer [run-jetty]]
    [clj-http.cookies :refer [wrap-cookies]]
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

(defn- matches-url? [pattern url]
  "Tests if pattern matches url"
  (-> pattern
      re-pattern
      (re-matches url)
      some?))

(defn get-matchers-matching-url [interceptors url]
  "Returns a vector with matching interceptors"
  (filter #(matches-url? (key %) url) interceptors))

(defn apply-interceptor [response interceptor]
  (cond-> response
          (contains? interceptor :headers) (assoc :headers (merge (:headers response) (get interceptor :headers)))
          (contains? interceptor :body) (merge {:body (ByteArrayInputStream. (condp = (type (get interceptor :body))
                                                                               java.lang.String (.getBytes (get interceptor :body))
                                                                               (get interceptor :body))
                                                                             )})))

(defn extract-interceptor-for-type [interceptors type]
  "Will return interceptor without the matching key"
  (map (fn [el] (get el type)) interceptors))

(defn interceptors-for-method [interceptors method]
  "Takes the interceptors and looks for all interceptor that match
  either the method or the :all key"
  (->> interceptors
       vals
       (map #((comp vals select-keys) % [:all method]))
       flatten))

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

(defn format-body-if-possible [type-recorded-element]
  (let [existing-headers (get type-recorded-element :headers {})
        lower-case-headers (into {} (for [[k v] existing-headers] [(.toLowerCase k) v]))
        content-type (get lower-case-headers "content-type" "")
        body-byte-array (get type-recorded-element :body (.getBytes ""))]
    (letfn [(is-stringable? [ctype]
              (clj-str/includes? content-type ctype)
              )]
      (if (some is-stringable? ["application/json"
                                "application/xml"
                                "application/javascript"
                                "application/xhtml+xml"
                                "application/x-www-form-urlencoded"
                                "text"])
        (String. body-byte-array)
        body-byte-array
        ))))

(defn recorded-element->interceptor [recordElement elementIdx]
  "Takes a recordElement and a target index. Returns recorded element as an interceptor"
  (let [element-as-interceptor (nth (get recordElement :recorded) elementIdx)
        url-difference (clj-str/replace (:url element-as-interceptor) (get recordElement :base-url) "")
        method (:method element-as-interceptor)
        response-recorded-element (get element-as-interceptor :response)]
    {(str ".*\\Q" url-difference "\\E$") {method {
                                        :response (assoc response-recorded-element :body (format-body-if-possible response-recorded-element))
                                        }}}))

(defn record! [response request proxy-path base-url record?]
  (if record?
    (let [response-body (slurp-bytes (:body response))
          url (:url request)
          method (:method request)
          body (:body request)]
      (add-recording! {
                       :url      url
                       :method   method
                       :body     body
                       :response (select-keys (merge response {:body response-body})
                                              [:headers :status :body]
                                              ;[:cached :request-time :repeatable? :protocol-version :chunked? :cookies :reason-phrase :headers :status :length :body]
                                              )
                       } proxy-path base-url)
      (merge response {:body (ByteArrayInputStream. response-body)}))
    response))

(defn maybe-request [request request-fn make-request]
  (if make-request
    (request-fn request)
    {:body ""
     :headers {}}
    ))

(defn wrap-proxy
  "Proxies requests from proxied-path, a local URI, to the remote URI at
  remote-base-uri, also a string."
  [handler ^String proxied-path remote-base-uri & [http-opts]]
  (wrap-cookies
    (fn [req]
      (if (.startsWith ^String (:uri req) proxied-path)
        (let [rmt-full (URI. (str remote-base-uri "/"))
              rmt-path (URI. (.getScheme rmt-full)
                             (.getAuthority rmt-full)
                             (.getPath rmt-full) nil nil)
              lcl-path (URI. (subs (:uri req) (.length proxied-path)))
              remote-uri (.resolve rmt-path lcl-path)
              url (str remote-uri (if (:query-string req) (str "?" (:query-string req)) ""))
              method (:request-method req)
              original-request {:method           method
                                :url              url
                                :headers          (dissoc (:headers req) "host" "content-length")
                                :body             (if-let [len (get-in req [:headers "content-length"])]
                                                    (slurp-binary (:body req) (Integer/parseInt len)))
                                :follow-redirects true
                                :throw-exceptions false
                                :as               :stream}
              interceptors-for-method-url (-> http-opts
                                              (get :interceptors) ;Gets all interceptors
                                              (get-matchers-matching-url url) ; Filters by matchers matching url
                                              (interceptors-for-method method)) ; Filters by methods and :all
              ]
          (-> original-request
              (apply-interceptors (extract-interceptor-for-type interceptors-for-method-url :request)) ; Applys request interceptors
              (maybe-request request (get http-opts :make-request?))                                       ; Performs actual request
              (record! original-request proxied-path remote-base-uri (:record? http-opts)) ; if present records request and response... and returns unmodified response --> Side Effect
              (apply-interceptors (extract-interceptor-for-type interceptors-for-method-url :response)) ;Applys response interceptors
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
  (assoc-in proxy-configuration [:args] {:request  {}
                                         :response {}
                                         :record?  false
                                         :make-request? true}))

(defn add-proxy [key & args]
  (swap! registered-proxies assoc key (->> args
                                           (zipmap [:route :url :args])
                                           prepare-default-args)))

(defn remove-proxy [key]
  (swap! registered-proxies dissoc key))

(defn clear-proxies []
  (reset! registered-proxies {}))

(defn interceptors-for-id [id]
  (get-in @registered-proxies [id :args :interceptors] {}))

(defn get-existing-interceptors-for-current-key-type-matcher-method [current key type matcher method]
  "Returns a request/response(type) interceptor for a given matcher method"
  (get-in current [key :args :interceptors matcher method type]))

(defn existing-interceptors [key type matcher method]
  "Returns a map with existing headers for proxy [key] for type [request|response]
  method :get :post :put :options :all"
  (get-existing-interceptors-for-current-key-type-matcher-method @registered-proxies key type matcher method))

(defn route-by-id [id]
  "Given an id returns the route"
  (get-in @registered-proxies [id :route]))

(defn recorded-element-by-route [route]
  "Takes a route string returns recordings vector"
  (get-in @recordings [:recordings route]))

(defn recordings-from-recorded-element [recorded-element]
  "Takes a route string returns recordings vector"
  (get recorded-element :recorded))

(defn recorded-element-by-id [id]
  "Takes an id returns recorded element"
  (-> id
      route-by-id
      recorded-element-by-route))

(defn recordings-by-id [id]
  "Takes an id returns recordings vector"
  (-> id
      recorded-element-by-id
      recordings-from-recorded-element))

(defn clean-recordings-for-route [recording-collection route]
  (update-in recording-collection [:recordings route] assoc :recorded []))

(defn clean-recordings-for-route! [route]
  (swap! recordings clean-recordings-for-route route))

(defn clean-recordings-for-id! [id]
  (-> id
      route-by-id
      clean-recordings-for-route!))



(defn is-recording? [id]
  (get-in @registered-proxies [id :args :record?]))

(defn remove-header-from-map [current key type matcher method header-key]
  (update-in current [key :args :interceptors matcher method type :headers] dissoc header-key))

(defn remove-header! [key type matcher method header-key]
  "Removed the header from state"
  (swap! registered-proxies remove-header-from-map key type matcher method header-key))


;(defn update-header-if-present [])

(defmulti interceptor-merge-strategy (fn [_ b] (first (keys b))))

(defmethod interceptor-merge-strategy :body
  [a b] (-> a
            (merge b)
            (interceptor-merge-strategy {:headers {"Content-Length" (str (count (get b :body))  )}})))

(defmethod interceptor-merge-strategy :headers
  [a b] (merge-with into a b))

(defmethod interceptor-merge-strategy :default
  [a b] (merge-with into a b))

(defn update-type-interceptors! [type key interceptor-args matcher method]
  "Takes the type the key the matcher and the interceptor arguments and creates/replace an interceptor"
  (swap! registered-proxies assoc-in
         [key :args :interceptors matcher method type]
         (interceptor-merge-strategy (existing-interceptors key type matcher method) interceptor-args)))

(defn list-proxies []
  (->> @registered-proxies
       (map (fn [[k v]] [k (assoc v :recordings (recordings-by-id k))]))
       (into {})))

;TO-BE Tested
(defn merge-to-existing-interceptors-in-proxy-list [proxy-list key interceptors]
  (update-in proxy-list [key :args :interceptors] merge-with into (get-in proxy-list [key :args :interceptors])
             interceptors))


;TODO extract swap and add tests +1
(defn merge-to-existing-interceptors! [key interceptors]
  (swap! registered-proxies assoc-in [key :args :interceptors]
         (merge-with into (get-in @registered-proxies [key :args :interceptors])
                interceptors)))

(defn create-an-interceptor-from-recording-idx! [key recording-idx]
  (merge-to-existing-interceptors! key
                                   (recorded-element->interceptor (recorded-element-by-id key) recording-idx)))

(defn start-recording! [key]
  (swap! registered-proxies assoc-in [key :args :record?] true))

(defn stop-recording! [key]
  (swap! registered-proxies assoc-in [key :args :record?] false))

(defn update-make-request [key make-request?]
  (swap! registered-proxies assoc-in [key :args :make-request?] make-request?))

(def myapp
  (-> (constantly {
                   :status  404
                   :headers {}
                   :body    "404 - no route set for this path"
                   })
      wrap-dynamic
      wrap-reload))

(defn server ([] (server 3000))
  ([port] (let [running-server (run-jetty
                                 #'myapp                    ; Just changes here
                                 {:port port :join? false})]
            running-server)))
