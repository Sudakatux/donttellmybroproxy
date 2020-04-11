(ns donttellmybroproxy.dashboard
  (:require  ["@material-ui/core/colors" :as mui-colors]
             ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
    ;["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
             ["@material-ui/core/Grid" :default Grid]
             ["@material-ui/core/Box" :default Box]
             ["@material-ui/core/ButtonGroup" :default ButtonGroup]
             ;["@material-ui/core/ButtonNavigation" :default ButtonNavigation]
             ;["@material-ui/core/BottomNavigationAction" :default BottomNavigationAction]
             ["@material-ui/core/Button" :default Button]
             ["@material-ui/core/ListSubheader" :default ListSubheader]
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
             ["@material-ui/icons/Edit" :default Edit]
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
             [accountant.core :as accountant]
             [reagent.core :as r :refer [atom as-element]]))

;; Handlers

(defn remove-from-proxy-list [elem]
  (rf/dispatch [:proxy/remove-from-list! elem]))

(defn stop-server []
  (rf/dispatch [:server/stop!]))

(defn start-server [port]
  (rf/dispatch [:server/start! port]))

;; End Handlers

(defn main-action-buttons []
  (let [server-running? (rf/subscribe [:server/started?])
        status-message (if @server-running? "Running" "Not Running")
        proxy-port (rf/subscribe [:proxy/port])]
    [:> Card
     [:> CardHeader
      {
       :subheader (str "Proxy Server status : "  status-message)
       }
      ]
     [:> CardContent
      [:> Box { :mb 2}
        [text-field
         {:attrs {:label "Port number"
                  :id "route"
                  :type "number"
                  :disabled @server-running?}
          :value proxy-port
          :on-save #(rf/dispatch [:proxy/set-port! %])}]
       ]
      [:> Grid {:container true :spacing 4 }
       [:> Grid {:item true}
         [:> Fab
          {:aria-label "Play"
           :disabled @server-running?
           :on-click #(start-server @proxy-port)}
          [:> PlayArrow]
          ]
        ]
       [:> Grid {:item true}
       [:> Fab
        {:aria-label "Stop"
         :disabled (not @server-running?)
         :on-click stop-server}
        [:> Stop]]]]]]))

(defn running-proxy-list []
  (let [binded-lists (rf/subscribe [:proxy/list])
        keys-list (keys @binded-lists)]
    [:> Grid
     {:direction "column"
      :container true
      :spacing 2
      }
     [:> Grid {:item true}
      [main-action-buttons]]
     [:> Grid {:item true}
      [:> Card
       [:> CardActions

         [:> ButtonGroup
             [:> Button {:onClick #(accountant/navigate! "/create")}
               [:> Add]
              ]
          ]
        ]
       [:> CardContent
      [:> List
       {:dense true :subheader (r/as-element [:> ListSubheader "Registered proxies" ]  )}
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
                        :on-click #(remove-from-proxy-list elem)
                        }
                       [:> DeleteIcon
                        ]]
                      [:> IconButton
                       {:edge "end"
                        :on-click #(accountant/navigate! (str "/proxy/" (name elem)))
                        }
                       [:> Edit
                        ]]]]) keys-list))]]]]]))

;; TODO center me
(defn empty-content []
  [:> Button
   {:on-click #(accountant/navigate! "/create")
    :startIcon (r/as-element [:> Add] )
    :variant "contained"
    :size "large"
    }
   "Create Proxy"
   ])

(defn main-layout [{main-content :main-content}]
  ;[:> Grid
  ; {:direction "column"
  ;  :spacing 2
  ;  :justify "space-between"}
   ;[:> Grid
   ; {
   ;  :xs 12
   ;  :justify "center"
   ;  :direction "row"
   ;  :container true
   ;  }
   ; ]
   [:> Grid
    {
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
     [main-content]]]

;]
)
