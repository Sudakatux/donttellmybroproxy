(ns donttellmybro-proxy.interactive-server
  (:require [clojure.pprint :as pp]
            [donttellmybro-proxy.state :refer [custom-headers main-host add-header! update-host!]]
            [clojure.walk :refer [stringify-keys]]
            [muuntaja.core :as m])
  (:use [compojure.route :only [files not-found]]
        [compojure.core :only [defroutes GET PUT DELETE ANY context]]
        org.httpkit.server))

(def m (m/create))

(defn show-header-rules [_]
  {:status 200
   :headers {"Content-type" "application/json"}
   :body (m/encode  m "application/json" @custom-headers)
   })

(defn show-host [_]
  {:status 200
   :headers {"Content-type" "application/json"}
   :body (m/encode m "application/json" (assoc {} :host @main-host))
   })

(defn modify-headers! [req]
  (->> req
       :body
       (m/decode m "application/json")
       stringify-keys
       (swap! custom-headers merge )
       (show-header-rules)))

(defn modify-host! [req]
  (->> req
       :body
       (m/decode m "application/json")
       :host
       (reset! main-host)
       (show-host)))

(defroutes all-routes
  (GET "/api/headers" [] show-header-rules)
  (GET "/api/host" [] show-host)
  (PUT "/api/headers" [] modify-headers!)
  (PUT "/api/host" [] modify-host!))

(defonce server (atom nil))

(defn start-interactive-server [port]
  (reset! server (run-server #'all-routes {:port port})))




