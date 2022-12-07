 (ns disruptor.core
  (:require [disruptor.impl :as impl])
  (:import [java.util.concurrent TimeUnit]
           [com.lmax.disruptor EventHandler]
           [com.lmax.disruptor.dsl Disruptor]
           [com.lmax.disruptor.util DaemonThreadFactory]))

(defn disruptor
  "Create a new Disruptor."
  ^Disruptor
  [{:keys [size event-factory default-exception-handler]}]
  (let [disruptor (Disruptor. (or event-factory impl/atomic-event-factory)
                              (or size 1024)
                              DaemonThreadFactory/INSTANCE)]
    (when (fn? default-exception-handler)
      (.setDefaultExceptionHandler disruptor (impl/exception-handler default-exception-handler)))
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

(defn add-handlers!
  "Set up event handlers to handle events from the disruptor."
  [disruptor {:keys [handlers]}]
  (.handleEventsWith disruptor (into-array EventHandler (map impl/event-handler handlers))))
