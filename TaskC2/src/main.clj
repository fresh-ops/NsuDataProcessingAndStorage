(ns main)

(defn next-non-prime [start step table]
  (->> (iterate #(+ % step) start)
       (remove #(contains? table %))
       (first)))

(defn promote-non-prime [x table]
  (-> table 
      (assoc (next-non-prime x (table x) table) (table x)) 
      (dissoc x)))

(defn promote-prime [x table]
  (assoc table 
         (next-non-prime (+ x x) x table) 
         x))

(defn sieve [x table]
  (lazy-seq 
    (if (contains? table x)
      (sieve (inc x) (promote-non-prime x table))
      (cons x (sieve (inc x) (promote-prime x table))))))

(def primes
  (sieve 2 {}))

(def naturals (iterate inc 1))

(defn sieve-naive [[prime & numbers]]
  (lazy-seq (cons prime 
                  (sieve-naive (remove #(zero? (mod % prime)) numbers)))))

(def primes-naive (sieve-naive (rest naturals)))
