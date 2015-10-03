(ns pyrexia.common
  (:require [reagent.core :as r]))

(defonce app-state
  (r/atom
   {:nodes []
    :temperature-timer nil
    :map (js/Image.)
    :minValue 0
    :maxValue 0
    :selected nil
    :locations {}}))
