(ns disruptor.core-test
  (:require [clojure.test :refer [are deftest is testing use-fixtures]]
            [cognitect.anomalies :as anomalies]
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

(deftest error-handler-test
  (testing "handleEventException"
    (let [message "Oops."
          event 1
          err-map {:foo :bar}
          error (atom nil)
          disruptor (core/disruptor {:size 4 :default-exception-handler #(reset! error %)})]
      (core/add-handlers! disruptor {:handlers [(fn [_] (throw (ex-info message err-map)))]})
      (core/start! disruptor)
      (core/publish! disruptor {:event event})
      (core/shutdown! disruptor)
      (are [f x] (= x (f @error))
        ::anomalies/category      ::anomalies/fault
        ::anomalies/message       message
        :sequence                 0
        #(deref (:event %))       event
        #(ex-data (:exception %)) err-map))))
