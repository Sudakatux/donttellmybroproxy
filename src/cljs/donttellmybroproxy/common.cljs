(ns donttellmybroproxy.common
  (:require
    ["@material-ui/core/TextField" :default TextField]
    ["@material-ui/lab/Autocomplete" :default Autocomplete]
    ["@material-ui/icons/RadioButtonChecked" :default RadioButtonChecked]
    ["@material-ui/icons/RadioButtonUnchecked" :default RadioButtonUnchecked]
    ["@material-ui/icons/ArrowDropDown" :default ArrowDropDown]
    ["@material-ui/core/Grid" :default Grid]
    ["@material-ui/core/Grow" :default Grow]
    ["@material-ui/core/Box" :default Box]
    ["@material-ui/core/Button" :default Button]
    ["@material-ui/core/ButtonGroup" :default ButtonGroup]
    ["@material-ui/core/Popper" :default Popper]
    ["@material-ui/core/Fab" :default Fab]
    [hx.react :as hx :refer [defnc]]
    [hx.hooks :as hooks]
    [cljs-bean.core :refer [bean ->js]]
    [reagent.core :as r :refer [atom]]))

(defn text-field [{val :value
                   :keys [on-save]}]
  (let [draft (r/atom nil)
        value (r/track #(or @draft @val ""))]
    (fn [{props :attrs
          error :error}]
      [:> TextField
       (merge props
              (cond-> {
                       :on-focus #(reset! draft (or @val ""))
                       :on-blur (fn []
                                  (on-save (or @draft ""))
                                  (reset! draft nil))
                       :on-change #(reset! draft (.. % -target -value))
                       :defaultValue @value }
                      error (merge {:error true :helperText error})))])))

(defn text-area [{val :value
                   :keys [on-save]}]
  (let [draft (r/atom nil)
        value (r/track #(or @draft @val ""))]
    (fn [{props :attrs
          error :error}]
      [:textarea
       (merge props
              (cond-> {
                       :on-focus #(reset! draft (or @val ""))
                       :on-blur (fn []
                                  (on-save (or @draft "")))
                       :on-change #(reset! draft (.. % -target -value))
                       :value @value
                       }
                      error (merge {:error true :helperText error})))
       ])))

;(defnc HeaderAutocomplete [{initial-value :initialValue
;                            options :options
;                            on-save :onSave
;                            }]
;       (let [[val updateVal] (hooks/useState initial-value)
;             renderInput (fn [params]
;                           (let [params-clj (bean params)
;                                 inputProps-clj (:inputProps params-clj)
;                                 option-selected (not-empty (:value (bean inputProps-clj)))
;                                 selected-value (or option-selected val "")]
;                             (hx/f [TextField (merge  params-clj {
;                                                                  :label "Insert header"
;                                                                  :fullWidth true
;                                                                  :inputProps (.assign js/Object inputProps-clj (->js { :onChange #(updateVal (-> % .-target .-value))
;                                                                                                                       :value selected-value
;                                                                                                                       :onBlur #(on-save selected-value)
;                                                                                                                       :autoComplete "off"
;                                                                                                                       }))
;                                                                  :autoComplete "off"
;                                                                  :variant "outlined"} )]))
;                           )]
;
;         [Autocomplete {:id "autocomplete"
;                        :options (->js options)
;                        :renderInput renderInput
;                        :getOptionLabel (fn [elem]
;                                          (:title (bean elem) ))
;                        :freeSolo true }]))
