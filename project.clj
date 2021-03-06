(defproject tunnit "0.0.1-SNAPSHOT"
  :description "Hour tracking data parser"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-time "0.14.2"]
                 [org.clojure/tools.cli "0.3.5"]]
  :main tunnit.core
  :aot [tunnit.core]
  :profiles {:dev {:dependencies [[midje "1.9.1"]]
                   :plugins [[lein-midje "3.2.1"] [lein-cloverage "1.0.10"]]}
             ;; You can add dependencies that apply to `lein midje` below.
             ;; An example would be changing the logging destination for test runs.
             :midje {}})
             ;; Note that Midje itself is in the `dev` profile to support
             ;; running autotest in the repl.

  
