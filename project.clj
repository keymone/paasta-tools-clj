(defproject paasta-tools-clj "0.1.0-SNAPSHOT"
  :description "Paasta tools"
  :url "http://github.com/Yelp/paasta"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-commons/clj-yaml "0.7.0"]
                 [cheshire "5.9.0"]
                 [clj-http "3.10.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [martian "0.1.10"]]
  :main ^:skip-aot paasta-tools-clj.metastatus
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
