(ns donttellmybroproxy.forms
  (:require [re-frame.core :as rf]
            ["@material-ui/core/Card" :default Card]
            ["@material-ui/core/CardHeader" :default CardHeader]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/core/Box" :default Box]
            ["@material-ui/icons/Add" :default Add]
            ["@material-ui/core/Fab" :default Fab]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/CardContent" :default CardContent]
            ["@material-ui/core/Button" :default Button]
            [donttellmybroproxy.validations :refer [validate-proxy-entry validate-header-schema]]
            [donttellmybroproxy.common :refer [text-field HeaderAutocomplete]]
            [donttellmybroproxy.common :refer [text-field]]))



;(def root-db-path [:forms])
(def value-db-path [:forms :values])
(def error-db-path [:forms :errors])

(rf/reg-event-db
  :form/set-field
  (fn [db [_ form-id field-path new-value]]
    (assoc-in db (vec (concat value-db-path (cons form-id field-path))) new-value)))

(rf/reg-event-db
  :proxy/set-proxy-list
  (fn [db [_ list]]
    (-> db
        (assoc :proxy/list list))))


(rf/reg-event-db
  :proxy/set-headers!
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ proxy-id header-type headers]]
    (assoc-in proxy-list [proxy-id :args header-type :headers] headers)))

(rf/reg-sub
  :form/fields
  (fn [db]
    (get-in db value-db-path)))

(rf/reg-sub
  :form/field
  :<- [:form/fields]
  (fn [forms-data [_ form-id field-path]]
    (get-in forms-data (vec (cons form-id field-path)))))

(rf/reg-sub
  :form/errors
  (fn [db]
    (get-in db error-db-path)))

(rf/reg-sub
  :form/error
  :<- [:form/errors]
  (fn [forms-data [_ form-id field-path]]
    (get-in forms-data (vec (cons form-id field-path)))))

(rf/reg-event-fx
  :proxy/add-to-list!
  (fn [{:keys [db]} [_ proxy-payload]]
    (if-let [validation-errors (validate-proxy-entry proxy-payload)]
      {:db (assoc-in db [:forms :errors :new-proxy] validation-errors)}
      {:ajax/post {
                   :url "/api/proxy-server/create"
                   :params proxy-payload
                   :success-path [:list]
                   :success-event [:proxy/set-proxy-list]}})))

(rf/reg-event-fx
  :proxy/add-header!
  (fn [{:keys [db]} [_ {:keys [header-type-form id payload]}]]
    (if-let [validation-errors (validate-header-schema payload)]
      {:db (assoc-in db [:forms :errors header-type-form] validation-errors)}
      {:ajax/post {
                   :url (str "/api/proxy-server/" (name header-type-form)  "/headers/" id)
                   :params payload
                   :success-path [:list]
                   :success-event [:proxy/set-headers! (keyword id) header-type-form]}})))


(defn create-proxy [proxy-payload]
  (rf/dispatch [:proxy/add-to-list! (:new-proxy proxy-payload)]))

(defn add-header! [{header-type-form :header-type-form
                   id :id
                   payload :payload}]
  (.log js/console "This is the payload" payload header-type-form)
    (rf/dispatch [:proxy/add-header! {:header-type-form header-type-form
                                    :id id
                                    :payload payload}]))


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
       :value (rf/subscribe [:form/field :new-proxy [:id]])
       :on-save #(rf/dispatch [:form/set-field :new-proxy [:id] %])
       :error  @(rf/subscribe [:form/error :new-proxy [:id]])}]
     [text-field
      {:attrs {:label "/some-route"
               :id "route" }
       :value (rf/subscribe [:form/field :new-proxy [:route]])
       :on-save #(rf/dispatch [:form/set-field :new-proxy [:route] %])
       :error  @(rf/subscribe [:form/error :new-proxy [:route]])}]
     [text-field
      {:attrs {:label "Destination http://"
               :id "destination"}
       :value (rf/subscribe [:form/field :new-proxy [:destination]])
       :on-save #(rf/dispatch [:form/set-field :new-proxy [:destination] %])
       :error  @(rf/subscribe [:form/error :new-proxy [:destination]])}]
     [:> CardActions
      [:> Fab
       {:aria-label "Add"
        :on-click #(create-proxy @(rf/subscribe [:form/fields]))}
       [:> Add]]]]]])

(def options
  [{:title "Content-Type" :value "Content-Type"}
   {:title "Bareer" :value "Bareer"}])

(defn add-header-form [{header-type-form :header-type-form}]
  [:> Box {:style #js {:padding 20 }}
     [:> Grid
      {:direction "row"
       :justify "space-between"
       :container true}
      [:> Grid
       {
        :xs 4
        :item true
        }
       [:> HeaderAutocomplete
        {
         :initialValue @(rf/subscribe [:form/field header-type-form [:header-key]])
         :options options
         :onSave #(rf/dispatch [:form/set-field header-type-form [:header-key] %])
         }]
       ]
      [:> Grid
       {:xs 4
        :item true
        }
       [text-field
        {:attrs {:label "Header"
                 :id "header-value" }
         :value (rf/subscribe [:form/field header-type-form [:header-value]])
         :on-save #(rf/dispatch [:form/set-field header-type-form [:header-value] %])
         :error  @(rf/subscribe [:form/error header-type-form [:header-value]])}]
       ]

      ]
   ;[:> Box
   ; {:style #js {:width "100%"
   ;              :display "flex"
   ;              :justify-content "center"
   ;              }}

      [:> Button
       {:on-click #(add-header! {:header-type-form header-type-form
                                 :id @(rf/subscribe [:session/page])
                                 :payload (get @(rf/subscribe [:form/fields]) header-type-form)})
        :style #js {:margin-top 20}
        :color "primary"
        :variant "contained"
        }
       "Add Header"
       ]
    ;]
])
