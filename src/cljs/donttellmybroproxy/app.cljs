(ns donttellmybroproxy.app
  (:require  ["@material-ui/core/colors" :as mui-colors]
             ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
             ["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
             ["@material-ui/core/Grid" :default Grid]
             ["@material-ui/core/Typography" :default Typography]
             ["@material-ui/core/List" :default List]
             ["@material-ui/core/Fab" :default Fab]
             ["@material-ui/core/TextField" :default TextField]
             ["@material-ui/icons/Add" :default Add]
             ["@material-ui/icons/PlayArrow" :default PlayArrow]
             ["@material-ui/icons/Stop" :default Stop]
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

(defn create-proxy-form []
      [:> Grid
       {:container true
        :direction "column"}
       [:> TextField
        {:label "Proxy Id"
         :id "proxy-id"}]
       [:> TextField
        {:label "/proxy-route"
         :id "proxy-route"}]
       [:> TextField
        {:label "/proxy-destination"
         :id "proxy-destination"}]
       [:> Grid
        {:container true
         :justify "flex-end"}
        [:> Fab
         {:aria-label "Add"}
          [:> Add]
         ]
        ]
       ])

(defn main-action-buttons []
      [:> Grid
       {
        :container true
       }
       [:> Grid
        {:xs 4}
       [:> Typography
        {:variant "h5"}
        "Proxy Server status : "
        ]]
       [:> Grid
        {:xs 4
         :container true
         :justify "space-between"}
       [:> Fab
        {:aria-label "Add"}
        [:> PlayArrow]
        ]
       [:> Fab
        {:aria-label "Stop"}
        [:> Stop]
        ]]])

(defn running-proxy-list []
      [:> Grid
       {:direction "column"}
       [:> Typography
        {:variant "h6"}
        "Running proxies"]
       [:> List
        {:dense true}
        ;Should render list item
        ]
       ])

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

(mount-app)

;(js/alert "Hello from shadow-cljs")

