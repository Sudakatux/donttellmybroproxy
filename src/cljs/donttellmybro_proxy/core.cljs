(ns donttellmybro-proxy.core
  (:require [reagent.core :as r]
            ["@material-ui/core" :as mui]
            ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
            ["@material-ui/core/colors" :as mui-colors]
            ["@material-ui/icons" :as mui-icons]
            [ajax.core :refer [GET]]
            [goog.object :as gobj]
            [reagent.impl.template :as rtpl]))

(enable-console-print!)

(println "This text is printed from src/donttellmybro-proxy/core.cljs. Go ahead and edit it and see reloading in action.")

;; TextField cursor fix:

(def ^:private input-component
  (r/reactify-component
   (fn [props]
     [:input (-> props
                 (assoc :ref (:inputRef props))
                 (dissoc :inputRef))])))

(def ^:private textarea-component
  (r/reactify-component
   (fn [props]
     [:textarea (-> props
                    (assoc :ref (:inputRef props))
                    (dissoc :inputRef))])))

;; To fix cursor jumping when controlled input value is changed,
;; use wrapper input element created by Reagent instead of
;; letting Material-UI to create input element directly using React.
;; Create-element + convert-props-value is the same as what adapt-react-class does.
(defn text-field [props & children]
  (let [props (-> props
                  (assoc-in [:InputProps :inputComponent] (cond
                                                            (and (:multiline props) (:rows props) (not (:maxRows props)))
                                                            textarea-component

                                                            ;; FIXME: Autosize multiline field is broken.
                                                            (:multiline props)
                                                            nil

                                                            ;; Select doesn't require cursor fix so default can be used.
                                                            (:select props)
                                                            nil

                                                            :else
                                                            input-component))
                  rtpl/convert-prop-value)]
    (apply r/create-element mui/TextField props (map r/as-element children)))) ;turns a react component into something that can be ussed in hiccup





(def custom-theme
  (createMuiTheme
   #js {:palette #js {:primary #js {:main (gobj/get (.-red mui-colors) 100)}}}))

(defn custom-styles [theme]
  #js {:button #js {:margin (.. theme -spacing -unit)}
       :textField #js {:width 200
                       :marginLeft (.. theme -spacing -unit)
                       :marginRight (.. theme -spacing -unit)}})

(def with-custom-styles (withStyles custom-styles))

(defonce form-state (r/atom  "fooo"))

;; define your app data so that it doesn't get over-written on reload
(defn form [{:keys [classes] :as props}]
  [:> mui/Grid
   {:container true
    :direction "column"
    :spacing 16}

   ;; [:> mui/Grid {:item true}
   ;;  [:> mui/Toolbar
   ;;   {:disable-gutters true}
   ;;   [:> mui/Button
   ;;    {:variant "contained"
   ;;     :color "primary"
   ;;     :class (.-button classes)
   ;;     :on-click #(swap! text-state str " foo")}
   ;;    "Update value property"
   ;;    [:> mui-icons/AddBox]]

   ;;   [:> mui/Button
   ;;    {:variant "outlined"
   ;;     :color "secondary"
   ;;     :class (.-button classes)
   ;;     :on-click #(reset! text-state "")}
   ;;    "Reset"
   ;;    [:> mui-icons/Clear]]]]

   [:> mui/Grid {:item true}
    [text-field
     {:value @form-state
      :label "Proxy host address"
      :placeholder "address"
      :helper-text "Enter api url to redirect to"
      :class (.-textField classes)
      :on-change (fn [e]
                   (reset! form-state (.. e -target -value)))
      :inputRef #(js/console.log "input-ref" %)}]]

  ;; [:> mui/Grid {:item true}
  ;;   [text-field
  ;;    {:value @text-state
  ;;     :label "Textarea"
  ;;     :placeholder "Placeholder"
  ;;     :helper-text "Helper text"
  ;;     :class (.-textField classes)
  ;;     :on-change (fn [e]
  ;;                  (reset! text-state (.. e -target -value)))
  ;;     :multiline true
  ;;     ;; TODO: Autosize textarea is broken.
  ;;     :rows 10}]]

   [:> mui/Grid {:item true}
    [text-field
     {:value  @form-state
      :label "Select"
      :placeholder "Placeholder"
      :helper-text "Helper text"
      :class (.-textField classes)
      :on-change (fn [e]
                   (reset! form-state (.. e -target -value)))
      :select true}
     [:> mui/MenuItem
      {:value 1}
      "Item 1"]
     ;; Same as previous, alternative to adapt-react-class
     [:> mui/MenuItem
      {:value 2}
      "Item 2"]]]

   [:> mui/Grid {:item true}
    [:> mui/Grid
     {:container true
      :direction "row"
      :spacing 8}

     ;; For properties that require React Node as parameter,
     ;; either use r/as-element to convert Reagent hiccup forms into React elements,
     ;; or use r/create-element to directly instantiate element from React class (i.e. non-adapted React component).
     [:> mui/Grid {:item true}
      [:> mui/Chip
       {:icon (r/as-element [:> mui-icons/Face])
        :label "Icon element example, r/as-element"}]]

     [:> mui/Grid {:item true}
      [:> mui/Chip
       {:icon (r/create-element mui-icons/Face)
        :label "Icon element example, r/create-element"}]]]]])


(defn main []
  ;; fragment
  [:<>
   [:> mui/CssBaseline]
   [:> mui/MuiThemeProvider
    {:theme custom-theme}
    [:> mui/Grid
     {:container true
      :direction "row"
      :justify "center"}
     [:> mui/Grid
      {:item true
       :xs 6}
      [:> (with-custom-styles (r/reactify-component form))]]]]])

(r/render [main]
                (. js/document (getElementById "app")))




(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
