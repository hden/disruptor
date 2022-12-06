(ns disruptor.core
  (:require [disruptor.impl :as impl])
  (:import [java.util.concurrent TimeUnit]
           [com.lmax.disruptor EventHandler]
           [com.lmax.disruptor.dsl Disruptor]
           [com.lmax.disruptor.util DaemonThreadFactory]))

(defn disruptor
  "Create a new Disruptor."
  ^Disruptor
  [{:keys [size on-error event-factory]}]
  (let [disruptor (Disruptor. (or event-factory impl/atomic-event-factory)
                              (or size 1024)
                              DaemonThreadFactory/INSTANCE)]
    (when (fn? on-error)
      (.setDefaultExceptionHandler disruptor (impl/exception-handler on-error)))
    disruptor))

(defn publish!
  "Publish an event."
  [disruptor {:keys [event event-translator]}]
  (.publishEvent disruptor
                 (or event-translator impl/atomic-event-translator)
                 event))

(defn start!
  "Start the disruptor."
  [disruptor]
  (.start disruptor))

(defn shutdown!
  "Waits until all events currently in the disruptor have been processed by
  all event processors and then halts the processors."
  ([disruptor] (shutdown! disruptor {}))
  ([disruptor {:keys [timeout unit]}]
   (.shutdown disruptor
              (or timeout 1)
              (or unit TimeUnit/SECONDS))))

;; DSLs
(defn add-handlers!
  "Set up event handlers to handle events from the disruptor."
  [disruptor {:keys [handlers]}]
  (.handleEventsWith disruptor (into-array EventHandler (map impl/event-handler handlers))))

; (defn random-io [content shard]
;   (Thread/sleep (rand 1000))
;   (timbre/info (format "upload shard=%d content=%d" shard content)))

; (defn -main []
;   (let [disruptor (create-disruptor {:size 16})
;         n (.. Runtime getRuntime availableProcessors)
;         readers (add-handlers! disruptor (create-sharding-handlers random-io n))]
;     (then readers [(create-event-handler (fn [entry _ _]
;                                             (timbre/info (format "commit content %d" @entry))))])
;     (start! disruptor)
;     (timbre/info "disruptor started" disruptor)
;     (doseq [x (range 100)]
;       (publish! disruptor x))
;     (timbre/info "wrote 100 entries")
;     (shutdown! disruptor 60)))
