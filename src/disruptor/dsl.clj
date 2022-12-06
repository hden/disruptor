(ns disruptor.dsl
  (:require [disruptor.impl :as impl])
  (:import [com.lmax.disruptor EventHandler]
           [com.lmax.disruptor.dsl EventHandlerGroup]))

(defn then!
  "Set up event handlers to consume events from the disruptor.
  These handlers will only process events after every handlers in this group has processed the event."
  [^EventHandlerGroup g {:keys [handlers]}]
  (.then g (into-array EventHandler (map impl/event-handler handlers))))

(defn sharding-handlers
  "Create a list of handlers that process events in parallel."
  [f {:keys [n]}]
  (map (fn [x]
         (fn [{:as arg-map :keys [sequence]}]
           (when (= x (mod sequence n))
             (f arg-map))))
       (range (or n 1))))
