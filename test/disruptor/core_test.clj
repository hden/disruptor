(ns disruptor.core-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [disruptor.core :as core]))

(def disruptor (atom nil))

(defn reset-disruptor [f]
  (let [d (core/disruptor {:size 8})]
    (reset! disruptor d)
    (f)
    (core/shutdown! d)))

(use-fixtures :each reset-disruptor)

(deftest lifecycle-test
  (testing "publish without event handler"
    (let [disruptor @disruptor]
      (core/start! disruptor)
      (core/publish! disruptor {:event ::event})
      (core/shutdown! disruptor))))

(deftest publish-test
  (testing "read your writes"
    (let [disruptor @disruptor
          events (atom [])]
      (core/add-handlers! disruptor {:handlers [(fn [{:keys [event]}] (swap! events conj event))]})
      (core/start! disruptor)
      (core/publish! disruptor {:event 1})
      (core/publish! disruptor {:event 2})
      (core/publish! disruptor {:event 3})
      (core/shutdown! disruptor)
      (is (= [1 2 3] @events)))))
