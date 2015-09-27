(ns ^:figwheel-always pyrexia.core
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [goog.Timer :as timer]
   [goog.events :as events]
   [goog.dom.classes :as classes]
   [cognitect.transit :as t]
   [cljs-time.core :as time]
   [cljs-time.format :as tf]
   [reagent.core :as r])
  (:require-macros [pyrexia.env :as env :refer [cljs-env]])
  (:import [goog.net XhrIo]))

(enable-console-print!)

(defonce app-state
  (r/atom
   {:nodes []
    :timer nil
    :map (js/Image.)
    :minValue 100 ; random high value
    :maxValue 0
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
    (if (= weightTotal 0)
      0
      (/ weightedSum weightTotal))))

(defn alpha-blend [values]
  (let [distances (map :distance values)
        minDistance (apply min distances)
        fudgeFactor 200.0]
    (min 0.5 (/ 1.0 (/ minDistance fudgeFactor)))))

(defn temp-for-locations []
  (let [locations (:locations @app-state)
        nodes (:nodes @app-state)]
    (for [node (keys locations)
          :when (contains (keys nodes) node)
          :let [location (get locations node)
                data (get nodes node)]]
      {:location location :temp (:temp data)})))

(defn rangeValues [node x y]
  {:value (:temp node)
   :distance (Math/sqrt (+
                         (Math/pow (- (-> node :location first) x) 2.0)
                         (Math/pow (- (-> node :location second) y) 2.0)))})

(defn eitherRange [start end]
  (cond
    (< start end) (apply concat (range start end) (repeat [end]))
    (= start end) (repeat end)
    :else (apply concat (range start end -1) (repeat [start]))))

(defn colour-seq [[r1 g1 b1] [r2 g2 b2]]
  (take 0xff (partition 3 (interleave (eitherRange r1 r2) (eitherRange g1 g2) (eitherRange b1 b2)))))

(def red [0xff 0 0])
(def yellow [0xff 0xff 00])
(def green [00 0xff 00])
(def cyan [00 0xff 0xff])
(def blue [00 00 0xff])

(def colours [red yellow green cyan blue])

(def rainbow
  (apply concat
         (for [i (range (- (count colours) 1))
               :let [first (get colours i)
                     second (get colours (+ 1 i))]]
           (colour-seq first second))))

(defn- padstring [string length]
  (let [actualLength (count string)]
    (if (< actualLength length)
      (str (apply str (repeat (- length actualLength) "0")) string)
      string)))

(defn rgb [args]
  (apply str "#" (map #(padstring (.toString % 16) 2) args)))

(defn valueColour [value]
  (cond
    (= value 0) "#ffffff"
    (= (:maxValue @app-state) (:minValue @app-state)) (rgb (first rainbow))
    :else (let [valueRange (- (:maxValue @app-state) (:minValue @app-state))
                position (/ (- value (:minValue @app-state)) valueRange)
                rainbowSize (count rainbow)
                index (int (* position rainbowSize))]
            (rgb (nth rainbow index)))))

(defn draw-map []
  (let [gridSize 20
        imageWidth 600
        imageHeight 618
        boxWidth (/ imageWidth gridSize)
        boxHeight (/ imageHeight gridSize)
        values (-> (:nodes @app-state) vals)
        temp (seq (temp-for-locations))
        grid (apply merge
                    (for
                     [x (range 0 gridSize)
                      y (range 0 gridSize)
                      :let [rangeVals (map #(rangeValues % (* (+ .5 x) boxWidth) (* (+ .5 y) boxHeight)) temp)]]
                      {[x y] {:average (weighted-average rangeVals)
                              :blend (alpha-blend rangeVals)}}))
        minTemp (apply min (vals grid))
        maxTemp (apply max (vals grid))]
    [:svg {:width imageWidth
           :height imageHeight}
     [:image {:x 0 :y 0 :width imageWidth :height imageHeight "xlinkHref" "map.png"}]]

    ; (doall
    ;  (for [key (keys grid)
    ;        :let [value (get grid key)
    ;              x (first key)
    ;              y (second key)]]
    ;    (do
    ;      (set! (.-fillStyle context) (valueColour (:average value)))
    ;      ;(.log js/console "colour" (:average value) (valueColour (:average value)))
    ;      (set! (.-globalAlpha context) (:blend value))
    ;      (.fillRect context (* x boxWidth) (* y boxHeight) boxWidth boxHeight))))
))

(defn parse-nodes [node-data node-key]
  (let [buckets (-> node-data :aggregations :group_by_state :buckets)
        nodes (apply merge (map #(hash-map (:key %) (-> % :top_tag_hits :hits :hits first :_source)) buckets))]
    (.log js/console "nodes" (pr-str nodes))
    (if (-> nodes nil? not)
      (do
        (swap! app-state assoc :minValue (apply min (:minValue @app-state) (map :temp (vals nodes))))
        (.log js/console "minValue" (:minValue @app-state))
        (swap! app-state assoc :maxValue (apply max (:maxValue @app-state) (map :temp (vals nodes))))
        (.log js/console "maxValue" (:maxValue @app-state))))
    (swap! app-state assoc node-key nodes)
    (swap! app-state assoc :nodes (merge (:old-nodes @app-state) (:new-nodes @app-state)))))

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
  [:ul (map sensor-view (:nodes @app-state))])

(def render-sensors
  (r/render [sensors-view]
            (.getElementById js/document "sensors")))

(def render-map
  (r/render [draw-map]
            (.getElementById js/document "map")))
