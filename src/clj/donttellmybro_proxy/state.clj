(ns donttellmybro-proxy.state)


(defonce params (atom {:headers {}
                       :host "https://postman-echo.com"}))

;This is not being user remove


(defn merge-headers! [other]
  (swap! params assoc :headers (merge (:headers @params) other)))

(defn remove-header! [target]
  (swap! params assoc :headers (dissoc (:headers @params) target)))

(defn update-host! [new-host]
  (swap! params assoc :host new-host))


