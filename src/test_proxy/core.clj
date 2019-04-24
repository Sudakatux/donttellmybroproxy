(ns test-proxy.core
  (:require [clojure.pprint :as pprint]
            [org.httpkit.client :as http]
            [clojure.walk :refer [stringify-keys]]
            )
  (:use org.httpkit.server)
  (:gen-class))

(def main-host "http://somehost.com")

(defn handle-response [res]
  (let [{:keys [body headers status]} res]
     {
     :status status
     :headers (stringify-keys (dissoc headers :content-encoding)) ;http-kit seems to decompress for us
     ;:headers {"Content-Type" "text/html"}
     :body body
     }))

(defn build-url [host path query-string]
  (let [url (str host path)]
    (if (not-empty query-string)
      (str url "?" query-string)
      url)))

(defn handler[req]
  (let [{:keys [host uri query-string request-method body headers]
         :or {host main-host}} req]
    ;(pprint req)
     (handle-response  @(http/request {:url (build-url host uri query-string)
                    :method request-method
                    :body body
                    :headers (dissoc headers "content-length")
                                                 :as :stream}))))

(defonce server (atom nil))
(defonce params (atom {}))

(defn stop-server []
  (when-not (nil? server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server []
  (reset! server (run-server #'handler {:port 9090})))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (pprint args)
  (println "Hello, World!")
 )

