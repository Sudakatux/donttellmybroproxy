
(ns donttellmybro-proxy.core

  (:require [clojure.pprint :as pprint]
            [org.httpkit.client :as http]
            [clojure.walk :refer [stringify-keys]]
            [donttellmybro-proxy.state :refer [custom-headers main-host]])
  (:use org.httpkit.server)
  (:gen-class))


(defn build-headers [existing] ;http-kit seems to decompress for us
  (->>
   @custom-headers
   (merge (dissoc existing :content-encoding))
   stringify-keys))


(defn handle-response [res]
  (let [{:keys [body headers status]} res]
     {
     :status status
     :headers (build-headers headers)
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
         :or {host @main-host}} req]
        (->>  @(http/request {:url (build-url host uri query-string)
                    :method request-method
                    :body body
                    :headers (dissoc headers "content-length")
                              :as :stream})
              (handle-response))))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server []
  (reset! server (run-server #'handler {:port 9090})))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Starting Server!")
  (start-server)
 )

