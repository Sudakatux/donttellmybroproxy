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
            ["@material-ui/core/Select" :default Select]
            ["@material-ui/core/MenuItem" :default MenuItem]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/CardContent" :default CardContent]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Button" :default Button]
            [reagent.core :as r :refer [atom]]
            [donttellmybroproxy.validations :refer [validate-proxy-entry validate-header-schema validate-body]]
            [donttellmybroproxy.common :refer [text-field text-area]]))

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
  (fn [proxy-list [_ proxy-id matcher method]]
    (assoc-in proxy-list [proxy-id :args :interceptors matcher method] {})))

(rf/reg-event-db
  :proxy/set-headers!
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ proxy-id header-type matcher method headers]]
    (assoc-in proxy-list [proxy-id :args :interceptors matcher method header-type] headers)))

(rf/reg-event-db
  :proxy/set-body!
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ proxy-id type matcher method body]]
    (assoc-in proxy-list [proxy-id :args :interceptors matcher method type :body] body)))

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
                   :url (str "/api/proxy-server/" id  "/body/" (name type) )
                   :params payload
                   :success-path [:body]
                   :success-event [:proxy/set-body! (keyword id) type (:matcher payload) (:method payload)]}}))) ;Missing the set-body event

(rf/reg-event-fx
  :proxy/add-header!
  (fn [{:keys [db]} [_ {:keys [header-type-form id payload]}]]
    (if-let [validation-errors (validate-header-schema payload)]
      {:db (assoc-in db [:forms :errors header-type-form] validation-errors)}
      {:ajax/post {
                   :url (str "/api/proxy-server/" (name header-type-form)  "/headers/" id)
                   :params payload
                   :success-event [:proxy/set-headers! (keyword id) header-type-form (:matcher payload) (:method payload)]}})))

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

;(defn add-matcher! [{matcher :matcher
;                     proxy-id :proxy-id
;                     }]
;  (rf/dispatch [:proxy/add-matcher! (keyword proxy-id) matcher]))

(defn create-proxy-form []
  (let [proxy-id-value (rf/subscribe [:form/field :new-proxy [:id]])
        proxy-error @(rf/subscribe [:form/error :new-proxy [:id]])
        route-value (rf/subscribe [:form/field :new-proxy [:route]])
        route-error @(rf/subscribe [:form/error :new-proxy [:route]])
        destination-value (rf/subscribe [:form/field :new-proxy [:destination]])
        destination-error @(rf/subscribe [:form/error :new-proxy [:destination]])
        form-fields @(rf/subscribe [:form/fields])]
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
       :value proxy-id-value
       :on-save #(rf/dispatch [:form/set-field :new-proxy [:id] %])
       :error  proxy-error }]
     [text-field
      {:attrs {:label "/some-route"
               :id "route" }
       :value route-value
       :on-save #(rf/dispatch [:form/set-field :new-proxy [:route] %])
       :error  route-error}]
     [text-field
      {:attrs {:label "Destination http://"
               :id "destination"}
       :value destination-value
       :on-save #(rf/dispatch [:form/set-field :new-proxy [:destination] %])
       :error  destination-error}]
     [:> CardActions
      [:> Fab
       {:aria-label "Add"
        :on-click #(create-proxy form-fields)}
       [:> Add]]]]]]))

(defn add-header-form []
  (let [header-type-form (rf/subscribe [:session/request-or-response?])
        header-key-value (rf/subscribe [:form/field @header-type-form [:header-key]])
        header-key-error @(rf/subscribe [:form/error @header-type-form [:header-key]])
        header-key-dispatcher #(rf/dispatch [:form/set-field @header-type-form [:header-key] %])
        header-value (rf/subscribe [:form/field @header-type-form [:header-value]])
        header-value-error @(rf/subscribe [:form/error @header-type-form [:header-value]])
        header-value-dispatcher #(rf/dispatch [:form/set-field @header-type-form [:header-value] %])
        matcher @(rf/subscribe [:session/matcher-regex?])
        method @(rf/subscribe [:session/matcher-method?])
        form-fields-for-type  (get
                                @(rf/subscribe [:form/fields]) @header-type-form)
        current-proxy @(rf/subscribe [:session/page])
        ]
    (.log js/console matcher method)
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
        (cond-> {:defaultValue @header-key-value
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
        (cond-> {:defaultValue @header-value
                 :onChange #(header-value-dispatcher (-> % .-target .-value))
                 :label "Header Value"
                 :id "header-value"
                 }
                header-value-error (merge {:error true :helperText header-value-error}))

        ]
       ]]
     [:> Button
      { :disabled (or (nil? method) (nil? matcher))
       :on-click #(add-header! {:header-type-form @header-type-form
                                :id current-proxy
                                :payload (assoc
                                           form-fields-for-type
                                           :matcher matcher
                                           :method method) })
       :style #js {:margin-top 20}
       :color "primary"
       :variant "contained"
       }
      "Add Header"]]))

(defn new-matcher [{
                    modal-opened :modal-opened
                    on-close     :on-close
                    }]
  (let [matcher (rf/subscribe [:form/field :new-matcher [:matcher]])
        proxy-id (rf/subscribe [:session/page])
        matcher-error @(rf/subscribe [:form/error :new-matcher [:matcher]])
        method (rf/subscribe [:form/field :new-matcher [:method]])]
    [:> Dialog
     {
      :open modal-opened
      }
     [:> Card
      [:> CardContent
       [:> Grid
        {:container true
         :direction "column"}
        [text-field
         {:attrs   {:label    "Url Matcher"
                    :id       "matcher"
                    :multiple true
                    :rowMax   4}
          :value   matcher
          :on-save #(rf/dispatch [:form/set-field :new-matcher [:matcher] %])
          :error   matcher-error}]
        [:> Select {:label "Method"
                    :on-change #(rf/dispatch [:form/set-field :new-matcher [:method] (-> % .-target .-value)])
                    :value @method}
         [:> MenuItem
          {:value "all"}
          "ALL"
          ]
         [:> MenuItem
          {:value "post"}
          "POST"
          ]
         [:> MenuItem
          {:value "get"}
          "GET"
          ]
         [:> MenuItem
          {:value "options"}
          "OPTIONS"
          ]
         [:> MenuItem
          {:value "put"}
          "PUT"
          ]
         [:> MenuItem
          {:value "patch"}
          "PATCH"]]]]
      [:> CardActions
       [:> Button
        {:on-click (fn [] (do
                            (rf/dispatch [:proxy/add-matcher! (keyword @proxy-id) @matcher (keyword @method) ])
                            (on-close)
                            ) )
         :disabled (or (nil? @matcher) (nil? @method))
         }
        "Add"]
       [:> Button
        {:on-click on-close}
        "Cancel"]]]]))

(defn update-body-form
  "Takes a type meaning request response. Returns a body form"
  []
  (let [type (rf/subscribe [:session/request-or-response?])
        page (rf/subscribe [:session/page])
        matcher (rf/subscribe [:session/matcher-regex?])
        method (rf/subscribe [:session/matcher-method?])
        body-value (rf/subscribe [:proxy/body])
        body-value-dispatcher #(rf/dispatch [:proxy/set-body! (keyword @page) @type @matcher @method %])]
      [:> Grid
       {:container true
        :direction "column"}
       [text-area
        {:attrs {:label "Body"
                 :id "body"
                 :rows 4
                 }
         :value body-value
         :on-save #(body-value-dispatcher %)}]
       [:> CardActions
        [:> Button
         {:aria-label "Add"
          :disabled  (or (nil? @method) (nil? @matcher))
          :variant "contained"
          :on-click #(change-body! {:type @type
                                    :id @page
                                    :payload {
                                              :matcher @matcher
                                              :method @method
                                              :body @body-value
                                              }})}
         [:> Add] "Update"
         ]]])
  )

