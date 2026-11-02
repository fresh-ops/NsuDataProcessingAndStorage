(ns ru.nsu.g.solovev5.m.task-c1)
(require '[clojure.pprint :refer [pprint]])

(defn str-product
  [as bs]
  (reduce concat
          (map (fn [a]
                 (map (fn [b] (str a b)) bs))
               as)))

(defn eq-first-two?
  [lst] 
  (= (first lst) 
     (first (next lst))))

(defn words
  [n alpha]
  (reduce (fn [acc lst] 
            (remove eq-first-two? (str-product lst acc)))
          (repeat n alpha)))

(pprint (words 4 '("a" "b" "c")))
