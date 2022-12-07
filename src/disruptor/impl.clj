(ns disruptor.impl
  (:require [cognitect.anomalies :as anomalies])
  (:import [com.lmax.disruptor EventFactory
                               EventHandler
                               ExceptionHandler
                               EventTranslatorOneArg]))

(defn event-factory ^EventFactory [f]
  (reify EventFactory
    (newInstance [_]
      (f))))

(defn event-handler ^EventHandler [f]
  (reify EventHandler
    (onEvent [_ event sequence end-of-batch]
      (f {:event @event
          :sequence sequence
          :end-of-batch end-of-batch}))))

(defn event-translator ^EventTranslatorOneArg [f]
  (reify EventTranslatorOneArg
    (translateTo [_ event sequence value]
      (f {:event event
          :sequence sequence
          :value value}))))

(defn exception-handler ^ExceptionHandler [f]
  (reify ExceptionHandler
    (handleEventException [_ ex sequence event]
      (f {::anomalies/category ::anomalies/fault
          ::anomalies/message (.getMessage ex)
          :sequence sequence
          :event event
          :exception ex}))

    (handleOnStartException [_ ex]
      (f {::anomalies/category ::anomalies/incorrect
          ::anomalies/message (.getMessage ex)
          :exception ex}))

    (handleOnShutdownException [_ ex]
      (f {::anomalies/category ::anomalies/incorrect
          ::anomalies/message (.getMessage ex)
          :exception ex}))))

(def atomic-event-factory (event-factory #(atom nil)))
(def atomic-event-translator (event-translator (fn [{:keys [event value]}]
                                                 (reset! event value))))
