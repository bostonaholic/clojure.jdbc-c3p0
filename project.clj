(defproject clojure.jdbc/clojure.jdbc-c3p0 "0.3.3-SNAPSHOT"
  :description "c3p0, a mature, highly concurrent JDBC connection pooling library for clojure.jdbc"
  :url "https://github.com/bostonaholic/clojure.jdbc-c3p0"
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.mchange/c3p0 "0.9.5.2"]]

  :profiles {:dev {:dependencies [[clojure.jdbc "0.4.0-beta1"]
                                  [com.h2database/h2 "1.3.176"]]}
             :test {:jvm-opts ["-Dcom.mchange.v2.log.MLog=com.mchange.v2.log.FallbackMLog"
                               "-Dcom.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL=OFF"]}})
