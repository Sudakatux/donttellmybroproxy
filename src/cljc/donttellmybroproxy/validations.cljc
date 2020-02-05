(ns donttellmybroproxy.validations
  (:require
    [struct.core :as st]
    [clojure.string :as s]))

(def proxy-entry-schema
  [
   [:id
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

(def header-schema
  [
   [:matcher
    st/required]
   [:method
    st/required]
   [:header-key
    st/required
    st/string]
   [:header-value
    st/required
    st/string
    ]])

(def body-schema
  [[:matcher
    st/required]
   [:body
    st/required]])


(defn validate-proxy-entry [params]
  (first (st/validate params proxy-entry-schema)))

(defn validate-header-schema [params]
  (first (st/validate params header-schema)))

(defn validate-body [params]
  (first (st/validate params body-schema)))



