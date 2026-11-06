(ns main-test
  (:require [clojure.test :refer :all]
            [main :refer :all]))

(deftest gen-non-prime-not-in-table
  (let [table {8 2, 9 3, 10 5, 14 7}]
    (is (false? (contains? table (next-non-prime 8 2 table))))))

(deftest remove-previous-non-prime
  (let [table {8 2, 9 3, 10 5, 14 7}]
    (is (false? (contains? (promote-non-prime 8 table) 8)))))

(deftest put-multiple-of-prime
  (let [table {8 2, 9 3, 10 5, 14 7}]
    (is (some #(11) (vals (promote-prime 11 table))))))

(deftest gen-first-ten-primes
  (is (= [2 3 5 7 11 13 17 19 23 29]
         (take 10 primes))))

(deftest gen-many-without-errors
  (is (= 10000 (count (take 10000 primes)))))

(deftest eq-naive
  (is (= (take 1750 primes-naive)
         (take 1750 primes))))

