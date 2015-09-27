(ns pyrexia.colour-line
  (:require [reagent.core :as r]
            [pyrexia.common :as c]
            [pyrexia.rainbow :as rb]
            [goog.string :as gstring]))

(defn- mid [x y]
  (+ x (/ (- y x) 2)))

(defn- colours-view []
  (let [colour-count 20
        colour-width (.-width (:map @c/app-state))
        box-width (/ colour-width colour-count)
        bar-height 30
        rainbow-count (count rb/rainbow)
        rainbow-step (/ rainbow-count colour-count)]
    ^{:key :colour-map}
    [:svg {:width colour-width :height (* 2 bar-height)}
     (concat
      [^{:key :beg-colour} [:text {:x 0 :y (- bar-height 2) :font-size bar-height} (:minValue @c/app-state)]
       ^{:key :mid-colour} [:text {:x (- (/ colour-width 2) 40) :y (- bar-height 2) :font-size bar-height} (mid (:minValue @c/app-state) (:maxValue @c/app-state))]
       ^{:key :end-colour} [:text {:x (- colour-width 40) :y (- bar-height 2) :font-size bar-height} (:maxValue @c/app-state)]]
      (map
       #(with-meta
          [:rect {:x (+ (* % box-width) 1)
                  :y bar-height
                  :width box-width
                  :height bar-height
                  :style {:fill (rb/rgb (nth rb/rainbow (- (- rainbow-count 1) (* rainbow-step %))))}}]
          {:key (gstring/format "box-%d" %)})
       (range colour-count)))]))

(def render-colours
  (r/render [colours-view]
            (.getElementById js/document "colours")))
