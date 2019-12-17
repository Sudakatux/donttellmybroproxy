(ns donttellmybroproxy.proxy-test
  (:use midje.sweet)
  (:use donttellmybroproxy.proxy)
  (:import (java.io ByteArrayInputStream))
  )


(def sample-header-interceptor
  {:headers {"Content-Type" "This one", "Bareer" "123213"}})

(def some-body-interceptor
  {:body "This is a replaced body"})

(def sample-matcher
  {".*" {:response sample-header-interceptor}})

(def less-generic-matcher
  {".*a=2" {:response some-body-interceptor}})

(def test-state-map
  {:yahoo {:route "/yahoo",
           :url "http://www.yahoo.com",
           :args {:request {},
                  :response {},
                  :interceptors sample-matcher}}})

(fact "Should extract an interceptor configuration for a given matcher type"
      (extract-existing-interceptors test-state-map :yahoo :response ".*")
      => sample-header-interceptor)

(def sample-simple-request
  {
  :headers {}
  :body "This is a body that will get replaced"})

(def sample-header-with-request
  {
   :headers {"Content-Type" "12345678"}
   :body "This is a body that will get replaced"})


(fact "Should return a request with interceptor applied"
      (apply-interceptor sample-simple-request sample-header-interceptor)
      => (merge sample-simple-request sample-header-interceptor)
      (apply-interceptor sample-header-with-request sample-header-interceptor)
      => {:headers {"Content-Type" "This one"
                    "Bareer" "123213"}
          :body "This is a body that will get replaced"})

(fact "Should apply all interceptors in order"
      (select-keys (apply-interceptors sample-simple-request [sample-header-interceptor  {:headers {"Content-Type" "12345678"}}]) [:headers])
      => {  :headers {"Content-Type" "12345678" "Bareer" "123213"}})

(def sample-multi-matcher
  {".*" {:response {:headers {"Content-Type" "Text"}}},
   ".*a=2" {:response {:headers {"Some-Header" "Some Value"}}}})

(fact "Should return all matchers that match the given url"
      (get-matchers-matching-url "http://www.yahoo.com" sample-multi-matcher)
      => (vector (flatten (seq (select-keys sample-multi-matcher [".*"])))))

(fact "Should flatten the :request or :response from the interceptor"
      (extract-interceptor-for-type sample-multi-matcher :response)
      => '({:headers {"Content-Type" "Text"}} {:headers {"Some-Header" "Some Value"}}))

(def sample-state
  {:yahoo {:route "/yahoo",
           :url "http://www.yahoo.com",
           :args {:request {},
                  :response {},
                  :interceptors {".*" {:response {:headers {"Bareer" "123456", "Content-Type" "text"}}}}}}})

(fact "Should remove a header interceptor when provided with a type a key and the proxy id"
      (remove-header-from-map sample-state :yahoo :response ".*" "Content-Type")
      => {:yahoo {:route "/yahoo",
                    :url "http://www.yahoo.com",
                    :args {:request {},
                           :response {},
                           :interceptors {".*" {:response {:headers {"Bareer" "123456"}}}}}}})




