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
            [donttellmybroproxy.forms :refer [add-header-form]]
            [reagent.core :as r]
            [reagent.session :as session]
            [re-frame.core :as rf]))


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
       [add-header-form
        {
         :header-type-form :request-header-form
         }]
       ]
      [:> Grid
       {:xs 4}
       ; [existing-header-cloud]
       ]
      ]]])