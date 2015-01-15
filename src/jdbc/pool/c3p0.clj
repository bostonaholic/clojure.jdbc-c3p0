;; Copyright 2014-2015 Andrey Antukh <niwi@niwi.be>
;;
;; Licensed under the Apache License, Version 2.0 (the "License")
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns jdbc.pool.c3p0
  (:require [jdbc.core :refer [uri->dbspec]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource
           java.net.URI))

(defrecord DataSource [datasource]
  java.io.Closeable
  (close [_]
    (.close datasource)))

(defn- normalize-dbspec
  "Normalizes a dbspec for connection pool implementations."
  [{:keys [name vendor host port] :as dbspec}]
  (cond
   (or (string? dbspec) (instance? URI dbspec))
   (uri->dbspec dbspec)

   (and name vendor)
   (let [host   (or host "127.0.0.1")
         port   (if port (str ":" port) "")
         dbspec (dissoc dbspec :name :vendor :host :port)]
     (assoc dbspec
       :subprotocol vendor
       :subname (str "//" host port "/" name)))

   (map? dbspec)
   dbspec))

(defn make-datasource-spec
  "Given a plain dbspec, convert it on datasource dbspec
  using c3p0 connection pool implementation."
  [dbspec]
  (let [dbspec (normalize-dbspec dbspec)
        ds     (ComboPooledDataSource.)]
    (if (:connection-uri dbspec)
      (.setJdbcUrl ds (:connection-uri dbspec))
      (.setJdbcUrl ds (str "jdbc:"
                       (:subprotocol dbspec) ":"
                       (:subname dbspec))))
    (->DataSource (doto ds
                    (.setDriverClass (:classname dbspec nil))
                    (.setUser (:user dbspec nil))
                    (.setPassword (:password dbspec nil))

                    ;; Pool Size Management
                    (.setMinPoolSize (:min-pool-size dbspec 3))
                    (.setMaxPoolSize (:max-pool-size dbspec 15))
                    (.setInitialPoolSize (:initial-pool-size dbspec 0))
                    (.setCheckoutTimeout (:max-wait dbspec 0))

                    ;; Connection eviction
                    (.setMaxConnectionAge (quot (:max-connection-lifetime dbspec 3600000) 1000))
                    (.setMaxIdleTime (quot (:max-connection-idle-lifetime dbspec 1800000) 1000))
                    (.setMaxIdleTimeExcessConnections 120)

                    ;; Connection testing
                    (.setPreferredTestQuery (:test-connection-query dbspec nil))
                    (.setTestConnectionOnCheckin (:test-connection-on-borrow dbspec false))
                    (.setTestConnectionOnCheckout (:test-connection-on-return dbspec false))
                    (.setIdleConnectionTestPeriod (quot
                                                   (:test-idle-connections-period dbspec 800000)
                                                   1000))))))
