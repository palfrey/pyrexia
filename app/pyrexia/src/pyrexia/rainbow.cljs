(ns pyrexia.rainbow)

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
