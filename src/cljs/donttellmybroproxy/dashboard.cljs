(ns donttellmybroproxy.dashboard
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

             [re-frame.core :as rf]
             [reagent.core :as r :refer [atom as-element render-component]]))

;; Handlers

(defn proxy-list []
  (rf/dispatch [:proxy/load-list]))

(defn server-status []
  (rf/dispatch [:server/load-status]))

(defn remove-from-proxy-list [elem]
  (rf/dispatch [:proxy/remove-from-list! elem]))

(defn stop-server []
  (rf/dispatch [:server/stop!]))

(defn start-server []
  (rf/dispatch [:server/start!]))

(defn create-proxy [proxy-payload]
  (rf/dispatch [:proxy/add-to-list! proxy-payload]))
;; End Handlers

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
       ]]]))

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
     {:xs 2
      :item true}
     [running-proxy-list]
     ]
    [:> Grid
     {:xs 4
      :item true}
     [create-proxy-form]
     ]
    ]])
