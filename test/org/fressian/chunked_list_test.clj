(ns org.fressian.chunked-list-test
  (:use [clojure.test.generative :only (defspec) :as test])
  (:require [clojure.data.generators :as gen])
  (:import [org.fressian.impl ChunkedList]))

(set! *warn-on-reflection* true)

(defn small-positive-int
  "Generate a small positive integer for list size tests"
  []
  (gen/uniform 1 5000))

(defn medium-positive-int
  "Generate a medium positive integer for larger list tests"
  []
  (gen/uniform 100 10000))

(defspec chunked-list-preserves-order-and-size
  (fn [n]
    (let [cl (ChunkedList.)]
      (dotimes [i n]
        (.add cl (Long/valueOf i)))
      [(.size cl) (.toArray cl)]))
  [^{:tag `small-positive-int} n]
  (let [[size arr] %]
    (assert (= n size) (str "Size mismatch: expected " n " got " size))
    (assert (= n (alength ^objects arr)) "Array length mismatch")
    (dotimes [i n]
      (assert (= (long i) (aget ^objects arr i))
              (str "Value mismatch at index " i)))))

(defspec chunked-list-get-matches-array
  (fn [n]
    (let [cl (ChunkedList.)]
      (dotimes [i n]
        (.add cl (Long/valueOf i)))
      (let [arr (.toArray cl)]
        ;; Check get at various positions
        (and (= (.get cl 0) (aget ^objects arr 0))
             (= (.get cl (dec n)) (aget ^objects arr (dec n)))
             (= (.get cl (quot n 2)) (aget ^objects arr (quot n 2)))))))
  [^{:tag `medium-positive-int} n]
  (assert (true? %) "get() results don't match toArray()"))

(defspec chunked-list-chunk-boundaries
  (fn [_]
    ;; Test at exact chunk boundary (1024) and crossing it (1025)
    (let [cl1024 (ChunkedList.)
          cl1025 (ChunkedList.)]
      (dotimes [i 1024] (.add cl1024 (Long/valueOf i)))
      (dotimes [i 1025] (.add cl1025 (Long/valueOf i)))
      (let [arr1024 (.toArray cl1024)
            arr1025 (.toArray cl1025)]
        {:size1024 (.size cl1024)
         :len1024 (alength ^objects arr1024)
         :last1024 (aget ^objects arr1024 1023)
         :size1025 (.size cl1025)
         :len1025 (alength ^objects arr1025)
         :elem1024 (aget ^objects arr1025 1024)})))
  [^{:tag `gen/byte} _]
  (let [{:keys [size1024 len1024 last1024 size1025 len1025 elem1024]} %]
    (assert (= 1024 size1024 len1024) "1024 element list size wrong")
    (assert (= 1023 last1024) "Last element of 1024 list wrong")
    (assert (= 1025 size1025 len1025) "1025 element list size wrong")
    (assert (= 1024 elem1024) "Element at index 1024 wrong")))

(defspec chunked-list-expansion
  (fn [_]
    ;; Test beyond initial 16 chunks (>16384 elements)
    (let [cl (ChunkedList.)
          n 20000]
      (dotimes [i n] (.add cl (Long/valueOf i)))
      [(.size cl) (aget ^objects (.toArray cl) 19999)]))
  [^{:tag `gen/byte} _]
  (let [[size last-elem] %]
    (assert (= 20000 size) "20K list size wrong")
    (assert (= 19999 last-elem) "Last element of 20K list wrong")))

(defspec chunked-list-empty
  (fn [_]
    (let [cl (ChunkedList.)]
      [(.size cl) (alength ^objects (.toArray cl))]))
  [^{:tag `gen/byte} _]
  (let [[size len] %]
    (assert (= 0 size len) "Empty list should have size 0")))
