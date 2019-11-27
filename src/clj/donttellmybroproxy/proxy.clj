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
  ;(clojure.pprint/pprint (slurp (:body param)))
  param)

(defn apply-response-args [response config]
  (merge-with into response config))

(defn match-interceptor [url interceptors]
  (let [interceptor-matchers (keys interceptors)
      matches-url (filter #(re-matches (re-pattern %) url) interceptor-matchers)]
  (first matches-url)))

(defn create-body-interceptors [url interceptors]
  (if-let [matcher-key (match-interceptor url interceptors)]
      {:body (ByteArrayInputStream. (.getBytes (get interceptors matcher-key)) )  }
      {}))

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
                               :as :stream} (:request http-opts))               ; TODO merging should be controlled
                       request
                        (apply-response-args (:response http-opts))
                        (apply-merge-in-body (create-body-interceptors url (get-in http-opts [:interceptors :response])))
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
                                          prepare-default-args)))                ;

(defn remove-proxy [key]
  (swap! registered-proxies dissoc key))

(defn clear-proxies []
  (reset! registered-proxies {}))

(defn list-proxies []
  @registered-proxies)

;; TODO We can use a protocol to manage this updates
(defn existing-headers [key type]
  "Returns a map with existing headers for proxy [key] for type [request|response]"
  (get-in (list-proxies) [key :args type :headers]))

(defn update-request-headers! [key header-args]
  (swap! registered-proxies assoc-in [key :args :request :headers] (merge (existing-headers key :request)  header-args)))

(defn update-response-headers! [key header-args]
  (swap! registered-proxies assoc-in [key :args :response :headers] (merge (existing-headers key :response)  header-args)))

;;; Interceptors
(defn existing-interceptors [key type]
  "Returns a map with existing headers for proxy [key] for type [request|response]"
  (get-in (list-proxies) [key :args :interceptors type]))

(defn update-request-interceptors! [key interceptor-args]
  (swap! registered-proxies assoc-in [key :args :interceptors :request] (merge (existing-interceptors key :request)  interceptor-args)))

(defn update-response-interceptors! [key interceptor-args]
  (swap! registered-proxies assoc-in [key :args :interceptors :response ] (merge (existing-interceptors key :response)  interceptor-args)))


(def myapp
  (-> (constantly {:status 404 :headers {} :body "404 - not found"})
      ; (wrap-proxy listen-path remote-uri http-opts)
      wrap-dynamic
      wrap-reload
      ))

(defn server ([] (server 3000))
  ([port] (let [running-server (run-jetty
                                 #'myapp
                                 {:port port :join? false})]
            running-server
            ))
  )

(defn -main
  []
  ;[listen-path listen-port remote-uri http-opts]
  (println "Hi"))
