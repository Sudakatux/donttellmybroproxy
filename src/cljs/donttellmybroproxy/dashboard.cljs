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
             ["@material-ui/core/Typography" :default Typography]
             ["@material-ui/icons/Add" :default Add]
             ["@material-ui/icons/PlayArrow" :default PlayArrow]
             ["@material-ui/icons/Stop" :default Stop]
             ["@material-ui/icons/Delete" :default DeleteIcon]
             ["@material-ui/icons/AddCircle" :default AddCircle]
             ["@material-ui/icons/Http" :default Http]
             ["@material-ui/core/AppBar" :default AppBar]
             ["@material-ui/core/Toolbar" :default Toolbar]
             ["@material-ui/core/Card" :default Card]
             ["@material-ui/core/CardContent" :default CardContent]
             ["@material-ui/core/CardActions" :default CardActions]
             ["@material-ui/core/CardHeader" :default CardHeader]
             [donttellmybroproxy.common :refer [text-field]]
             [re-frame.core :as rf]
             [reagent.core :as r :refer [atom as-element render-component]]))

;; Handlers

(defn remove-from-proxy-list [elem]
  (rf/dispatch [:proxy/remove-from-list! elem]))

(defn stop-server []
  (rf/dispatch [:server/stop!]))

(defn start-server []
  (rf/dispatch [:server/start!]))

(defn create-proxy [proxy-payload]
  (rf/dispatch [:proxy/add-to-list! proxy-payload]))
;; End Handlers

(defn main-action-buttons []
  (let [server-running? (rf/subscribe [:server/started?])
        status-message (if @server-running? "Running" "Not Running")]
    [:> AppBar
     {
      :position "static"
      }
     [:> Toolbar
      [:> Grid
       {:spacing 4
        :xs 6
        :container true
        :justify "space-between"}
       [:> Typography
        {:variant "h5"}
        (str "Proxy Server status : "  status-message)
        ]
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
        ]]]]))

;(defn main-action-button-styles [theme]
;  #js {:status {:flexGrow 1}})

(defn create-proxy-form []
[:> Card
   {:style #js {:max-width 1000}}
  [:> CardHeader {:title "Create proxy"}]
   [:> CardContent
    [:> Grid
     {:container true
      :direction "column"}
    [text-field
     {:attrs {:label "Proxy Id"
              :id "id"}
      :value (rf/subscribe [:proxy-form/field :id])
      :on-save #(rf/dispatch [:proxy-form/set-field :id %])
      :error  @(rf/subscribe [:proxy-form/error :id])}]
    [text-field
     {:attrs {:label "/some-route"
              :id "route" }
      :value (rf/subscribe [:proxy-form/field :route])
      :on-save #(rf/dispatch [:proxy-form/set-field :route %])
      :error  @(rf/subscribe [:proxy-form/error :route])}]
    [text-field
     {:attrs {:label "Destination http://"
              :id "destination"}
      :value (rf/subscribe [:proxy-form/field :destination])
      :on-save #(rf/dispatch [:proxy-form/set-field :destination %])
      :error  @(rf/subscribe [:proxy-form/error :destination])}]
     [:> CardActions
     [:> Fab
      {:aria-label "Add"
       :on-click #(create-proxy @(rf/subscribe [:proxy-form/fields]))}
      [:> Add]]]]]])

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
                       ]]]]) keys-list))]]))
(defn empty-content []
  nil)

(defn main-layout [{main-content :main-content}]
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
    [main-action-buttons]]
   [:> Grid
    {
     ; :direction "row"
     :container true
     :style #js {:height "100%"}
     }
    [:> Grid
     {:xs 2
      :item true
      :style #js {:borderRight "1px solid"
                  :marginRight "1rem"
                  :paddingTop 10}
     }
     [running-proxy-list]]
    [:> Grid
     {:xs 8
      :item true
      :style #js {:backgroundColor "#f6f8fa"
                  :paddingTop 10}}
     [main-content]]]])
