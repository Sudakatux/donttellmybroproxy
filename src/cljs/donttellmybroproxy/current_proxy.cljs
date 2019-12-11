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
            ["@material-ui/core/Tabs" :default Tabs]
            ["@material-ui/core/Tab" :default Tab]
            ["@material-ui/core/Select" :default Select]
            ["@material-ui/core/MenuItem" :default MenuItem]
            ["@material-ui/core/Button" :default Button]
            [donttellmybroproxy.forms :refer [add-header-form update-body-form new-matcher]]
            [reagent.core :as r]
            [reagent.session :as session]
            [re-frame.core :as rf]))

(rf/reg-event-db
  :session/set-matcher!
  (fn [db [_ matcher]]
    (-> db
        (assoc :session/matcher? matcher))))

(rf/reg-event-db
  :session/set-request-or-response!
  (fn [db [_ type]]
    (-> db
        (assoc :session/request-or-response? type))))

(rf/reg-sub
  :proxy/matchers
  :<- [:proxy/list]
  :<- [:session/page]
  (fn [[list page]]
    (get-in list [(keyword page) :args :interceptors] {})))

(rf/reg-sub
  :session/request-or-response?
  (fn [db _]
    (keyword (get db :session/request-or-response? "response"))))

(rf/reg-sub
  :session/matcher?
  (fn [db _]
    (:session/matcher? db)))

(rf/reg-sub
  :proxy/response-headers
  :<- [:proxy/list]
  :<- [:session/page]
  :<- [:session/matcher?]
  (fn [[list page matcher] [_ id]]
    (get-in list [(keyword page) :args :interceptors matcher id :headers] {})))


(defn existing-header-cloud [] ; Note i hardcoded the value
  (let [ header-type-form @(rf/subscribe [:session/request-or-response?])
        header-values @(rf/subscribe [:proxy/response-headers header-type-form])]
    [:> Grid
     {:direction "row"}
     (into [:<>]
           (map (fn [[hk hv]]
                  ^{:key hk}
                  [:> Chip
                   {:label    (str hk ":" hv)
                    :onDelete #(%)}
                   ]) header-values))]))

(defn single-header-configuration [{title  :title}]
   [:<>
   [:> Typography title]
   [:> Grid
    {:direction "row"
     :container true}
    [:> Grid
     {:xs 8}
     [add-header-form]
     ]
    [:> Grid
     {:xs 4}
     [existing-header-cloud]
     ]]])


(defn add-header-card []
  (let [type @(rf/subscribe [:session/request-or-response?])]
  [:> Card
   {:style #js {:maxWidth 1000}}
   [:> CardHeader {:title "Headers"}]
   [:> CardContent
    [single-header-configuration {:title  "Response Header"
                                  :target type}]]]))
(defn add-interceptor-card []
  [:> Card
   {:style #js {:maxWidth 1000}}
   [:> CardHeader {:title "Body"}]
   [:> CardContent
    [update-body-form]]])



(defn matcher-menu []
  (let [
        modal-state (r/atom false)
        close-modal #(reset! modal-state false)
        open-modal (fn [] (reset! modal-state true))]
    (fn []
      (let [matchers @(rf/subscribe [:proxy/matchers])]
        [:> Grid
         (into [:> Select
                {:style     #js {:width "50%"}
                 :value @(rf/subscribe [:session/matcher?])
                 :on-change #(rf/dispatch [:session/set-matcher! (-> % .-target .-value)])}]
               (map (fn [matcher]
                      ^{:key matcher}
                      [:> MenuItem
                       {:value matcher}
                       matcher
                       ]) (keys matchers))
               )
         [:> Button
          {:on-click open-modal}
          "Add Matcher"
          ]
         [new-matcher {:modal-opened @modal-state
                       :on-close close-modal}]
         ]))))

(defn card-container []
  (let [type @(rf/subscribe [:session/request-or-response?])]
    [:> Grid
     {:container true
      :direction "column"}
     [matcher-menu]
     [:> Tabs
      {:value type
       :onChange (fn [_ newType] (rf/dispatch [:session/set-request-or-response! newType]))}                                              ; TODO add support for this
[:> Tab {:label "response" :value :response}]
[:> Tab {:label "request" :value :request}]]
[:> Grid
 {:container true
  :direction "column"
  :spacing 2}
 [:> Grid
  {:item true}
    [add-header-card]

  ]
 [:> Grid
  {:item true }
  [add-interceptor-card]
  ]
 ]]))
