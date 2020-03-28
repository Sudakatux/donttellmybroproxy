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
  {".*" {:all {:response sample-header-interceptor}} })

(def less-generic-matcher
  {".*a=2" {:all {:response some-body-interceptor}}})

(def test-state-map
  {:yahoo {:route "/yahoo",
           :url "http://www.yahoo.com",
           :args {:request {},
                  :response {},
                  :interceptors sample-matcher}}})

(fact "Should extract an interceptor configuration for a given matcher type"
      (get-existing-interceptors-for-current-key-type-matcher-method test-state-map :yahoo :response ".*" :all)
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
      (get-matchers-matching-url sample-multi-matcher "http://www.yahoo.com")
      => (vector (flatten (seq (select-keys sample-multi-matcher [".*"])))))

(fact "Should flatten the :request or :response from the interceptor"
      (extract-interceptor-for-type (vals sample-multi-matcher)  :response)
      => '({:headers {"Content-Type" "Text"}} {:headers {"Some-Header" "Some Value"}}))

(def sample-state
  {:yahoo {:route "/yahoo",
           :url "http://www.yahoo.com",
           :args {:request {},
                  :response {},
                  :interceptors {".*" {:all {:response {:headers {"Bareer" "123456", "Content-Type" "text"}}}} }}}})

(fact "Should remove a header interceptor when provided with a type a key and the proxy id"
      (remove-header-from-map sample-state :yahoo :response ".*" :all "Content-Type")
      => {:yahoo {:route "/yahoo",
                    :url "http://www.yahoo.com",
                    :args {:request {},
                           :response {},
                           :interceptors {".*" {:all {:response {:headers {"Bareer" "123456"}}}}}}}})

(def some-bytes (byte-array [(byte 0x43)
                    (byte 0x6c)
                    (byte 0x6f)
                    (byte 0x6a)
                    (byte 0x75)
                    (byte 0x72)
                    (byte 0x65)
                    (byte 0x21)]))

(def sample-recorded-element-response
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
   :headers {"Content-Type" "text charset=utf-8",
             "Date" "Tue, 24 Dec 2019 12:01:45 GMT",
             "ETag" "W/\"40e-4wWvkw27hk7OZ3gzloXmBsT7QXs\"",
             "Server" "nginx",
             "Vary" "Accept-Encoding",
             "Connection" "Close"},
   :status 200,
   :length -1,
   :body (.getBytes "Some request body")
   }
  )

(def sample-record-element
  {"/postman" {:base-url "https://postman-echo.com",
               :recorded [{:url "https://postman-echo.com/post?",
                           :method :post,
                           :response sample-recorded-element-response
                           }]}}) ;Use vector instead of list

(def expected-result
  {
   ".*\\Q/post?\\E$" {:post {:response (assoc sample-recorded-element-response :body "Some request body") }}
   }
  )

(fact "Should take a recorded element and return an interceptor"
      (recorded-element->interceptor (get sample-record-element "/postman") 0)
      => expected-result)


;(fact "Should return interceptors matching methods")
(def sample-interceptors-all {".*" {:all {:response {:headers {"Bareer" "1232313"}}}}})

(fact "Should return interceptors matching :all and method if present"
      (interceptors-for-method {".*" {:all {:response {:headers {"Bareer" "1232313"}}}}} :get)
      => '({:response {:headers {"Bareer" "1232313"}}}))


(def sample-recorded-response
  {:headers {"accept-ch" "device-memory, dpr, width, viewport-width, rtt, downlink, ect",
             "Referrer-Policy" "no-referrer-when-downgrade",
             "Server" "ATS",
             "Age" "0",
             "Content-Type" "text/html; charset=UTF-8",
             "X-Content-Type-Options" "nosniff",
             "X-Frame-Options" "SAMEORIGIN",
             "Strict-Transport-Security" "max-age=31536000",
             "Connection" "keep-alive",
             "Transfer-Encoding" "chunked",
             "Expires" "-1",
             "P3P" "policyref=\"https://policies.yahoo.com/w3c/p3p.xml\", CP=\"CAO DSP COR CUR ADM DEV TAI PSA PSD IVAi IVDi CONi TELo OTPi OUR DELi SAMi OTRi UNRi PUBi IND PHY ONL UNI PUR FIN COM NAV INT DEM CNT STA POL HEA PRE LOC GOV\"",
             "Expect-CT" "max-age=31536000, report-uri=\"http://csp.yahoo.com/beacon/csp?src=yahoocom-expect-ct-report-only\"",
             "Date" "Tue, 18 Feb 2020 12:27:32 GMT",
             "X-XSS-Protection" "1; mode=block",
             "Cache-Control" "no-store, no-cache, max-age=0, private",
             "accept-ch-lifetime" "604800"},
   :status 200,
   :body some-bytes}
  )

(fact "Should be able to convert body if content type if valid headers"
      (format-body-if-possible sample-recorded-response)
      => "Clojure!")

(fact "Should return the byte array if content type is not stringable"
      (format-body-if-possible (assoc-in sample-recorded-response [:headers "Content-Type" ] "video/mpeg"))
      => some-bytes)


; Instruction to convert toInterceptor
;(swap! registered-proxies assoc-in [:postman :args :interceptors] (toInterceptor (get-in @recordings [:recordings "/postman"]) 0))
