(ns donttellmybroproxy.app
  (:require
            ["@material-ui/core/colors" :as mui-colors]
            ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
            ["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
            [donttellmybroproxy.dashboard :refer [main-layout empty-content create-proxy-form]]
            [ajax.core :refer [GET POST PUT DELETE]]
            [donttellmybroproxy.validations :refer [validate-proxy-entry]]
            [re-frame.core :as rf]
            [reagent.core :as r :refer [atom as-element render-component]]
            [donttellmybroproxy.add-header :refer [add-header-card]]
            [reitit.frontend :as reitit]
            [reagent.session :as session]
            [accountant.core :as accountant]))

;; API Internal Event Handlers
(rf/reg-fx
  :ajax/get
  (fn [{:keys [url success-event error-event success-path]}]
    (GET url
         (cond-> {:headers {"Accept" "application/transit+json"}}
                 success-event (assoc :handler
                                      #(rf/dispatch
                                         (conj success-event
                                               (if success-path
                                                 (get-in % success-path)
                                                 %))))
                 error-event  (assoc :error-handler
                                     #(rf/dispatch (conj error-event %)))))))

(rf/reg-fx
  :ajax/put
  (fn [{:keys [url success-event error-event params]}]
    (PUT url
         (cond-> {:headers {"Accept" "application/transit+json"}}
                 params (assoc :params params)
                 success-event (assoc :handler #(rf/dispatch success-event))
                 error-event  (assoc :error-handler #(rf/dispatch (conj error-event %)))))))
(rf/reg-fx
  :ajax/delete
  (fn [{:keys [url success-event error-event params]}]
    (DELETE url
         (cond-> {:headers {"Accept" "application/transit+json"}}
                 params (assoc :params params)
                 success-event (assoc :handler #(rf/dispatch success-event))
                 error-event  (assoc :error-handler #(rf/dispatch (conj error-event %)))))))


(rf/reg-fx
  :ajax/post
  (fn [{:keys [url params success-event error-event success-path]}]
    (POST url
          (cond-> {:headers {"Accept" "application/transit+json"}
                   :params params}
                  success-event (assoc :handler
                                       #(rf/dispatch
                                          (conj success-event
                                                (if success-path
                                                  (get-in % success-path)
                                                  %))))
                  error-event  (assoc :error-handler
                                      #(rf/dispatch (conj error-event %)))))))
;; End Api Internal events


;; Reducers DB-Events
(rf/reg-event-db
  :server/set-status
  (fn [db [_ returned-status]]
      (-> db
          (assoc :server/started? returned-status))))

(rf/reg-event-db
  :proxy/set-proxy-list
  (fn [db [_ list]]
    (-> db
        (assoc :proxy/list list))))

(rf/reg-event-db
  :proxy/list-remove
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ item]]
    (dissoc proxy-list item)))

(rf/reg-event-db
  :proxy-form/set-field
  [(rf/path :proxy-form/fields)]
  (fn [fields [_ id value]]
    (assoc fields id value)))

(rf/reg-event-db
  :proxy-form/clear-fields
  [(rf/path :proxy-form/fields)]
  (fn [_ _]
    {}))

;; End Reducers

;; Selectors ---- subscribers
(rf/reg-sub
  :proxy-form/fields
  (fn [db _]
    (:proxy-form/fields db)))

(rf/reg-sub
  :proxy-form/field
  :<- [:proxy-form/fields]
  (fn [fields [_ id]]
    (get fields id)))

(rf/reg-sub
  :proxy-form/errors
  (fn [db _]
    (:proxy-form/errors db)))

(rf/reg-sub
  :proxy-form/error
  :<- [:proxy-form/errors]
  (fn [errors [_ id]]
    (get errors id)))

(rf/reg-sub
  :proxy/list
  (fn [db _]
      (:proxy/list db [])))

(rf/reg-sub
  :proxy/response-headers
  :<- [:proxy/list]
  (fn [list [_ id]]
    (get-in list [id :args :response :headers])))

(rf/reg-sub
  :server/started?
  (fn [db _]
      (:server/started? db)))
;; End Selectors

;; External effects
(rf/reg-event-fx
  :app/initialize
  (fn [_ _]
      {:db {:server/started? false
            :proxy/list []
            :proxy-form/errors {}}
       :dispatch-n  (list [:proxy/load-list] [:server/load-status])}))

(rf/reg-event-fx
  :proxy/load-list
  (fn [_ _]
    {:ajax/get {
                :url "/api/proxy-server/list"
                :success-path [:list]
                :success-event [:proxy/set-proxy-list]}}))

(rf/reg-event-fx
  :server/load-status
  (fn [_ _]
    {:ajax/get {
                :url "/api/proxy-server/status"
                :success-path [:result :server-running]
                :success-event [:server/set-status]}}))

(rf/reg-event-fx
  :proxy/remove-from-list!
  (fn [_ [_ elem]]
    {:ajax/delete {
                   :url (str "/api/proxy-server/delete/" (name elem))
                   :success-event [:proxy/list-remove elem]}}))

(rf/reg-event-fx
  :server/stop!
  (fn [_ _]
    {:ajax/put {
                :url "/api/proxy-server/stop"
                :success-event [:server/set-status false]}}))

(rf/reg-event-fx
  :server/start!
  (fn [_ _]
    {:ajax/put {
                :url "/api/proxy-server/start"
                :params {:port 3001 }           ; This should be configurable
                :success-event [:server/set-status true]}}))

(rf/reg-event-fx
  :proxy/add-to-list!
  (fn [{:keys [db]} [_ proxy-payload]]
    (if-let [validation-errors (validate-proxy-entry proxy-payload)]
    {:db (assoc db :proxy-form/errors validation-errors) }
    {:ajax/post {
                 :url "/api/proxy-server/create"
                 :params proxy-payload
                 :success-path [:list]
                 :success-event [:proxy/set-proxy-list]}})))

;; TODO to be implemented
;; Should clear fields and add to list
(rf/reg-event-fx
  :proxy-server/add-proxy-server
  (fn [_ _]
    {:db {:server/started? false}}))

(def router
  (reitit/router
    [["/" :index]
     ["/create" :create]]))

;; TODO add routed param for selected proxy
;;
(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))



(defn page-for [route]
  (.log js/console "this is the route" route)
  (case route
    :index #'add-header-card
    :create #'create-proxy-form))

(defn custom-theme []
      (createMuiTheme
        (clj->js
          { :palette
           {:type       "light"
            :primary    (.-blue mui-colors)
            :secondary  (.-amber mui-colors)}
           :typography
           { :useNextVariants true}})))

(defn app []
  (let [page (:current-page (session/get :route))]
    [:> ThemeProvider
     {:theme (custom-theme)}
     [main-layout
      {:main-content page}]]
    ))

(defn mount-app []
      (render-component
        [app]
        (js/document.getElementById "content")))

(defn init! []
      (.log js/console "Initializing App...")
      (rf/dispatch [:app/initialize])
      (accountant/configure-navigation!
        {:nav-handler
         (fn [path]
           (let [match (reitit/match-by-path router path)
                 current-page (:name (:data match))
                 route-params (:path-params match)]
             (session/put! :route {:current-page (page-for current-page)
                                   :route-params route-params}))) ; TODO use re-frame instead
         :path-exists?
         (fn [path]
           (boolean (reitit/match-by-path router path)))})
      (accountant/dispatch-current!)
      (mount-app))

(init!)

;(js/alert "Hello from shadow-cljs")

