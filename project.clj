(defproject com.github.hden/disruptor "0.1.0"
  :description "FIXME: write description"
  :url "https://github.com/hden/disruptor"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [com.lmax/disruptor "4.0.0"]
                 [com.cognitect/anomalies "0.1.12"]]
  :repl-options {:init-ns disruptor.core}
  :profiles
  {:dev [:project/dev :profiles/dev]
   :profiles/dev {}
   :project/dev
   {:dependencies
    [[tortue/spy "2.15.0"]]}})
