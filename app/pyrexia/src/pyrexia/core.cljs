(ns ^:figwheel-always pyrexia.core
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [goog.Timer :as timer]
   [goog.events :as events]
   [goog.dom.classes :as classes]
   [cognitect.transit :as t]
   [cljs-time.core :as time]
   [cljs-time.format :as tf]
   [reagent.core :as r]
   [pyrexia.common :as c]
   [pyrexia.colour-line :as cl]
   [pyrexia.rainbow :as rb]
   [pyrexia.map :as map])
  (:require-macros [pyrexia.env :as env :refer [cljs-env]])
  (:import [goog.net XhrIo]))

(enable-console-print!)

(def r (t/reader :json))
(def w (t/writer :json-verbose))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! c/app-state update-in [:__figwheel_counter] inc))

(def search-query "{\"size\":0,\"aggs\":{\"group_by_state\":{\"terms\":{\"field\":\"id\"},\"aggs\":{\"top_tag_hits\":{\"top_hits\":{\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}],\"size\":1}}}}}}")

(def logstashFormatter (tf/formatter "yyyy.MM.dd"))
(defn logName [date]
  (str "temperature-" (tf/unparse logstashFormatter date)))

(defn retrieve
  [log payload callback error-callback]
  (let [xhr (XhrIo.)]
    (events/listen xhr goog.net.EventType.COMPLETE
                   (fn [e]
                     (callback (t/read r (.getResponseText xhr)))))
    (. xhr
       (send (str "http://" (cljs-env :es-host "localhost") ":9200/" log "/_search") "POST" payload))))

(defn parse-nodes [node-data node-key]
  (let [buckets (-> node-data :aggregations :group_by_state :buckets)
        nodes (apply merge (map #(hash-map (:key %) (-> % :top_tag_hits :hits :hits first :_source)) buckets))]
    (.log js/console "nodes" (pr-str nodes))
    (if (-> nodes nil? not)
      (do
        (swap! c/app-state assoc :minValue (apply min (:minValue @c/app-state) (map :temp (vals nodes))))
        (.log js/console "minValue" (:minValue @c/app-state))
        (swap! c/app-state assoc :maxValue (apply max (:maxValue @c/app-state) (map :temp (vals nodes))))
        (.log js/console "maxValue" (:maxValue @c/app-state))))
    (swap! c/app-state assoc node-key nodes)
    (swap! c/app-state assoc :nodes (merge (:old-nodes @c/app-state) (:new-nodes @c/app-state)))
    (map/draw-map map/canvas-dom (:map @c/app-state))))

(defn fetch-events []
  (retrieve (logName (time/now)) search-query #(parse-nodes (-> % keywordize-keys) :new-nodes) #())
  (retrieve (-> (time/now) (time/minus (time/days 1)) logName) search-query #(parse-nodes (-> % keywordize-keys) :old-nodes) #()))

(defn poll
  []
  (let [timer (goog.Timer. 5000)]
    (do
      (fetch-events)
      (. timer (start))
      (swap! c/app-state assoc :timer timer)
      ;(events/listen timer goog.Timer/TICK fetch-events)
)))

(if (-> (:timer @c/app-state) nil? not)
  (.stop (:timer @c/app-state)))
(poll)

(defn sensor-view [sensor]
  ^{:key (first sensor)}
  [:li #js {:onMouseOver
            (fn [e]
              (classes/add (.-target e) "foo")
              (.stopPropagation e))
            :onMouseOut
            (fn [e]
              (classes/remove (.-target e) "foo")
              (.stopPropagation e))}
   (first sensor)

   ^{:key (str (first sensor) "-ul")}
   [:ul
    (map
     #(with-meta [:li (str (-> % first name) " : " (-> % second))] {:key (-> % first name)})
     (select-keys (second sensor) [:temp :humid (keyword "@timestamp")]))]])

(defn sensors-view []
  [:ul (map sensor-view (into (sorted-map-by <) (:nodes @c/app-state)))])

(def render-sensors
  (r/render [sensors-view]
            (. js/document (getElementById "sensors"))))
