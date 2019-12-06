(ns donttellmybroproxy.proxy
  (:import
    [java.net URI] )
  (:require
    [clj-http.client         :refer [request]]
    [clojure.string          :refer [join split]]
    [ring.adapter.jetty      :refer [run-jetty]]
    [clj-http.cookies        :refer [wrap-cookies]]
    [ring.middleware.reload :refer [wrap-reload]]
    )
  (:import (java.io ByteArrayInputStream)))

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

(defn debug-interceptor [param]
  (clojure.pprint/pprint param)
  param)

(defn apply-response-args [response config]
  (clojure.pprint/pprint response)
  (clojure.pprint/pprint config)
  (merge-with into response config))

(defn match-interceptor [url interceptors]
  (let [interceptor-matchers (keys interceptors)
      matches-url (filter #(re-matches (re-pattern %) url) interceptor-matchers)]
  (first matches-url)))

;; TODO move me to another namespace
(defmacro if-let*
  ([bindings-vec then] `(if-let* ~bindings-vec ~then nil))
  ([bindings-vec then else]
   (if (seq bindings-vec)
     `(let ~bindings-vec
        (if (and ~@(take-nth 2 bindings-vec))
          ~then
          ~else)))))


(defn apply-interceptors [response url interceptors type]
  "Takes the handler ring response the url interceptors and interceptor type (request | response)
  and returns the new response"
  (if-let* [matcher-key (match-interceptor url interceptors)
           replacements (get-in interceptors [matcher-key type] )]
    (cond-> response
            (contains? replacements :headers) (assoc :headers (merge (:headers response) (get-in interceptors [matcher-key type :headers])) )
            (contains? replacements :body) (merge {:body (ByteArrayInputStream. (.getBytes (get-in interceptors [matcher-key type :body])))}))
      response))

(defn apply-merge-in-body [response body-param]
  (merge response body-param))


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
              url (str remote-uri "?" (:query-string req))]
              (-> (merge {:method (:request-method req)
                               :url url
                               :headers (dissoc (:headers req) "host" "content-length")
                               :body (if-let [len (get-in req [:headers "content-length"])]
                                       (slurp-binary (:body req) (Integer/parseInt len)))
                               :follow-redirects true
                               :throw-exceptions false
                               :as :stream})               ; TODO merging should be controlled (:request http-opts)
                       request
                        (apply-interceptors url (get http-opts :interceptors) :response)
                        debug-interceptor  ; TODO interception for response should come here
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
  (get-in current [key :args :interceptors matcher type]))

(defn existing-interceptors [key type matcher]
  "Returns a map with existing headers for proxy [key] for type [request|response]"
  (extract-existing-interceptors (list-proxies) key type matcher))

(defn update-request-interceptors! [key interceptor-args matcher]
  (swap! registered-proxies assoc-in
         [key :args :interceptors matcher :request]
         (merge-with into (existing-interceptors key :request matcher) interceptor-args)))

(defn update-response-interceptors! [key interceptor-args matcher]
  (swap! registered-proxies assoc-in
         [key :args :interceptors matcher :response]
         (merge-with into (existing-interceptors key :response matcher) interceptor-args)))


(def myapp
  (-> (constantly {:status 404 :headers {} :body "404 - not found"})
      wrap-dynamic
      wrap-reload))

(defn server ([] (server 3000))
  ([port] (let [running-server (run-jetty
                                 #'myapp                      ; Just changes here
                                 {:port port :join? false})]
            running-server
            )))
