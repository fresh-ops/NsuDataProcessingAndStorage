(ns main)

(defn heavy-odd? [x]
  (Thread/sleep 10)
  (odd? x))

(defn p-filter [pred coll]
    (->> coll
         (partition-all 12)
         (partition-all (.availableProcessors (Runtime/getRuntime)))
         (mapcat (fn [batch]
                   (->> batch 
                        (map #(future (doall (filter pred %))))
                        (doall)
                        (mapcat deref))))))  

(defn measure-filter []
  (time (doall 
          (take 100 
                (filter heavy-odd? (iterate inc 1))))))

(defn measure-p-filter []
  (time (doall 
          (take 100 
                (p-filter heavy-odd? (iterate inc 1))))))

