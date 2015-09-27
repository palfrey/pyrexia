(ns pyrexia.common
  (:require [reagent.core :as r]))

(defonce app-state
  (r/atom
   {:nodes []
    :timer nil
    :map (js/Image.)
    :minValue 100 ; random high value
    :maxValue 0
    :selected nil
    :locations {}}))
