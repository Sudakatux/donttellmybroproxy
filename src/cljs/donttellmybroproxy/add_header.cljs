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
            [hx.react :as hx :refer [defnc]]
            [hx.hooks :as hooks]
            [cljs-bean.core :refer [bean ->js]]
            [reagent.core :as r]
            [donttellmybroproxy.common :refer [text-field]]
            [re-frame.core :as rf]))

(def options
     [{:title "title" :year 1990}
      {:title "title2" :year 2990}])
;;
;;  Note this is not a re-agent component
;;  Move me to common
(defnc HeaderAutocomplete [__]
       (let [[val updateVal] (hooks/useState "some val")
             renderInput (fn [params]
                           (let [params-clj (bean params)
                                 inputProps-clj (:inputProps params-clj)
                                 option-selected (not-empty (:value (bean inputProps-clj)))
                                 selected-value (or option-selected val)]
                             (hx/f [TextField (merge  params-clj {
                                                                  :label "Insert header"
                                                                  :fullWidth true
                                                                  :inputProps (.assign js/Object inputProps-clj (->js { :onChange #(updateVal (-> % .-target .-value))
                                                                                                                        :value selected-value}))
                                                                  :variant "outlined"} )]))
                      )]

       [Autocomplete {:id "autocomplete"
                      :options (->js options)
                      :renderInput renderInput
                      :getOptionLabel (fn [elem]
                                        (:title (bean elem) ))
                      :freeSolo true
                      }]))
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
     [:> HeaderAutocomplete]
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
  (let [header-values @(rf/subscribe [:proxy/response-headers :yahoo])]
    (.log js/console "header values" (get header-values "Bareer3") )
  ;[:> Grid
  ; {:direction "row"}
   (into [:<>]
         (map (fn [[hk hv]]
                ^{:key hk}
                [:> Chip
                 {:label (str hk ":" hv)
                  :onDelete #( %)}
                 ]) header-values))

    ;]
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
       [existing-header-cloud]]
      ]]])