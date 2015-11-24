(ns pyrexia.map
  (:require [pyrexia.common :as c]
            [pyrexia.rainbow :as rb]))

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
        fudgeFactor 80.0]
    (if (= (count distances) 0)
      0.0 ; means that we don't wipe the map when there's no items
      (min 0.5 (/ 1.0 (/ minDistance fudgeFactor))))))

(defn contains [coll key]
  (not (not-any? #(= % key) coll)))

(defn temp-for-locations []
  (let [locations (:locations @c/app-state)
        nodes (:nodes @c/app-state)]
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

(defn valueColour [value]
  (cond
    (= value 0) "#ffffff"
    (= (:maxValue @c/app-state) (:minValue @c/app-state)) (rb/rgb (first rb/rainbow))
    :else (let [valueRange (- (:maxValue @c/app-state) (:minValue @c/app-state))
                position (/ (- value (:minValue @c/app-state)) valueRange)
                rainbowSize (count rb/rainbow)
                index (min (int (* (- 1.0 position) rainbowSize)) (- rainbowSize 1))]
            (rb/rgb (nth rb/rainbow index)))))

(defn draw-map [canvas mapImage]
  (let [context (.getContext canvas "2d")
        gridSize 20
        imageWidth (.-width mapImage)
        imageHeight (.-height mapImage)
        boxWidth (/ imageWidth gridSize)
        boxHeight (/ imageHeight gridSize)
        values (-> (:nodes @c/app-state) vals)
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
    (set! (.-width canvas) imageWidth)
    (set! (.-height canvas) imageHeight)
    (.log js/console "image" imageWidth imageHeight)
    (.drawImage context mapImage 0 0)

    (doall
     (for [key (keys grid)
           :let [value (get grid key)
                 x (first key)
                 y (second key)]]
       (do
         (set! (.-fillStyle context) (valueColour (:average value)))
           ;(.log js/console "colour" (:average value) (valueColour (:average value)))
         (set! (.-globalAlpha context) (:blend value))
         (.fillRect context (* x boxWidth) (* y boxHeight) boxWidth boxHeight))))

      ; (set! (.-fillStyle context) "#000000")
      ; (set! (.-globalAlpha context) 1.0)
      ; (doall
      ;  (for [[x y] (-> @c/app-state :locations vals)]
      ;    (.fillRect context x y boxWidth boxHeight)))
  	;
))

(def canvas-dom (.getElementById js/document "map"))

(defonce map-image
  (let [img (js/Image.)]
    (set! (.-src img) "map.png")
    (set! (.-onload img)
          (fn [e]
            (swap! c/app-state assoc :map (.-target e))
            (draw-map canvas-dom (:map @c/app-state))))
    img))
