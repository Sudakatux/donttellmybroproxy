(ns donttellmybroproxy.validations
  (:require
    [struct.core :as st]
    [clojure.string :as s]))

(def proxy-entry-schema
  [[:id
    st/required
    st/string]
   [:destination
    st/required
    st/string]
   [:route
    st/required
    st/string
    {:message "routes must be prefixed with a slash"
     :validate (fn [route] (s/starts-with? route "/"))}]])

(defn validate-proxy-entry [params]
  (first (st/validate params proxy-entry-schema)))


