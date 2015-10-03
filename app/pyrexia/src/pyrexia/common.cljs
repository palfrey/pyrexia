(ns pyrexia.common
  (:require [reagent.core :as r]
            [cognitect.transit :as t]
            [goog.events :as events])
  (:import [goog.net XhrIo])
  (:require-macros [pyrexia.env :as env :refer [cljs-env]]))

(defonce app-state
  (r/atom
   {:nodes []
    :temperature-timer nil
    :map (js/Image.)
    :minValue 0
    :maxValue 0
    :selected nil
    :locations {}}))

(def r (t/reader :json))
(def w (t/writer :json-verbose))

(defn retrieve
  [url method content headers callback error-callback]
  (let [xhr (XhrIo.)]
    (events/listen xhr goog.net.EventType.COMPLETE
                   (fn [e]
                     (if (.isSuccess xhr)
                       (callback (t/read r (.getResponseText xhr)))
                       (error-callback (t/read r (.getResponseText xhr))))))
    (. xhr send (str "http://" (cljs-env :es-host "localhost") ":9200/" url) method content headers)))
