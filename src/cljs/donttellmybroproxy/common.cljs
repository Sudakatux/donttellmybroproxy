(ns donttellmybroproxy.common
  (:require
    ["@material-ui/core/TextField" :default TextField]
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
                       :value @value }
                      error (merge {:error true :helperText error})))])))
