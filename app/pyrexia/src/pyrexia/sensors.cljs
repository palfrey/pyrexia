(ns pyrexia.sensors
  (:require [goog.dom.classes :as classes]
            [pyrexia.common :as c]
            [reagent.core :as r]
            [goog.events :as events]
            [cognitect.transit :as t]
            [goog.events.EventType :as event-type]
            [pyrexia.map :as map]
            [clojure.walk :refer [stringify-keys]]))

(defn set-node-location [node x y]
  (c/retrieve
   (str "nodes/node/" node) "PUT"
   (t/write c/w (stringify-keys {:node node :x x :y y})) {}
   #() #()))

(defonce map-selector
  (events/listen
   map/canvas-dom event-type/CLICK
   (fn [e]
     (let [selected (:selected @c/app-state)
           x (.-offsetX e)
           y (.-offsetY e)]
       (if (-> selected nil? not)
         (do
           (swap! c/app-state assoc-in [:locations selected] [x y])
           (set-node-location selected x y)
           (.log js/console (:locations @c/app-state))
           (map/draw-map map/canvas-dom (:map @c/app-state))))))))

(defn sensor-view [sensor]
  (let [id (first sensor)]
    ^{:key id}
    [:li {:class "no-box"
          :id id
          :onMouseOver
          (fn [e]
            (classes/add (.getElementById js/document id) "box")
            (.stopPropagation e))
          :onMouseOut
          (fn [e]
            (classes/remove (.getElementById js/document id) "box")
            (.stopPropagation e))
          :onClick
          (fn [e]
            (doseq [el (array-seq (.getElementsByClassName js/document "selected"))]
              (classes/remove el "selected"))
            (classes/add (.getElementById js/document id) "selected")
            (swap! c/app-state assoc :selected id)
            (.stopPropagation e))}
     (first sensor)

     ^{:key (str (first sensor) "-ul")}
     [:ul
      (map
       #(with-meta [:li (str (-> % first name) " : " (-> % second))] {:key (-> % first name)})
       (select-keys (second sensor) [:temp :humid (keyword "@timestamp")]))]]))

(defn sensors-view []
  [:ul (map sensor-view (into (sorted-map-by <) (:nodes @c/app-state)))])

(def render-sensors
  (r/render [sensors-view]
            (. js/document (getElementById "sensors"))))
