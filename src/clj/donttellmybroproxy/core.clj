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
;TODO endpoint paths are in a confusing order refactor proxy-server/:id/thenTheAction
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
    ["/proxy-server/:type/headers/:id"
     {
      :post
      (fn [{{:keys [matcher header-key header-value]} :body-params
            {:keys [type id]} :path-params}]
        (proxy/update-type-interceptors! (keyword type) (keyword id) {:headers {header-key header-value}} matcher)
        (response/ok (proxy/existing-interceptors (keyword id) (keyword type) matcher))
        )
      :delete
      (fn [{{:keys [matcher header-key]} :body-params
            {:keys [type id]} :path-params}]
        (proxy/remove-header! (keyword id) (keyword type) matcher header-key)
        (response/ok (proxy/existing-interceptors (keyword id) (keyword type) matcher))
        )
      }
     ]

    ["/proxy-server/:type/body/:id"
     {
      :post
      (fn [{{:keys [matcher body]} :body-params
            {:keys [type id]} :path-params}]
        (proxy/update-type-interceptors! (keyword type) (keyword id)  {:body body} matcher)
        (response/ok {:list (proxy/existing-interceptors (keyword id) :response matcher) } )
        )
      }
     ]
    ["/proxy-server/record/:id"
     {
      :post
      (fn [{{:keys [record?]} :body-params
            {:keys [id]} :path-params}]
        (if record?
            (proxy/start-recording! (keyword id))
            (proxy/stop-recording! (keyword id)))
        (response/ok {
                      :record? (proxy/is-recording? (keyword id))
                      :recordings (proxy/recordings-by-id (keyword id))
                      }))
      }
     ]
    ["/proxy-server/:id/recordings/:recording_idx/to_interceptor"
     {
      :post
      (fn [{{:keys [id recording_idx]} :path-params}]
        (proxy/create-an-interceptor-from-recording-idx! (keyword id) (Integer/parseInt recording_idx))
        (response/ok {:interceptors (proxy/interceptors-for-id (keyword id))}))
      }
     ]
    ["/proxy-server/:id/interceptors/download"
     {:get
      (fn [{{:keys [id]} :path-params}]
        {:status 200
         :headers {
                   "content-type" "application/edn"
                   "Content-Disposition" (str "attachment; filename=\"" id ".edn\"" )
                   }
         :body (prn-str  {:interceptors (proxy/interceptors-for-id (keyword id))} )
         })
      }]
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

