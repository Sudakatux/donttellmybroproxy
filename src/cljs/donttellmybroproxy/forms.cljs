(ns donttellmybroproxy.forms
  (:require [re-frame.core :as rf]
            ["@material-ui/core/Card" :default Card]
            ["@material-ui/core/CardHeader" :default CardHeader]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/icons/Add" :default Add]
            ["@material-ui/core/Fab" :default Fab]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/CardContent" :default CardContent]
            [donttellmybroproxy.validations :refer [validate-proxy-entry]]
            [donttellmybroproxy.common :refer [text-field]]))

(defn create-proxy [proxy-payload]
  (rf/dispatch [:proxy/add-to-list! (:new-proxy proxy-payload)]))

;(def root-db-path [:forms])
(def value-db-path [:forms :values])
(def error-db-path [:forms :errors])

(rf/reg-event-db
  :form/set-field
  (fn [db [_ form-id field-path new-value]]
    (assoc-in db (vec (concat value-db-path (cons form-id field-path))) new-value)))

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

