(ns ^:figwheel-always pyrexia.core
    (:require
        [goog.Timer :as timer]
        [goog.events :as events]
        [cognitect.transit :as t])
    (:import  [goog.net XhrIo])
)

(enable-console-print!)

(defonce app-state (atom {
    :nodes []
}))

(def r (t/reader :json))
(def w (t/writer :json-verbose))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(def search-query "{\"size\":0,\"aggs\":{\"group_by_state\":{\"terms\":{\"field\":\"id\"},\"aggs\":{\"top_tag_hits\":{\"top_hits\":{\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":1}}}}}}")

(defn retrieve
  [payload callback error-callback]
  (let [xhr (XhrIo.)]
     (events/listen xhr goog.net.EventType.COMPLETE
       (fn [e]
         (callback (t/read r (.getResponseText xhr)))))
     (. xhr
       (send "http://localhost:9200/temperature-2015.09.08/_search" "POST" payload))))

(defn fetch-events []
    (retrieve search-query #() #() ))

(defn poll
  []
  (let [timer (goog.Timer. 24000)]
    (do (fetch-events)
        (. timer (start))
        (events/listen timer goog.Timer/TICK fetch-events))))

(defn start-app
  []
  (do (poll))
)
(start-app)
