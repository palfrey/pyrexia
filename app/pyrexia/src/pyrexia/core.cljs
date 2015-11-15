(ns ^:figwheel-always pyrexia.core
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [goog.Timer :as timer]
   [goog.events :as events]
   [cljs-time.core :as time]
   [cljs-time.format :as tf]
   [reagent.core :as r]
   [pyrexia.common :as c]
   [pyrexia.colour-line :as cl]
   [pyrexia.rainbow :as rb]
   [pyrexia.map :as map]
   [pyrexia.sensors :as sensors]))

(enable-console-print!)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! c/app-state update-in [:__figwheel_counter] inc))

(def search-query "{
  \"size\": 0,
  \"aggs\": {
    \"not_null_temp\": {
      \"filter\": {
        \"exists\": {
          \"field\": \"temp\"
        }
      },
      \"aggs\": {
        \"group_by_state\": {
          \"terms\": {
            \"field\": \"id\"
          },
          \"aggs\": {
            \"top_tag_hits\": {
              \"top_hits\": {
                \"sort\": [
                  {
                    \"@timestamp\": {
                      \"order\": \"desc\"
                    }
                  }
                ],
                \"size\": 1
              }
            }
          }
        }
      }
    }
  }
}")

(def logstashFormatter (tf/formatter "yyyy.MM.dd"))
(defn logName [date]
  (str "temperature-" (tf/unparse logstashFormatter date)))

(defn retrieve-search [log payload callback error-callback]
  (c/retrieve
   (str log "/_search") "POST" payload {}
   callback error-callback))

(defn create-index [log callback error-callback]
  (c/retrieve
   log "PUT" nil {}
   callback error-callback))

(defn min-skip-null [& values]
  (apply min (filter #(-> % nil? not) values)))

(defn parse-nodes [node-data node-key]
  (let [buckets (-> node-data :aggregations :not_null_temp :group_by_state :buckets)
        nodes (apply merge (map #(hash-map (:key %) (-> % :top_tag_hits :hits :hits first :_source)) buckets))]
    (.log js/console "nodes" (pr-str nodes))
    (swap! c/app-state assoc node-key nodes)
    (swap! c/app-state assoc :nodes (merge (:old-nodes @c/app-state) (:new-nodes @c/app-state)))
    (swap! c/app-state assoc :minValue (apply min-skip-null (map :temp (vals (:nodes @c/app-state)))))
    (.log js/console "minValue" (:minValue @c/app-state))
    (swap! c/app-state assoc :maxValue (apply max (map :temp (vals (:nodes @c/app-state)))))
    (.log js/console "maxValue" (:maxValue @c/app-state))
    (map/draw-map map/canvas-dom (:map @c/app-state))))

(defn fetch-events []
  (retrieve-search (logName (time/now)) search-query #(parse-nodes (-> % keywordize-keys) :new-nodes) #())
  (retrieve-search (-> (time/now) (time/minus (time/days 1)) logName) search-query #(parse-nodes (-> % keywordize-keys) :old-nodes) #()))

(defn fetch-nodes []
  (retrieve-search
   "nodes" ""
   (fn [data]
     (.log js/console "result" data)
     (let [nodes (-> data keywordize-keys :hits :hits)
           nodes (map (keyword "_source") nodes)]
       (.log js/console "parsed" nodes)
       (doseq [node nodes]
         (swap! c/app-state assoc-in [:locations (:node node)] [(:x node) (:y node)]))
       (map/draw-map map/canvas-dom (:map @c/app-state))))
   (fn [data]
     (if (= (get data "status") 404)
       (do
         (.log js/console "need to make nodes")
         (create-index "nodes" #() #()))
       (.log js/console "error" data)))))

(defn poll
  [key every func]
  (if (-> (key @c/app-state) nil? not)
    (.stop (key @c/app-state)))
  (let [timer (goog.Timer. every)]
    (do
      (func)
      (. timer (start))
      (swap! c/app-state assoc key timer)
      (events/listen timer goog.Timer/TICK func))))

(poll :temperature-timer 5000 fetch-events)
(poll :nodes-timer 5000 fetch-nodes)
