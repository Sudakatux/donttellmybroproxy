(ns donttellmybroproxy.current_proxy
  (:require ["@material-ui/core/colors" :as mui-colors]
            ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
            ["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/core/Typography" :default Typography]
            ["@material-ui/core/Chip" :default Chip]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/lab/Autocomplete" :default Autocomplete]
            ["@material-ui/core/Card" :default Card]
            ["@material-ui/core/CardContent" :default CardContent]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/CardHeader" :default CardHeader]
            [donttellmybroproxy.forms :refer [add-header-form add-interceptor]]
            [reagent.core :as r]
            [reagent.session :as session]
            [re-frame.core :as rf]))


(defn existing-header-cloud [{header-type-form :header-type-form}]                 ; Note i hardcoded the value
  (let [header-values @(rf/subscribe [:proxy/response-headers header-type-form])]
  [:> Grid
   {:direction "row"}
   (into [:<>]
         (map (fn [[hk hv]]
                ^{:key hk}
                [:> Chip
                 {:label (str hk ":" hv)
                  :onDelete #( %)}
                 ]) header-values))]))

(defn single-header-configuration [{title :title
                                    target :target}]
  [:<>
   [:> Typography title]
   [:> Grid
    {:direction "row"
     :container true}
    [:> Grid
     {:xs 8}
     [add-header-form
      {
       :header-type-form target
       }]
     ]
    [:> Grid
     {:xs 4}
     [existing-header-cloud
      {:header-type-form target}
      ]]]])


(defn add-header-card []
  [:> Card
   {:style #js {:maxWidth 1000}}
   [:> CardHeader {:title "Headers"}]
   [:> CardContent
   [single-header-configuration {:title "Request Header"
                                 :target :request}]
    [single-header-configuration {:title "Response Header"
                                  :target :response}]]])
(defn add-interceptor-card []
  [:> Card
   {:style #js {:maxWidth 1000}}
   [:> CardHeader {:title "Headers"}]
   [:> CardContent
    [add-interceptor]]])

(defn forms-container []
  [:> Grid
   {:container true
    :direction "row"}
   [add-header-card]
   [add-interceptor-card]

   ]
  )
