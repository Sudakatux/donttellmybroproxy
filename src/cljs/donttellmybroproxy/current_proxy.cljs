(ns donttellmybroproxy.current_proxy
  (:require ["@material-ui/core/colors" :as mui-colors]
            ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
            ["@material-ui/core/styles/MuiThemeProvider" :default ThemeProvider]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/core/Typography" :default Typography]
            ["@material-ui/core/ClickAwayListener" :default ClickAwayListener]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Card" :default Card]
            ["@material-ui/core/ExpansionPanel" :default ExpansionPanel]

            ["@material-ui/core/ExpansionPanelDetails" :default ExpansionPanelDetails]
            ["@material-ui/core/ExpansionPanelSummary" :default ExpansionPanelSummary]
            ["@material-ui/core/ExpansionPanelActions" :default ExpansionPanelActions]

            ["@material-ui/core/CardContent" :default CardContent]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/Divider" :default Divider]
            ["@material-ui/core/CardHeader" :default CardHeader]
            ["@material-ui/core/Box" :default Box]
            ["@material-ui/core/ButtonGroup" :default ButtonGroup]
            ["@material-ui/core/IconButton" :default IconButton]
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
            ["@material-ui/icons/Delete" :default DeleteIcon]
            ["@material-ui/icons/GetApp" :default GetApp]
            ["@material-ui/core/Switch" :default Switch]
            ["@material-ui/icons/Publish" :default Publish]
            ["@material-ui/icons/RadioButtonChecked" :default RadioButtonChecked]
            ["@material-ui/icons/RadioButtonUnchecked" :default RadioButtonUnchecked]
            ["@material-ui/icons/ArrowDropDown" :default ArrowDropDown]
            ["@material-ui/icons/ClearAll" :default ClearAllIcon]
            ["@material-ui/core/Fab" :default Fab]
            ["@material-ui/core/FormControlLabel" :default FormControlLabel]
            [donttellmybroproxy.forms :refer [add-header-form update-body-form new-matcher]]
            [accountant.core :as accountant]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(rf/reg-event-db
  :session/set-matcher-regex!
  [(rf/path :session/matcher?)]
  (fn [matcher [_ regex]]
    (assoc matcher :regex regex)))

(rf/reg-event-db
  :session/set-matcher-method!
  [(rf/path :session/matcher?)]
  (fn [matcher [_ method]]
    (assoc matcher :method (keyword method))))

(rf/reg-event-db
  :session/set-request-or-response!
  (fn [db [_ type]]
    (-> db
        (assoc :session/request-or-response? type))))

(rf/reg-event-db
  :proxy/clear-recordings-for-id!
  (fn [db [_ id]]
    (-> db
        (update-in [:proxy/list (keyword id)] :recordings []))))

(rf/reg-event-db
  :proxy/start-stop-recording!
  (fn [db [_ id {:keys [record? recordings]}]]
    (let [id-path [:proxy/list (keyword id)]
          args-path [:proxy/list (keyword id) :args]]
      (-> db
          (assoc-in id-path {
                             :args       (merge (get-in db args-path) {:record? record?})
                             :recordings recordings
                             })))))

(rf/reg-event-db
  :proxy/set-interceptors!
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ proxy-id interceptors]]
    (assoc-in proxy-list [proxy-id :args :interceptors] interceptors)))

(rf/reg-event-db
  :proxy/set-make-request!
  [(rf/path :proxy/list)]
  (fn [proxy-list [_ proxy-id make-request?]]
    (assoc-in proxy-list [proxy-id :args :make-request?] make-request?)))

(rf/reg-sub
  :proxy/matchers
  :<- [:proxy/list]
  :<- [:session/page]
  (fn [[list page]]
    (get-in list [(keyword page) :args :interceptors] {})))

(rf/reg-sub
  :proxy/make-request?
  :<- [:proxy/list]
  :<- [:session/page]
  (fn [[list page]]
    (get-in list [(keyword page) :args :make-request?] false)))

(rf/reg-sub
  :proxy/matchers-methods
  :<- [:proxy/list]
  :<- [:session/page]
  :<- [:session/matcher-regex?]
  (fn [[list page regex]]
    (keys (get-in list [(keyword page) :args :interceptors regex] {}))))

(rf/reg-sub
  :session/request-or-response?
  (fn [db _]
    (keyword (get db :session/request-or-response? "response"))))

(rf/reg-sub
  :session/matcher?
  (fn [db _]
    (get db :session/matcher? {})))

(rf/reg-sub
  :session/matcher-regex?
  (fn [db _]
    (get-in db [:session/matcher? :regex] "")))

(rf/reg-sub
  :session/matcher-method?
  (fn [db _]
    (get-in db [:session/matcher? :method] "")))

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
                   :url           (str "/api/proxy-server/" id "/headers/" (name type))
                   :params        {:header-key header-key
                                   :matcher    (get matcher :regex)
                                   :method     (get matcher :method)}
                   :success-path  [:list]
                   :success-event [:proxy/set-headers! (keyword id) type (get matcher :regex) (get matcher :method) {:headers (dissoc current-headers header-key)}]}}))

(rf/reg-event-fx
  :proxy/record!
  (fn [_ [_ {:keys [id record?]}]]
    {:ajax/post {
                 :url           (str "/api/proxy-server/" id "/record" )
                 :params        {:record? record?}
                 :success-event [:proxy/start-stop-recording! (keyword id)]
                 }}))

(rf/reg-event-fx
  :proxy/convert-to-interceptor!
  (fn [_ [_ {:keys [id recording-idx]}]]
    {:ajax/post {
                 :success-path  [:interceptors]
                 :url           (str "/api/proxy-server/" id "/recordings/" recording-idx "/to_interceptor")
                 :success-event [:proxy/set-interceptors! (keyword id)]}}))

(rf/reg-event-fx
  :proxy/clear-recordings!
  (fn [_ [_ id]]
    {:ajax/delete {
                 :url           (str "/api/proxy-server/" id "/recordings")
                 :success-event [:proxy/clear-recordings-for-id! (keyword id)]}}))

(rf/reg-event-fx
  :proxy/upload-interceptors!
  (fn [_ [_ {:keys [id form-data]}]]
    {:ajax/post {
                 :success-path  [:interceptors]
                 :url           (str "/api/proxy-server/" id "/interceptors/file")
                 :body          form-data
                 :success-event [:proxy/set-interceptors! (keyword id)]}}))

(rf/reg-event-fx
  :proxy/update-make-request!
  (fn [_ [_ {:keys [id make-request?]}]]
    {:ajax/put {
                 :url           (str "/api/proxy-server/" id "/should-make-request")
                 :params          {:make-request? make-request?}
                 :success-event [:proxy/set-make-request! (keyword id) make-request?]}}))

(rf/reg-sub
  :proxy/response-headers
  :<- [:proxy/list]
  :<- [:session/page]
  :<- [:session/matcher?]
  :<- [:session/request-or-response?]
  (fn [[list page {:keys [regex method]} type] [_ _]]
    (get-in list [(keyword page) :args :interceptors regex method type :headers] {})))

(rf/reg-sub
  :proxy/body
  :<- [:proxy/list]
  :<- [:session/page]
  :<- [:session/matcher?]
  :<- [:session/request-or-response?]
  (fn [[list page {:keys [regex method]} type] [_ _]]
    (get-in list [(keyword page) :args :interceptors regex method type :body] "")))


(defn existing-header-grid []
  (let [header-values (rf/subscribe [:proxy/response-headers])
        type (rf/subscribe [:session/request-or-response?])
        matcher (rf/subscribe [:session/matcher?])
        page (rf/subscribe [:session/page])
        row-item-props {:item true
                        :xs 4
                        }
        row-container-props {:container true
                             :item true
                             :direction "row"
                             :spacing 4
                             :xs 12
                             :align-items "center"
                             :style #js {:border-bottom "1px #d5d5d5 solid"
                                         :padding "1 rem"}
                             }
        header-title-props { :component "strong" }]


[:> Grid
 {:container true
  :direction "column"
  :spacing 4
  :xs 12
  :style #js {:border-top "1px #d5d5d5 solid"
              :width "100%"}
  :align-items "center"
  }
 [:> Grid
  row-container-props
  [:> Grid
   row-item-props
   [:> Typography
      header-title-props
      "HEADER KEY"
    ]
   ]
  [:> Grid
   row-item-props
   [:> Typography
    header-title-props
    "HEADER VALUE"
    ]
   ]
  [:> Grid
   row-item-props
   [:> Typography
    header-title-props
    "ACTIONS"
    ]
   ]
  ]
 [:> Box {:style #js {
                      :overflow "auto"
                      :max-height "20rem",
                      :border-top "1px #d5d5d5 solid",
                      :padding "1rem"
                      :width "100%"
                      }
          :mt 2}
     (into [:<>]
           (map (fn [[header-key header-value]]
                  ^{:key header-key}
                  [:> Grid
                   row-container-props
                  [:> Grid
                   row-item-props
                   header-key]
                   [:> Grid
                    row-item-props
                    header-value]
                   [:> Grid
                    row-item-props
                    [:> IconButton
                     {:edge "end"
                      :on-click (fn []
                                  (rf/dispatch [:proxy/remove-header! {:type            @type
                                                                       :matcher         @matcher
                                                                       :header-key      header-key
                                                                       :id              @page
                                                                       :current-headers @header-values}]))
                      }
                     [:> DeleteIcon
                      ]]

                    ]
                   ]) @header-values))
 ]]
))


(defn rec-toggle []
  (let [current_proxy @(rf/subscribe [:session/page])
        record? @(rf/subscribe [:proxy/record?])]
    [:> Button
     {:aria-label "Rec"
      :on-click   (fn [] (rf/dispatch [:proxy/record! {:id      current_proxy
                                                       :record? (not record?)}]))
      :startIcon  (if record?
                    (r/as-element [:> RadioButtonChecked])
                    (r/as-element [:> RadioButtonUnchecked]))
      }
     "Record"]))

(defn process-file-upload [fevent]
  (let [form-data (doto
                    (js/FormData.)
                    (.append "file" (-> fevent .-target .-files (aget 0))))
        current_proxy @(rf/subscribe [:session/page])]
    (rf/dispatch [:proxy/upload-interceptors! {
                                               :id        current_proxy
                                               :form-data form-data
                                               }])))

(defn upload-interceptor []
  (let [!file (atom nil)]
    (fn []
      [:> Button
       {:onClick   (fn []
                     (when-let [file @!file]
                       (.click file)
                       ))
        :startIcon (r/as-element [:> Publish])
        }
       [:input {:type      "file"
                :id        "file"
                :ref       (fn [el]
                             (reset! !file el))
                :on-change process-file-upload
                :style     #js {:display "none"}
                }]
       "Upload"
       ]
      )
    )
  )

(defn record-button []
  (let [
        anchor (r/atom nil)
        opened? (r/atom false)
        set-anchor #(reset! anchor %)]
    (fn [recordings]
      (let [current_proxy @(rf/subscribe [:session/page])]
        [:div
         [:> ButtonGroup {
                          :variant "contained"
                          ;         :ref anchorRef

                          }

          [:> Button {:startIcon (r/as-element [:> GetApp])}
           [:a
            {:href  (str "/api/proxy-server/" current_proxy "/interceptors/file")
             :style #js {:text-decoration "none"
                         :color           "inherit"}}
            "Download"
            ]
           ]
          [upload-interceptor]
          [rec-toggle]
          [:> Button {
                      :size     "small"
                      :disabled (empty? recordings)
                      :onClick  (fn [event]
                                  (when (nil? @anchor)
                                    (set-anchor (-> event .-currentTarget))
                                    )
                                  (reset! opened? true)
                                  )

                      }
           [:> ArrowDropDown]]]
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
               :on-click #(accountant/navigate! (str "/proxy/" current_proxy "/recordings"))
               }
              [:> Button
               "Show Recordings"
               ]]]]]]]))))

(defn single-header-configuration []
  [:<>
   [:> Grid
    {:direction "row"
     :container true}
    [:> Grid {:item true}
     [existing-header-grid]
     ]
    [:> Grid
     {:xs 8
      :item true}
     [add-header-form]
     ]
    ]])

(defn edit-header-section []

    [:> ExpansionPanel
     {:style #js {:maxWidth 1000}}
     [:> ExpansionPanelSummary
      "Headers"
      ]
     [:> ExpansionPanelDetails
      [single-header-configuration]]]
)

(defn edit-body-section []
  [:> ExpansionPanel
   {:style #js {:maxWidth 1000}}
   [:>  ExpansionPanelSummary
    "Body"]
   [:> ExpansionPanelDetails
      [update-body-form]
     ]])

(defn matcher-menu []
  (let [
        modal-state (r/atom false)
        close-modal #(reset! modal-state false)
        open-modal (fn [] (reset! modal-state true))]
    (fn []
      (let [matchers (rf/subscribe [:proxy/matchers])
            methods (rf/subscribe [:proxy/matchers-methods])
            selected-matcher-regex (rf/subscribe [:session/matcher-regex?])
            selected-matcher-method (rf/subscribe [:session/matcher-method?])]
        [:> Grid
         {
          :direction "row"
          :container true
          }
         (into [:> Select
                {
                 :style     #js {:width "25%"}
                 :value     @selected-matcher-regex
                 :on-change #(rf/dispatch [:session/set-matcher-regex! (-> % .-target .-value)])}]
               (map (fn [matcher]
                      ^{:key matcher}
                      [:> MenuItem
                       {:value matcher}
                       matcher
                       ]) (keys @matchers)))
         (into [:> Select
                {
                 :label     "Method"
                 :style     #js {:width "25%"}
                 :value     (if (not (nil? @selected-matcher-method)) (name @selected-matcher-method) "")
                 :on-change #(rf/dispatch [:session/set-matcher-method! (-> % .-target .-value keyword)])}]
               (map (fn [method]
                      ^{:key (str @selected-matcher-regex method)}
                      [:> MenuItem
                       {:value (name method)}
                       (name method)
                       ]) @methods))
         [:> Button
          {:on-click open-modal}
          "Add Matcher"]
         [new-matcher {:modal-opened @modal-state
                       :on-close     close-modal}]
         ]))))

(defn card-container []
  (let [type (rf/subscribe [:session/request-or-response?])
        recordings (rf/subscribe [:proxy/recordings])
        page @(rf/subscribe [:session/page])
        make-request? (rf/subscribe [:proxy/make-request?])
        update-make-request (fn [] (rf/dispatch [:proxy/update-make-request! {:id page :make-request? (not @make-request?)}] ))
        ]
    [:> Grid
     {:container true
      :direction "column"
      :spacing 2
      }
     [:> Grid
      {
       :item true
       :xs   12
       }
      [matcher-menu]
      ]
     [:> Grid
      {:item true}
      [record-button @recordings]
      ]
     [:> Grid
      {:item true}
      [:> FormControlLabel
       {:control (r/create-element Switch #js {:checked @make-request? :onChange update-make-request})
        :label "Make Request"}
       ]
      ]
     [:> Grid
      {:item true}
      [:> Tabs
       {:value @type
        :onChange (fn [_ newType] (rf/dispatch [:session/set-request-or-response! newType]))}
       [:> Tab {:label "response" :value :response}]
       [:> Tab {:label "request" :value :request}]]
      ]
     [:> Grid {:item true}
      [:> Grid
       {:container true
        :direction "column"
        :spacing   2}
       [:> Grid
        {:item true}
        [edit-header-section]

        ]
       [:> Grid
        {:item true}
        [edit-body-section]
        ]]
      ]
     ]))

;;;;;; Recordings

(defn recordings-proxy-layout []
  (let [recordings @(rf/subscribe [:proxy/recordings])
        current_proxy @(rf/subscribe [:session/page])]
    [:> Grid {:container true}
     [:> Grid {:item true}
      [:> Button {
                  :size     "small"
                  :disabled (empty? recordings)
                  :on-click #(rf/dispatch [:proxy/clear-recordings! current_proxy])
                  }
       "Clear Recordings" [:> ClearAllIcon]]]
     [:> Grid {:item true :xs 12}
      (into [:> List
             (map-indexed (fn [idx {:keys [url method]}]
                            ^{:key url}
                            [:<>
                             [:> ListItem {:alignItems "flex-start"}
                              [:> ListItemText {
                                                :primary   url
                                                :secondary (str "Method: " (name method))
                                                }]
                              [:> ListItemSecondaryAction
                               [:> Button
                                {
                                 :onClick #(rf/dispatch [:proxy/convert-to-interceptor! {:id current_proxy :recording-idx idx}])
                                 }
                                "Convert to Interceptor"
                                ]]]
                             [:> Divider {:variant   "inset"
                                          :component "li"}]
                             ]
                            ) recordings)
             ])
      ]
     ]

    ))