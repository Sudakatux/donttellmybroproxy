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
    [ring.middleware.content-type :as content-type]
    [reitit.ring.middleware.multipart :as multipart]
    [clojure.edn :as edn])
  (:gen-class))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Pragma"] "no-cache"))))

(defn wrap-formats [handler]
  (-> handler
      (muuntaja/wrap-format)))

; TODO associate port to status instead of flag
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
    {:middleware [wrap-formats
                  multipart/multipart-middleware]}
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
    ["/proxy-server/:id/delete"
     {
      :delete
      (fn [{{:keys [id]} :path-params}]
        (proxy/remove-proxy (keyword id))
        (response/ok {:result (str "unbinded proxy for id:" id)})
        )
      }
     ]
    ["/proxy-server/:id/headers/:type"
     {
      :post
      (fn [{{:keys [matcher header-key header-value method]} :body-params
            {:keys [type id]} :path-params}]
        (proxy/update-type-interceptors! (keyword type) (keyword id) {:headers {header-key header-value}} matcher (keyword method))
        (response/ok (proxy/existing-interceptors (keyword id) (keyword type) matcher (keyword method)))
        )
      :delete
      (fn [{{:keys [matcher header-key method]} :body-params
            {:keys [type id]} :path-params}]
        (proxy/remove-header! (keyword id) (keyword type) matcher (keyword method) header-key)
        (response/ok (proxy/existing-interceptors (keyword id) (keyword type) matcher (keyword method)))
        )
      }
     ]

    ["/proxy-server/:type/body/:id"
     {
      :post
      (fn [{{:keys [matcher body method]} :body-params
            {:keys [type id]} :path-params}]
        (proxy/update-type-interceptors! (keyword type) (keyword id)  {:body body} matcher (keyword method))
        (response/ok {:list (proxy/existing-interceptors (keyword id) :response matcher (keyword method)) } )
        )
      }
     ]
    ["/proxy-server/:id/record"
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
    ["/proxy-server/:id/should-make-request"
     {:put
      (fn [{{:keys [make-request?]} :body-params
            {:keys [id]} :path-params}]
        (proxy/update-make-request (keyword id) make-request?)
        (response/no-content))

      }]
    ["/proxy-server/:id/recordings/:recording_idx/to_interceptor"
     {
      :post
      (fn [{{:keys [id recording_idx]} :path-params}]
        (proxy/create-an-interceptor-from-recording-idx! (keyword id) (Integer/parseInt recording_idx))
        (response/ok {:interceptors (proxy/interceptors-for-id (keyword id))}))
      }
     ]
    ["/proxy-server/:id/recordings"
     {
      :delete
      (fn [{{:keys [id]} :path-params}]
        (proxy/clean-recordings-for-id! (keyword id))
        (response/no-content))
      }
     ]
    ["/proxy-server/:id/interceptors/file"
     {:get
      (fn [{{:keys [id]} :path-params}]
        {:status 200
         :headers {
                   "content-type" "application/edn"
                   "Content-Disposition" (str "attachment; filename=\"" id ".edn\"" )
                   }
         :body (prn-str  {:interceptors (proxy/interceptors-for-id (keyword id))} )
         })
        :post {
               :parameters {:multipart {:file multipart/temp-file-part}}
               :handler (fn [{{:keys [id]} :path-params
                              multipart-params :multipart-params}]
                          (let [file-obj (get multipart-params "file")] ;TODO Refactor this can be done in one single step
                            (proxy/merge-to-existing-interceptors!
                              (keyword id)
                              (:interceptors
                                (edn/read-string (slurp (:tempfile file-obj)))))
                            {:status 200
                             :body {:interceptors (proxy/interceptors-for-id (keyword id))}}))
               }
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

