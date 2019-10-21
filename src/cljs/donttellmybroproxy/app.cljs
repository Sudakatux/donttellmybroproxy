(ns donttellmybroproxy.app
  (:require  ["@material-ui/core/colors" :as mui-colors]
             ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
             ["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
             ["@material-ui/core/Grid" :default Grid]
             ["@material-ui/core/Typography" :default Typography]
             ["@material-ui/core/List" :default List]
             ["@material-ui/core/ListItem" :default ListItem]
             ["@material-ui/core/ListItemText" :default ListItemText]
             ["@material-ui/core/ListItemSecondaryAction" :default ListItemSecondaryAction]
             ["@material-ui/core/IconButton" :default IconButton]
             ["@material-ui/core/Fab" :default Fab]
             ["@material-ui/core/TextField" :default TextField]
             ["@material-ui/icons/Add" :default Add]
             ["@material-ui/icons/PlayArrow" :default PlayArrow]
             ["@material-ui/icons/Stop" :default Stop]
             ["@material-ui/icons/Delete" :default DeleteIcon]

             [ajax.core :refer [GET POST PUT DELETE]]
             [re-frame.core :as rf]
             [reagent.core :as r :refer [atom as-element render-component]]))
;import StopIcon from '@material-ui/icons/Stop';
(defn custom-theme []
      (createMuiTheme
        (clj->js
          { :palette
           {:type       "light"
            :primary    (.-blue mui-colors)
            :secondary  (.-amber mui-colors)}
           :typography
           { :useNextVariants true}})))

(defn text-field [{val :value
                   props :attrs
                   :keys [on-save]}]
  (let [draft (r/atom nil)
        value (r/track #(or @draft @val ""))]
    (fn []
      [:> TextField
       (merge props
              {
               :on-focus #(reset! draft (or @val ""))
               :on-blur (fn []
                          (on-save (or @draft ""))
                          (reset! draft nil))
               :on-change #(reset! draft (.. % -target -value))
               :value @value })
       ])))

(defn create-proxy [proxy-payload]
  (POST "/api/proxy-server/create"
       {:headers {"Accept" "application/transit+json"}
        :params proxy-payload
        :handler #(rf/dispatch [:proxy/set-proxy-list  (:list %) ])})) ;; TODO need to clear fields

(defn create-proxy-form []
      [:> Grid
       {:container true
        :direction "column"}
       [text-field
        {:attrs {:label "Proxy Id"
                 :id "id"}
         :value (rf/subscribe [:proxy-form/field :id])
         :on-save #(rf/dispatch [:proxy-form/set-field :id %])}]
       [text-field
        {:attrs {:label "/proxy-route"
                 :id "route"}
         :value (rf/subscribe [:proxy-form/field :route])
         :on-save #(rf/dispatch [:proxy-form/set-field :route %])}]
       [text-field
        {:attrs {:label "Destination http://"
                 :id "destination"}
         :value (rf/subscribe [:proxy-form/field :destination])
         :on-save #(rf/dispatch [:proxy-form/set-field :destination %])}]
       [:> Grid
        {:container true
         :justify "flex-end"}
        [:> Fab
         {:aria-label "Add"
          :on-click #(create-proxy @(rf/subscribe [:proxy-form/fields]))}
          [:> Add]
         ]
        ]
       ])

(defn start-server []
  (PUT "/api/proxy-server/start"
       {:headers {"Accept" "application/transit+json"}
        :params {:port 3001 }                               ;;TODO add this to a text field so user can specify
        :handler #(rf/dispatch [:server/set-status true])}))

(defn stop-server []
  (PUT "/api/proxy-server/stop"
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:server/set-status false])}))

(defn main-action-buttons []
      (let [server-running? (rf/subscribe [:server/started?])
            status-message (if @server-running? "Running" "Not Running")]
          [:> Grid
           {
            :container true
           }
           [:> Grid
            {:xs 4}
           [:> Typography
            {:variant "h5"}
             (str "Proxy Server status : "  status-message)
            ]]
           [:> Grid
            {:xs 4
             :container true
             :justify "space-between"}
           [:> Fab
            {:aria-label "Play"
             :disabled @server-running?
             :on-click start-server}
            [:> PlayArrow]
            ]
           [:> Fab
            {:aria-label "Stop"
             :disabled (not @server-running?)
             :on-click stop-server}
            [:> Stop]                                       ;; TODO implement stop
            ]]])
      )

(defn remove-from-proxy-list [elem]
  (DELETE (str "/api/proxy-server/delete/" (name elem))
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:proxy/list-remove elem])}))


(defn running-proxy-list []
  (let [binded-lists (rf/subscribe [:proxy/list])
        keys-list (keys @binded-lists)]
    [:> Grid
     {:direction "column"}
     [:> Typography
      {:variant "h6"}
      "Running proxies"]
     [:> List
      {:dense true}
      ;Should render list item
      (into [:<>]
            (map (fn [elem]
                   ^{:key elem}
                   [:> ListItem
                    [:> ListItemText
                     {:primary elem}
                     ]
                    [:> ListItemSecondaryAction
                     [:> IconButton
                      {:edge "end"
                       :on-click #(remove-from-proxy-list elem)}
                      [:> DeleteIcon
                       ]]]
                    ]
                   ) keys-list))
      ]
     ]
    ))

(defn main-layout []
      [:> Grid
       {:direction "column"
        :justify "space-between"}
       [:> Grid
        {
         :xs 12
         :justify "center"
         :direction "row"
         :container true
         }
          [main-action-buttons]
        ]
      [:> Grid
       {
        :direction "row"
        :container true
        }
       [:> Grid
        {:xs 4}
          [running-proxy-list]
        ]
       [:> Grid
        {:xs 4}
        [create-proxy-form]
        ]
       ]])

(defn app []
      [:> ThemeProvider
       {:theme (custom-theme)}
       [main-layout]
       ])

(defn mount-app []
      (render-component
        [app]
        (js/document.getElementById "content")))

(defn server-status []
      (GET "/api/proxy-server/status"
           {:headers {"Accept" "application/transit+json"}
            :handler #(rf/dispatch [:server/set-status (get-in % [:result :server-running])])}))

(defn proxy-list []
  (GET "/api/proxy-server/list"
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:proxy/set-proxy-list  (:list %) ])}))

(rf/reg-event-db
  :server/set-status
  (fn [db [_ returned-status]]
      (-> db
          (assoc :server/started? returned-status))))

(rf/reg-event-db
  :proxy/set-proxy-list
  (fn [db [_ list]]
    (-> db
        (assoc :proxy/list list))))

(rf/reg-event-db
  :proxy/set-proxy-list
  (fn [db [_ list]]
    (-> db
        (assoc :proxy/list list))))

(rf/reg-event-db
  :proxy/list-remove
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ item]]
    (dissoc proxy-list item)))

(rf/reg-event-db
  :proxy-form/set-field
  [(rf/path :proxy-form/fields)]
  (fn [fields [_ id value]]
    (assoc fields id value)))

(rf/reg-sub
  :proxy-form/fields
  (fn [db _]
    (:proxy-form/fields db)))

(rf/reg-sub
  :proxy-form/field
  :<- [:proxy-form/fields]
  (fn [fields [_ id]]
    (get fields id)))

(rf/reg-event-db
  :proxy-form/clear-fields
  [(rf/path :proxy-form/fields)]
  (fn [_ _]
    {}))

(rf/reg-sub
  :proxy/list
  (fn [db _]
      (:proxy/list db [])))

(rf/reg-event-fx
  :app/initialize
  (fn [_ _]
      {:db {:server/started? false
            :proxy/list []}}))

;; TODO to be implemented
;; Should clear fields and add to list
(rf/reg-event-fx
  :proxy-server/add-proxy-server
  (fn [_ _]
    {:db {:server/started? false}}))

(rf/reg-sub
  :server/started?
  (fn [db _]
      (:server/started? db)))

(defn init! []
      (.log js/console "Initializing App...")
      (rf/dispatch [:app/initialize])
      (server-status)
      (proxy-list)
      (mount-app))

(init!)

;(js/alert "Hello from shadow-cljs")

