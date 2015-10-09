(defproject population "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.taoensso/timbre "4.1.1"]
                 [environ "1.0.1"]
                 [com.mchange/c3p0 "0.9.2.1"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [yesql "0.4.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]]
  :main ^:skip-aot population.core
  :target-path "target/%s"
  :jvm-opts ["-Xmx4g"]
  :profiles {:uberjar {:aot :all}})
