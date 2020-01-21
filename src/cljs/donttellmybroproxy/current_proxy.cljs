(ns donttellmybroproxy.current_proxy
  (:require ["@material-ui/core/colors" :as mui-colors]
            ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
            ["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/core/Typography" :default Typography]
            ["@material-ui/core/ClickAwayListener" :default ClickAwayListener]
            ["@material-ui/core/Chip" :default Chip]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Card" :default Card]
            ["@material-ui/core/CardContent" :default CardContent]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/Divider" :default Divider]
            ["@material-ui/core/CardHeader" :default CardHeader]
            ["@material-ui/core/Box" :default Box]
            ["@material-ui/core/ButtonGroup" :default ButtonGroup]
            ["@material-ui/core/Popper" :default Popper]
            ["@material-ui/core/Paper" :default Paper]
            ["@material-ui/core/MenuList" :default MenuList]
            ["@material-ui/core/MenuItem" :default MenuItem]
            ["@material-ui/core/Tabs" :default Tabs]
            ["@material-ui/core/Tab" :default Tab]
            ["@material-ui/core/Select" :default Select]
            ["@material-ui/core/ListItem" :default ListItem]
            ["@material-ui/core/ListItemText" :default ListItemText]
            ["@material-ui/core/ListItemSecondaryAction" :default ListItemSecondaryAction]
            ["@material-ui/core/List" :default List]
            ["@material-ui/core/Button" :default Button]
            ["@material-ui/icons/RadioButtonChecked" :default RadioButtonChecked]
            ["@material-ui/icons/RadioButtonUnchecked" :default RadioButtonUnchecked]
            ["@material-ui/icons/ArrowDropDown" :default ArrowDropDown]
            ["@material-ui/core/Fab" :default Fab]
            [donttellmybroproxy.forms :refer [add-header-form update-body-form new-matcher]]
            [accountant.core :as accountant]
            [reagent.core :as r]
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

(rf/reg-event-db
  :proxy/start-stop-recording!
  (fn [db [_ id {:keys [record? recordings]}]]
    (let [id-path [:proxy/list (keyword id)]
          args-path [:proxy/list (keyword id) :args]]
      (-> db
          (assoc-in id-path {
                             :args (merge (get-in db args-path) {:record? record?})
                             :recordings recordings
                             })))))

(rf/reg-event-db
  :proxy/set-interceptors!
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ proxy-id interceptors]]
    (assoc-in proxy-list [proxy-id :args :interceptors ] interceptors)))

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
  :proxy/record?
  :<- [:proxy/list]
  :<- [:session/page]
  (fn [[list page]]
    (get-in list [(keyword page) :args :record?] {})))

(rf/reg-sub
  :proxy/recordings
  :<- [:proxy/list]
  :<- [:session/page]
  (fn [[list page]]
    (get-in list [(keyword page) :recordings] [])))

(rf/reg-event-fx
  :proxy/remove-header!
  (fn [_ [_ {:keys [type header-key id matcher current-headers]}]]
    {:ajax/delete {
                 :url (str "/api/proxy-server/" (name type) "/headers/" id )
                 :params {:header-key header-key
                          :matcher matcher}
                 :success-path [:list]
                 :success-event [:proxy/set-headers! (keyword id) type matcher {:headers (dissoc current-headers header-key)}]}}))

(rf/reg-event-fx
  :proxy/record!
  (fn [_ [_ {:keys [id record?]}]]
    {:ajax/post {
                :url    (str "/api/proxy-server/record/" id)
                :params {:record? record?}
                :success-event [:proxy/start-stop-recording! (keyword id)]
                }}))

(rf/reg-event-fx
  :proxy/convert-to-interceptor!
  (fn [_ [_ {:keys [id recording-idx]}]]
    {:ajax/post {
                 :success-path [:interceptors]
                 :url    (str "/api/proxy-server/" id "/recordings/" recording-idx "/to_interceptor")
                 :success-event [:proxy/set-interceptors! (keyword id)]}}))

(rf/reg-event-fx
  :proxy/upload-interceptors!
  (fn [_ [_ {:keys [id form-data]}]]
    {:ajax/post {
                 :success-path [:interceptors]
                 :url    (str "/api/proxy-server/" id "/interceptors/file")
                 :body form-data
                 :success-event [:proxy/set-interceptors! (keyword id)]}}))

(rf/reg-sub
  :proxy/response-headers
  :<- [:proxy/list]
  :<- [:session/page]
  :<- [:session/matcher?]
  (fn [[list page matcher] [_ id]]
    (get-in list [(keyword page) :args :interceptors matcher id :headers] {})))

(defn existing-header-cloud []
  (let [header-type-form @(rf/subscribe [:session/request-or-response?])
        header-values @(rf/subscribe [:proxy/response-headers header-type-form])
        type @(rf/subscribe [:session/request-or-response?])
        matcher @(rf/subscribe [:session/matcher?])
        page @(rf/subscribe [:session/page])]
    [:> Grid
     {:direction "row"}
     (into [:<>]
           (map (fn [[hk hv]]
                  ^{:key hk}
                  [:> Chip
                   {:label (str hk " : " hv)
                    :onDelete (fn []
                                (rf/dispatch [:proxy/remove-header! {:type type
                                                                     :matcher matcher
                                                                     :header-key hk
                                                                     :id page
                                                                     :current-headers header-values}]))}
                   ]) header-values))]))

(defn rec-toggle []
  (let [current_proxy @(rf/subscribe [:session/page])
        record? @(rf/subscribe [:proxy/record?])]
    [:> Button
     {:aria-label "Rec"
      :on-click (fn [] (rf/dispatch [:proxy/record! {:id current_proxy
                                                     :record? (not record?)}]))
      :startIcon (if record? (r/as-element [:> RadioButtonChecked] )   (r/as-element [:> RadioButtonUnchecked]) )
      }
     "Record"
     ]))

(defn process-file-upload [fevent]
  (let [form-data (doto
                    (js/FormData.)
                    (.append "file" (-> fevent .-target .-files (aget 0))))
        current_proxy @(rf/subscribe [:session/page])]
    (rf/dispatch [:proxy/upload-interceptors! {
                                               :id current_proxy
                                               :form-data form-data
                                               }])
    ))




(defn upload-interceptor []
  (let [!file (atom nil)]
    (fn []
       [:Button
        {:onClick (fn []
                    (when-let [file @!file]
                      (.click file)
                      ))}
        [:input {:type "file"
                 :id "file"
                 :ref (fn [el]
                        (reset! !file el))
                 :on-change process-file-upload
                 :style #js {:display "none"}
                 }]

        "Upload"
        ]

      )
    )
  )


(defn record-button [recordings]
  (let [
        anchor (r/atom nil)
        opened? (r/atom false)
        set-anchor #(reset! anchor %)]
    (fn [recordings]
        [:div
         [:> ButtonGroup {
                          :variant "contained"
                          ;         :ref anchorRef
                          }
          [:Button
           "Download"
           ]
          [upload-interceptor]
          [rec-toggle]
          [:> Button {
                      :size    "small"
                      :disabled (empty? recordings)
                      :onClick (fn [event]
                                   (when (nil? @anchor)
                                     (set-anchor (-> event .-currentTarget))
                                     )
                                    (reset! opened? true)
                                 )

                      }
           [:> ArrowDropDown]]

          ]
         [:> Popper {
                     :open          @opened?
                     :anchorEl      @anchor
                     :disablePortal true
                     }
          [:> Paper
           [:> ClickAwayListener
            {
             :onClickAway #(swap! opened? not)
             }
            [:> MenuList
             {
              :autoFocusItem @opened?
              }
             [:> MenuItem
              {
               :on-click #(accountant/navigate! (str "/proxy/postman/recordings"))
               }
              [:> Button
               "Show Recordings"
               ]
              ]
             ]
            ]
           ]
          ]
         ])))

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
      (let [matchers @(rf/subscribe [:proxy/matchers])
            recordings @(rf/subscribe [:proxy/recordings])
            selected-matcher @(rf/subscribe [:session/matcher?])]
        [:> Grid
         {
          :direction "row"
          :container true
          }
         (into [:> Select
                {:style     #js {:width "50%"}
                 :value selected-matcher
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
         [record-button recordings]]))))

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

;;;;;; Recordings

(defn recordings-proxy-layout []
  (let [recordings @(rf/subscribe [:proxy/recordings])
        current_proxy @(rf/subscribe [:session/page])]
(into [:> List
       (map-indexed (fn [idx {:keys [url method]}]
              ^{:key url}
              [:<>
                [:> ListItem {:alignItems "flex-start"}
                 [:> ListItemText {
                                   :primary url
                                   :secondary (str "Method: " (name method))
                                   }]
                 [:> ListItemSecondaryAction
                  [:> Button
                   {
                    :onClick #(rf/dispatch [:proxy/convert-to-interceptor! {:id current_proxy :recording-idx idx} ])
                    }
                      "Convert to Interceptor"
                   ]]]
                [:> Divider { :variant "inset"
                             :component "li"}]
               ]
              ) recordings)

       ])))