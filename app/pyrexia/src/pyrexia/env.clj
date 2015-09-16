(ns pyrexia.env
  (:require [environ.core :refer [env]]))

(defmacro cljs-env [kw default]
  (if-let [ret (env kw)]
    ret
    default))
