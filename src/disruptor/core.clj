(ns disruptor.core
  (:require [cognitect.anomalies :as anomalies]
            [taoensso.timbre :as timbre])
  (:import [java.util.concurrent TimeUnit]
           [com.lmax.disruptor EventFactory
                               EventHandler
                               ExceptionHandler
                               EventTranslatorOneArg]
           [com.lmax.disruptor.dsl Disruptor]
           [com.lmax.disruptor.util DaemonThreadFactory]))

(defn- ^EventFactory create-event-factory [f]
  (reify EventFactory
    (newInstance [_]
      (f))))

(defn- ^EventHandler create-event-handler [f]
  (reify EventHandler
    (onEvent [_ x y z]
      (f x y z))))

(defn- ^EventTranslatorOneArg create-event-translator [f]
  (reify EventTranslatorOneArg
    (translateTo [_ x y z]
      (f x y z))))

(defn- ^ExceptionHandler create-exception-handler [f]
  (reify ExceptionHandler
    (handleEventException [_ ex sequence event]
      (f {::anomalies/category ::anomalies/fault
          ::anomalies/message (.getMessage ex)
          :exception ex}))
    (handleOnStartException [_ ex]
      (f {::anomalies/category ::anomalies/incorrect
          ::anomalies/message (.getMessage ex)
          :exception ex}))
    (handleOnShutdownException [_ ex]
      (f {::anomalies/category ::anomalies/incorrect
          ::anomalies/message (.getMessage ex)
          :exception ex}))))

(defn create-disruptor [{:keys [size on-error]}]
  (let [event-factory (create-event-factory #(volatile! nil))
        disruptor (Disruptor. event-factory (or size 1024) DaemonThreadFactory/INSTANCE)]
    (doto disruptor
      (.setDefaultExceptionHandler (create-exception-handler (or on-error println))))))

(defn publish! [disruptor x]
  (let [event-translator (create-event-translator #(vreset! %1 %3))]
    (.publishEvent disruptor event-translator x)))

(defn start! [disruptor]
  (.start disruptor))

(defn shutdown!
  ([disruptor] (shutdown! disruptor 1))
  ([disruptor timeout] (shutdown! disruptor timeout TimeUnit/SECONDS))
  ([disruptor timeout unit] (.shutdown disruptor timeout unit)))

;; DSLs
(defn add-handlers! [disruptor coll]
  (.handleEventsWith disruptor (into-array EventHandler coll)))

(defn then [x coll]
  (.then x (into-array EventHandler coll)))

(defn create-sharding-handlers [f n]
  (map (fn [x]
         (create-event-handler (fn [entry sequence _]
                                 (when (= x (mod sequence n))
                                   (f @entry x)))))
       (range n)))

(defn random-io [content shard]
  (Thread/sleep (rand 1000))
  (timbre/info (format "upload shard=%d content=%d" shard content)))

(defn -main []
  (let [disruptor (create-disruptor {:size 16})
        n (.. Runtime getRuntime availableProcessors)
        readers (add-handlers! disruptor (create-sharding-handlers random-io n))]
    (then readers [(create-event-handler (fn [entry _ _]
                                            (timbre/info (format "commit content %d" @entry))))])
    (start! disruptor)
    (timbre/info "disruptor started" disruptor)
    (doseq [x (range 100)]
      (publish! disruptor x))
    (timbre/info "wrote 100 entries")
    (shutdown! disruptor 60)))
