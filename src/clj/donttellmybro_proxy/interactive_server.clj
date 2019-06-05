(ns donttellmybro-proxy.interactive-server
  (:require [clojure.pprint :as pp]
            [donttellmybro-proxy.state :refer [params update-host! merge-headers!]]
            [clojure.walk :refer [stringify-keys]]
            [compojure.route :as route]
            [muuntaja.core :as m])
  (:use [compojure.core :only [defroutes GET PUT DELETE ANY context]]
        org.httpkit.server))

(def m (m/create))
(def response-type "application/json")



(defn show-header-rules []
  {:status 200
   :headers {"Content-type" response-type}
   :body (m/encode  m response-type (:headers @params))
   })

(defn show-host []
  {:status 200
   :headers {"Content-type" response-type}
   :body (m/encode m response-type (select-keys @params [:host]))
   })

(defn show-params [_]
  {:status 200
   :headers {"Content-type" response-type}
   :body (m/encode  m response-type @params)
   })


(defn modify-headers! [req]
  (->> req
       :body
       (m/decode m response-type)
       stringify-keys
       merge-headers!)
  (show-header-rules))


(defn modify-host! [req]
  (->> req
       :body
       (m/decode m response-type)
       :host
       update-host!)
  (show-host))
 

(defroutes all-routes
  (route/resources "/")
  (GET "/api/headers" [] show-header-rules)
  (GET "/api/host" [] show-host)
  (GET "/api/params" [] show-params)
  (PUT "/api/headers" [] modify-headers!)
  (PUT "/api/host" [] modify-host!))

(defonce server (atom nil))

(defn start-interactive-server [port]
  (reset! server (run-server #'all-routes {:port port})))




