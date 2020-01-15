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

(def some-bytes (byte-array [(byte 0x43)
                    (byte 0x6c)
                    (byte 0x6f)
                    (byte 0x6a)
                    (byte 0x75)
                    (byte 0x72)
                    (byte 0x65)
                    (byte 0x21)]))

(def sample-recorded-response
  {:cached nil,
   :request-time 1243,
   :repeatable? false,
   :protocol-version {:name "HTTP", :major 1, :minor 1},
   :chunked? false,
   :cookies {"sails.sid" {:discard true,
                          :path "/",
                          :secure false,
                          :value "s%3AhEcGVYfm4C_yhtl-gKhzjy2xTZeeHfAD.SYprp3zYF6aiUX2cmJryFu1mEbDYDz5zWobDkXumAhE",
                          :version 0}},
   :reason-phrase "OK",
   :headers {"Content-Type" "application/json; charset=utf-8",
             "Date" "Tue, 24 Dec 2019 12:01:45 GMT",
             "ETag" "W/\"40e-4wWvkw27hk7OZ3gzloXmBsT7QXs\"",
             "Server" "nginx",
             "Vary" "Accept-Encoding",
             "Connection" "Close"},
   :status 200,
   :length -1,
   ;:body some-bytes
   }
  )

(def sample-record-element
  {"/postman" {:base-url "https://postman-echo.com",
               :recorded [{:url "https://postman-echo.com/post?",
                           :method :post,
                           :body (.getBytes "Some request body"),
                           :response sample-recorded-response}]}}) ;Use vector instead of list

(def expected-result
  {
   "./post?" {
              :response sample-recorded-response
              }
   }
  )

(fact "Should take a recorded element and return an interceptor"
      (recorded-element->interceptor (get sample-record-element "/postman") 0)
      => expected-result)

; Instruction to convert toInterceptor
;(swap! registered-proxies assoc-in [:postman :args :interceptors] (toInterceptor (get-in @recordings [:recordings "/postman"]) 0))
