(ns disruptor.dsl-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [disruptor.core :as core]
            [disruptor.dsl :as dsl]
            [spy.core :as spy]))

(def disruptor (atom nil))

(defn reset-disruptor [f]
  (let [d (core/disruptor {:size 8})]
    (reset! disruptor d)
    (f)
    (core/shutdown! d)))

(use-fixtures :each reset-disruptor)

(deftest then-test
  (let [disruptor @disruptor
        events (atom [])
        handler-fn (fn [signature]
                     (fn [{:keys [event]}]
                       (swap! events conj [signature event])
                       (Thread/sleep 10)))
        handler-group (core/add-handlers! disruptor {:handlers [(handler-fn :A)]})]
    (dsl/then! handler-group {:handlers [(handler-fn :B)]})
    (core/start! disruptor)
    (core/publish! disruptor {:event 1})
    (core/publish! disruptor {:event 2})
    (core/publish! disruptor {:event 3})
    (core/shutdown! disruptor)
    (is (= #{[:A 1]
             [:B 1]
             [:A 2]
             [:B 2]
             [:A 3]
             [:B 3]}
           (into #{} @events)))))

(deftest sharding-handlers-test
  (let [disruptor @disruptor
        events (atom [])
        handlers (->> {:n 3}
                      (dsl/sharding-handlers (fn [{:keys [event]}]
                                               (swap! events conj event)
                                               (Thread/sleep 10)))
                      (map spy/spy))]
    (core/add-handlers! disruptor {:handlers handlers})
    (core/start! disruptor)
    (core/publish! disruptor {:event 1})
    (core/publish! disruptor {:event 2})
    (core/publish! disruptor {:event 3})
    (core/shutdown! disruptor)
    (is (= 3 (count @events)))
    (is (= #{1 2 3}
           (into #{} @events)))
    (is (every? #(spy/called-n-times? % 3) handlers))))
