(ns donttellmybro-proxy.state)

(defonce custom-headers (atom {}))
(defonce main-host (atom "https://postman-echo.com"))

(defn add-header! [header-key header-value]
  (swap! custom-headers assoc header-key header-value))

(defn update-host! [new-host]
  (reset! main-host new-host))


