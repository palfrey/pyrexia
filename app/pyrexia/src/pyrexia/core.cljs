(ns ^:figwheel-always pyrexia.core
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [goog.Timer :as timer]
   [goog.events :as events]
   [goog.dom.classes :as classes]
   [cognitect.transit :as t]
   [om.core :as om]
   [om.dom :as dom]
   [cljs-time.core :as time]
   [cljs-time.format :as tf]
   [monet.canvas :as canvas])
  (:require-macros [pyrexia.env :as env :refer [cljs-env]])
  (:import [goog.net XhrIo])) (enable-console-print!)

(defonce app-state (atom {:nodes []
                          :timer nil
                          :map (js/Image.)
                          :locations {"temp-1a:fe:34:fa:b2:af" [500 500]
                                      "temp-1a:fe:34:fa:b2:bf" [100 100]}}))

(def r (t/reader :json))
(def w (t/writer :json-verbose))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! app-state update-in [:__figwheel_counter] inc))

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

(defn contains [coll key]
  (not (not-any? #(= % key) coll)))

(defn weighted-average [values]
  (let [distances (seq (map :distance values))
        weights (map #(/ 1 (+ % 1)) distances)
        weightTotal (apply + weights)
        weightedValues (map #(/ (:value %) (+ 1 (:distance %))) values)
        weightedSum (apply + weightedValues)]
    ;(.log js/console "wa" weightedSum weightTotal (/ weightedSum weightTotal))
    (/ weightedSum weightTotal)))

(defn alpha-blend [values]
  (let [distances (map :distance values)
        maxDistance (apply max distances)
        fudgeFactor 700.0]
    ;(.log js/console "ab" maxDistance)
    (min 1.0 (/ 1.0 (/ maxDistance fudgeFactor)))))

(defn temp-for-locations [boxWidth boxHeight]
  (let [locations (:locations @app-state)
        nodes (:nodes @app-state)]
    (.log js/console "tl-beg" (keys locations) (keys nodes) (keys @app-state))
    (for [node (keys locations)
          :when (contains (keys nodes) node)
          :let [location (get locations node)
                data (get nodes node)]]
      (do
        (.log js/console "tl" location data)
        {:location location :temp (:temp data)}))))

(defn rangeValues [node x y]
  {:value (:temp node) :distance (Math/sqrt (+
                                             (Math/pow (- (-> node :location first) x) 2.0)
                                             (Math/pow (- (-> node :location second) y) 2.0)))})

(defn draw-map [canvas mapImage]
  (let [context (.getContext canvas "2d")
        gridSize 40
        imageWidth (.-width mapImage)
        imageHeight (.-height mapImage)
        boxWidth (/ (.-width canvas) gridSize)
        boxHeight (/ (.-height canvas) gridSize)
        values (-> (:nodes @app-state) vals)
        temp (-> (temp-for-locations boxWidth boxHeight) seq)
        grid (apply merge
                    (for
                     [x (range 0 gridSize)
                      y (range 0 gridSize)
                      :let [rangeVals (map #(rangeValues % (* (+ .5 x) imageWidth) (* (+ .5 y) imageHeight)) temp)]]
                      {[x y] {:average (weighted-average rangeVals)
                              :blend (alpha-blend rangeVals)}}))
        minTemp (apply min (vals grid))
        maxTemp (apply max (vals grid))]
    (set! (.-width canvas) imageWidth)
    (set! (.-height canvas) imageHeight)
    (.log js/console "image" imageWidth imageHeight)
    (.drawImage context mapImage 0 0)
    (doall (for [key (keys grid)
                 :let [value (get grid key)
                       x (first key)
                       y (second key)]]
             (do
               ;(.log js/console "grid" (first key) (second key) (:blend value))
               (set! (.-fillStyle context) "#ff0000")
               (set! (.-globalAlpha context) (:blend value))
               (.fillRect context (* x boxWidth) (* y boxHeight) boxWidth boxHeight))))))

(def canvas-dom (.getElementById js/document "map"))

(defn parse-nodes [node-data node-key]
  (let [buckets (-> node-data :aggregations :group_by_state :buckets)
        nodes (apply merge (map #(hash-map (:key %) (-> % :top_tag_hits :hits :hits first :_source)) buckets))]
    (.log js/console "nodes" (pr-str nodes))
    (swap! app-state assoc node-key nodes)
    (swap! app-state assoc :nodes (merge (:old-nodes @app-state) (:new-nodes @app-state)))
    (draw-map canvas-dom (:map @app-state))))

(defn fetch-events []
  (retrieve (logName (time/now)) search-query #(parse-nodes (-> % keywordize-keys) :new-nodes) #())
  (retrieve (-> (time/now) (time/minus (time/days 1)) logName) search-query #(parse-nodes (-> % keywordize-keys) :old-nodes) #()))

(defn poll
  []
  (let [timer (goog.Timer. 5000)]
    (do
      (fetch-events)
      (. timer (start))
      (swap! app-state assoc :timer timer)
      ;(events/listen timer goog.Timer/TICK fetch-events)
)))

(if (-> (:timer @app-state) nil? not)
  (.stop (:timer @app-state)))
(poll)

(defn sensor-view [sensor owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:onMouseOver (fn [e]
                                  (classes/add (.-target e) "foo")
                                  (.stopPropagation e))
                   :onMouseOut (fn [e]
                                 (classes/remove (.-target e) "foo")
                                 (.stopPropagation e))}
              (first sensor)
              (apply dom/ul nil
                     (map
                      #(dom/li nil (-> % first name) " : " (-> % second))
                      (select-keys (second sensor) [:temp :humid (keyword "@timestamp")])))))))

(defn sensors-view [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul nil
             (om/build-all sensor-view (:nodes data))))))

(om/root sensors-view app-state
         {:target (. js/document (getElementById "sensors"))})

(defn map-image []
  (let [img (js/Image.)]
    (set! (.-src img) "map.png")
    (set! (.-onload img)
          (fn [e]
            (swap! app-state assoc :map (.-target e))
            (draw-map canvas-dom (:map @app-state))))
    img))

(map-image)
