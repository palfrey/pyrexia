(ns pyrexia.sensors
  (:require [goog.dom.classes :as classes]
            [pyrexia.common :as c]
            [reagent.core :as r]))

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
