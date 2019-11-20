(ns donttellmybroproxy.core
  (:require
    [reitit.ring :as reitit]
    [ring.adapter.jetty :as jetty]
    [ring.util.http-response :as response]
    [ring.middleware.reload :refer [wrap-reload]]
    [muuntaja.middleware :as muuntaja]
    [donttellmybroproxy.proxy :as proxy]
    [clojure.java.io :as io]
    [ring.util.response :as resp]
    [ring.middleware.content-type :as content-type])
  (:gen-class))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Pragma"] "no-cache"))))

(defn wrap-formats [handler]
  (-> handler
      (muuntaja/wrap-format)))


(def proxy-server (atom {:server-started false}))

(defn server-running? []
  (if-let [server-instance (:instance @proxy-server)]
    (.isStarted server-instance)
    false
    )
  )

(def routes
  [
   ["/api"
    {:middleware [wrap-formats]}
    ["/proxy-server/start"
     {:put
      (fn [{{:keys [port] :or {port 3001}} :body-params}]
        (if (server-running?)
          (response/conflict {:result "Server is already running"})
          (do
              (swap! proxy-server assoc :instance (proxy/server port))
              (response/no-content))))
      }]
    ["/proxy-server/stop"
     {:put
      (fn [& _]
        (if (server-running?)
          (do
            (.stop (:instance @proxy-server))
            (response/no-content))
          (response/conflict {:result "Server was not running"})))
      }]
    ["/proxy-server/status"
     {:get
      (fn [& _]
        (response/ok {:result {:server-running (server-running? )}}))
      }]
    ["/proxy-server/create"
     {
     :post
     (fn [{{:keys [id route destination]} :body-params}]
       (proxy/add-proxy (keyword id)  route destination #{})
       (response/ok {:list (proxy/list-proxies)} ))
      }]
    ["/proxy-server/delete/:id"
     {
      :delete
      (fn [{{:keys [id]} :path-params}]
        (proxy/remove-proxy (keyword id))
        (response/ok {:result (str "unbinded proxy for id:" id)})
        )
      }
     ]
    ;; TODO this two endpoints can be merged into one
    ["/proxy-server/request/headers/:id"
     {
      :post
      (fn [{{:keys [header-key header-value]} :body-params
            {:keys [id]} :path-params}]
        (proxy/update-request-headers! (keyword id) {header-key header-value})
        (response/ok {:list (proxy/existing-headers (keyword id) :request) } )
        )
      }
     ]
    ["/proxy-server/response/headers/:id"
     {
      :post
      (fn [{{:keys [header-key header-value]} :body-params
            {:keys [id]} :path-params}]
        (proxy/update-response-headers! (keyword id) {header-key header-value})
        (response/ok {:list (proxy/existing-headers (keyword id) :response) } )
        )
      }
     ]
    ["/proxy-server/response/header/:id"
     {
      :get
      (fn [{{:keys [id]} :path-params}]
        (response/ok {:list (proxy/existing-headers (keyword id) :response) } ))
      }
     ]
    ["/proxy-server/list"
     {
      :get
      (fn [& _]
        (response/ok {:list (proxy/list-proxies)} ))
      }
     ]

    ]
   ])

(def handler
  (reitit/routes
    (reitit/ring-handler
      (reitit/router routes))
    (reitit/create-resource-handler
      {:path "/*"})
    (reitit/create-default-handler
      {:not-found
       (-> (fn [request]
             (or (resp/resource-response (:uri request) {:root "public"})
                 (-> (resp/resource-response "index.html" {:root "public"})
                     (resp/content-type "text/html")))
             )
           content-type/wrap-content-type)
       :method-not-allowed
       (constantly (response/method-not-allowed "405 - Not allowed"))
       :not-acceptable
       (constantly (response/not-acceptable "406 - Not acceptable"))})
    {:conflicts (constantly nil)}
    ))

(defn server ([] (server 3000))
  ([port] (jetty/run-jetty
            (-> #'handler
                wrap-nocache
                wrap-reload)
            {:port port :join? false})))


(defn -main []
  (server))

