(ns donttellmybroproxy.core
  (:require
    [reitit.ring :as reitit]
    [ring.adapter.jetty :as jetty]
    [ring.util.http-response :as response]
    [ring.middleware.reload :refer [wrap-reload]]
    [muuntaja.middleware :as muuntaja]
    [donttellmybroproxy.proxy :as proxy] ))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Pragma"] "no-cache"))))

(defn wrap-formats [handler]
  (-> handler
      (muuntaja/wrap-format)))

(def routes
  [["/api"
    {:middleware [wrap-formats]}
    ["/start-proxy-server"
     {:put
      (fn [{{:keys [port] :or {port 3001}} :body-params}]
        (proxy/server port)
        (response/ok {:result "Proxy server started successfully"}))
      }]
    ["/create-proxy"
     {
     :put
     (fn [{{:keys [id route destination]} :body-params}]
       (proxy/add-proxy (keyword id)  route destination #{})
       (response/ok {:result (str id " was bound to route:" route " with destination: " destination)}))
      }]
    ["/remove-proxy/:id"
     {
      :delete
      (fn [{{:keys [id]} :path-params}]
        (proxy/remove-proxy (keyword id))
        (response/ok {:result (str "unbinded proxy for id:" id)})
        )
      }
     ]
    ["/list-proxy"
     {
      :get
      (fn [& args]
        (response/ok {:list (proxy/list-proxies)} ))
      }
     ]
    ]])

(def handler
  (reitit/routes
    (reitit/ring-handler
      (reitit/router routes))
    (reitit/create-resource-handler
      {:path "/"})
    (reitit/create-default-handler
      {:not-found
       (constantly (response/not-found "404 - Page not found"))
       :method-not-allowed
       (constantly (response/method-not-allowed "405 - Not allowed"))
       :not-acceptable
       (constantly (response/not-acceptable "406 - Not acceptable"))})))

(defn server ([] (server 3000))
  ([port] (jetty/run-jetty
            (-> #'handler
                wrap-nocache
                wrap-reload)
            {:port port :join? false})))

;(defn -main []
;  (jetty/run-jetty
;    (-> #'handler wrap-nocache wrap-reload)
;    {:port 3000
;     :join? false}))

