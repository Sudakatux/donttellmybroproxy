(ns donttellmybro-proxy.interactive-server
  (:require [clojure.pprint :as pp]
            [donttellmybro-proxy.state :refer [params update-host! merge-headers! remove-header!]]
            [clojure.walk :refer [stringify-keys]]
            [compojure.route :as route]
            [muuntaja.core :as m]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response])
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

(defn remove-header-key! [header-key]
  (->> header-key
       remove-header!)
  (show-header-rules))


(defn modify-host! [req]
  (->> req
       :body
       (m/decode m response-type)
       :host
       update-host!)
  (show-host))
 

(defroutes all-routes
 (route/resources "/" {:root "public"})
 ;; NOTE: this will deliver your index.html
 (GET "/" [] (-> (response/resource-response "index.html" {:root "public"})
                 (response/content-type "text/html")))
  (GET "/api/headers" [] show-header-rules)
  (GET "/api/host" [] show-host)
  (GET "/api/params" [] show-params)
  (PUT "/api/headers" [] modify-headers!)
  (PUT "/api/host" [] modify-host!)
  (DELETE "/api/headers/:header_key" [header_key] (remove-header-key! header_key)))

(defonce server (atom nil))

(def dev-app (wrap-reload (wrap-defaults #'all-routes (assoc-in site-defaults [:security :anti-forgery] false))))

(defn start-interactive-server [port]
  (reset! server (run-server #'dev-app {:port port})))




