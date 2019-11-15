(ns donttellmybroproxy.add-header
  (:require ["@material-ui/core/colors" :as mui-colors]
            ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
            ["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/core/Chip" :default Chip]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/lab/Autocomplete" :default Autocomplete]
            ["@material-ui/core/Card" :default Card]
            ["@material-ui/core/CardContent" :default CardContent]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/CardHeader" :default CardHeader]
            ["@material-ui/core/Button" :default Button]

            [reagent.core :as r]
            [reagent.session :as session]
            [donttellmybroproxy.common :refer [text-field HeaderAutocomplete]]
            [re-frame.core :as rf]))

(def options
     [{:title "title" :year 1990}
      {:title "title2" :year 2990}])
;;
;;  Note this is not a re-agent component
;;  Move me to common

(defn add-header-form []
  [:> Grid
   {:container true
    :justify "space-between"
    :direction "column"}
   [:> Grid
    {:direction "row"
     :justify "space-between"
     :container true}
    [:> Grid
     {
      :xs 4
      }
     [:> HeaderAutocomplete
     {
      :initialValue ""
      :options options
      :onSave (fn [val]
                   (.log js/console "selected value is" val))
      }]
     ]
    [:> Grid
     {:xs 4}
     [:> TextField {:label "Header value"}]
     ]]
   [:> Button
    "Add Header"
    ]
   ])

(defn existing-header-cloud []                 ; Note i hardcoded the value
  (let [current-page (session/get :route)
        header-values @(rf/subscribe [:proxy/response-headers :yahoo])]
(.log js/console "current page" current-page)
  [:> Grid
   {:direction "row"}
   (into [:<>]
         (map (fn [[hk hv]]
                ^{:key hk}
                [:> Chip
                 {:label (str hk ":" hv)
                  :onDelete #( %)}
                 ]) header-values))

    ]
))



(defn add-header-card []
  [:> Card
   {:style #js {:maxWidth 1000}}
   [:> CardHeader {:title "Add Header"}]
   [:> CardContent
     [:> Grid
      {:direction "row"
       :container true}
      [:> Grid
       {:xs 8}
       [add-header-form]
       ]
      [:> Grid
       {:xs 4}
       ; [existing-header-cloud]
       ]
      ]]])