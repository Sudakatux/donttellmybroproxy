(ns donttellmybroproxy.forms
  (:require [re-frame.core :as rf]
            ["@material-ui/core/Card" :default Card]
            ["@material-ui/core/CardHeader" :default CardHeader]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/core/Box" :default Box]
            ["@material-ui/core/Dialog" :default Dialog]
            ["@material-ui/core/Paper" :default Paper]
            ["@material-ui/icons/Add" :default Add]
            ["@material-ui/core/Fab" :default Fab]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/CardContent" :default CardContent]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Button" :default Button]
            [reagent.core :as r :refer [atom]]
            [donttellmybroproxy.validations :refer [validate-proxy-entry validate-header-schema validate-body]]
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
  :proxy/add-matcher!
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ proxy-id matcher]]
    (assoc-in proxy-list [proxy-id :args :interceptors matcher] {})
    ))


(rf/reg-event-db
  :proxy/set-headers!
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ proxy-id header-type matcher headers]]
    (assoc-in proxy-list [proxy-id :args :interceptors matcher header-type] headers)))

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

;; TODO implement me
(rf/reg-event-fx
  :proxy/update-body!
  (fn [{:keys [db]} [_ {:keys [type id payload]}]]
    (if-let [validation-errors (validate-body payload)]
      {:db (assoc-in db [:forms :errors type] validation-errors)}
      {:ajax/post {
                   :url (str "/api/proxy-server/" (name type)  "/body/" id)
                   :params payload
                   :success-path [:body]
                   :success-event [:proxy/set-body! (keyword id) type (:matcher payload)]}}))) ;Missing the set-body event

(rf/reg-event-fx
  :proxy/add-header!
  (fn [{:keys [db]} [_ {:keys [header-type-form id payload]}]]
    (if-let [validation-errors (validate-header-schema payload)]
      {:db (assoc-in db [:forms :errors header-type-form] validation-errors)}
      {:ajax/post {
                   :url (str "/api/proxy-server/" (name header-type-form)  "/headers/" id)
                   :params payload
                   :success-event [:proxy/set-headers! (keyword id) header-type-form (:matcher payload)]}})))


(defn create-proxy [proxy-payload]
  (rf/dispatch [:proxy/add-to-list! (:new-proxy proxy-payload)]))

(defn change-body! [change-body-params]
  (rf/dispatch [:proxy/update-body! change-body-params]))

(defn add-header! [{header-type-form :header-type-form
                   id :id
                   payload :payload}]
    (rf/dispatch [:proxy/add-header! {:header-type-form header-type-form
                                    :id id
                                    :payload payload}]))

(defn add-matcher! [{matcher :matcher
                     proxy-id :proxy-id
                     }]
  (rf/dispatch [:proxy/add-matcher! (keyword proxy-id) matcher]))

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

(defn add-header-form []
  (let [header-type-form (rf/subscribe [:session/request-or-response?])
        header-key-value (rf/subscribe [:form/field @header-type-form [:header-key]])
        header-key-error @(rf/subscribe [:form/error @header-type-form [:header-key]])
        header-key-dispatcher #(rf/dispatch [:form/set-field @header-type-form [:header-key] %])
        header-value (rf/subscribe [:form/field @header-type-form [:header-value]])
        header-value-error @(rf/subscribe [:form/error @header-type-form [:header-value]])
        header-value-dispatcher #(rf/dispatch [:form/set-field @header-type-form [:header-value] %])
        ]
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
       [:> TextField
        (cond-> {:value @header-key-value
                 :onChange #(header-key-dispatcher (-> % .-target .-value))
                 :label "Header Key"
                 :id "header-key"
                 }
                header-key-error (merge {:error true :helperText header-key-error}))
        ]
       ]
      [:> Grid
       {:xs 4
        :item true
        }
       [:> TextField
        (cond-> {:value @header-value
                 :onChange #(header-value-dispatcher (-> % .-target .-value))
                 :label "Header Value"
                 :id "header-value"
                 }
                header-value-error (merge {:error true :helperText header-value-error}))

        ]
       ]]
     [:> Button
      {:on-click #(add-header! {:header-type-form @header-type-form
                                :id @(rf/subscribe [:session/page])
                                :payload (assoc
                                           (get
                                             @(rf/subscribe [:form/fields]) @header-type-form)
                                           :matcher @(rf/subscribe [:session/matcher?])) }) ;; TODO hardcoded matcher
       :style #js {:margin-top 20}
       :color "primary"
       :variant "contained"
       }
      "Add Header"]]
    )
  )

(defn new-matcher [{
                    modal-opened :modal-opened
                    on-close     :on-close
                    }]
  [:> Dialog
   {
    :open modal-opened
    }
   [:> Card
    [:> CardContent
     [text-field
      {:attrs   {:label    "Url Matcher"
                 :id       "matcher"
                 :multiple true
                 :rowMax   4}
       :value   (rf/subscribe [:form/field :new-matcher [:matcher]])
       :on-save #(rf/dispatch [:form/set-field :new-matcher [:matcher] %])
       :error   @(rf/subscribe [:form/error :new-matcher [:matcher]])}]
     ]
    [:> CardActions
     [:> Button
      {:on-click (fn [] (do
                     (add-matcher! {:matcher @(rf/subscribe [:form/field :new-matcher [:matcher]])
                                    :proxy-id @(rf/subscribe [:session/page])})
                     (on-close)
                     ) )}
      "Add"
      ]
     [:> Button
      {:on-click on-close}
      "Cancel"
      ]]]])


(defn update-body-form
  "Takes a type meaning request response. Returns a body form"
  []
  (let [type (rf/subscribe [:session/request-or-response?])
        body-value (rf/subscribe [:form/field @type [:body]])
        body-value-error @(rf/subscribe [:form/error @type [:body]])
        body-value-dispatcher #(rf/dispatch [:form/set-field @type [:body] %])]
    [:> Card
     {:style #js {:max-width 1000}}
     [:> CardHeader {:title "Create proxy"}]
     [:> CardContent
      [:> Grid
       {:container true
        :direction "column"}
       [:> TextField
        (cond-> {
                 :value @body-value
                 :onChange #(body-value-dispatcher (-> % .-target .-value))
                 :multiline true
                 :rowsMax 4
                 }
                body-value-error (merge {:error true :helperText body-value-error}))
        ]
       [:> CardActions
        [:> Fab
         {:aria-label "Add"
          :on-click #(change-body! {:type @type
                                    :id @(rf/subscribe [:session/page])
                                    :payload (assoc
                                               (get @(rf/subscribe [:form/fields]) @type)
                                               :matcher @(rf/subscribe [:session/matcher?]))} )}
         [:> Add]]]]]]
    )

  )

