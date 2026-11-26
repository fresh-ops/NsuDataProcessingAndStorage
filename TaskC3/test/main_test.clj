(ns main-test
  (:require [clojure.test :refer :all]
            [main :refer :all]))

(deftest filter-comparison-test
  (testing "Comparison with common filter"
    (is (=
          (filter odd? (take 100 (iterate inc 1))) 
          (p-filter odd? (take 100 (iterate inc 1)))))))

(deftest lazy-seq-test
  (testing "Filter lazy sequence"
    (is (=
          (take 100 (filter odd? (iterate inc 1))) 
          (take 100 (p-filter odd? (iterate inc 1)))))))
