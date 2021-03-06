(ns donttellmybroproxy.app
  (:require
            ["@material-ui/core/colors" :as mui-colors]
            ["@material-ui/core/styles" :refer [createMuiTheme withStyles ThemeProvider]]
    ;["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
            [donttellmybroproxy.dashboard :refer [main-layout empty-content]]
            [donttellmybroproxy.forms :refer [create-proxy-form]]
            [ajax.core :refer [GET POST PUT DELETE]]
            [re-frame.core :as rf]
            [reagent.core :as r :refer [atom as-element]]
            [reagent.dom :as rdom]
            [donttellmybroproxy.current_proxy :refer [card-container recordings-proxy-layout]]
            [reitit.frontend :as reitit]
            [reagent.session :as session]
            [accountant.core :as accountant]))


(def default_port 3001)

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
  (fn [{:keys [url params body success-event error-event success-path]}]
    (POST url
          (cond-> {:headers {"Accept" "application/transit+json"}}
                  params (assoc :params params)
                  body (assoc :body body)
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
  :session/set-page!
  (fn [db [_ page-info]]
    (-> db
        (assoc :session/page page-info))))

(rf/reg-event-db
  :proxy/set-port!
  (fn [db [_ port]]
    (-> db
        (assoc :proxy/port port))))

(rf/reg-event-db
  :proxy/list-remove
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ item]]
    (dissoc proxy-list item)))

(rf/reg-sub
  :proxy/list
  (fn [db _]
      (:proxy/list db [])))

(rf/reg-sub
  :proxy/port
  (fn [db _]
    (:proxy/port db default_port)))

(rf/reg-sub
  :session/page
  (fn [db _]
    (:session/page db)))

(rf/reg-sub
  :server/started?
  (fn [db _]
      (:server/started? db)))
;; End Selectors

;; External effects
;;; INITALIZE EFFECT
(rf/reg-event-fx
  :app/initialize
  (fn [_ _]
      {:db {:server/started? false
            :proxy/list {}
            :proxy-form/errors {}
            :session/page nil
            :proxy/port default_port
            }
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
                   :url (str "/api/proxy-server/" (name elem) "/delete" )
                   :success-event [:proxy/list-remove elem]}}))

(rf/reg-event-fx
  :server/stop!
  (fn [_ _]
    {:ajax/put {
                :url "/api/proxy-server/stop"
                :success-event [:server/set-status false]}}))

(rf/reg-event-fx
  :server/start!
  (fn [_ [_ port]]
    {:ajax/put {
                :url "/api/proxy-server/start"
                :params {:port (js/parseInt port)}
                :success-event [:server/set-status true]}}))
;; TODO to be implemented
;; Should clear fields and add to list
(rf/reg-event-fx
  :proxy-server/add-proxy-server
  (fn [_ _]
    {:db {:server/started? false}}))

(def router
  (reitit/router
    [["/" :index]
     ["/create" :create]
     ["/proxy/:id" :proxy-route]
     ["/proxy/:id/recordings" :proxy-route-recordings]]))

(defn page-for [route]
  (case route
    :index #'empty-content
    :create #'create-proxy-form
    :proxy-route #'card-container
    :proxy-route-recordings #'recordings-proxy-layout
    ))

(defn custom-theme []
      (createMuiTheme
        (clj->js
          {
           :palette
           {:type "light"
             :primary    (.-blue mui-colors)
            :secondary  (.-amber mui-colors)
            }
           :typography
           {:useNextVariants true}})))

(defn app []
  (let [page (:current-page (session/get :route))]

    [:> ThemeProvider
     {:theme (custom-theme)}
     [main-layout
      {:main-content page}]]
    ))

(defn mount-app []
      (rdom/render
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
             (rf/dispatch [:session/set-page! (get-in route-params [:id])]) ; TODO rename me page is id proxy-id or something
             (session/put! :route {:current-page (page-for current-page)
                                   :route-params route-params}))) ; TODO use re-frame instead
         :path-exists?
         (fn [path]
           (boolean (reitit/match-by-path router path)))})
      (accountant/dispatch-current!)
      (mount-app))

(init!)

;(js/alert "Hello from shadow-cljs")

