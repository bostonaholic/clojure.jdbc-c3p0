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
  (:require [clojure.string :as str]
            [clojure.walk :as walk])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource
           java.net.URI))

(defn- querystring->map
  "Given a URI instance, return its querystring as
  plain map with parsed keys and values."
  [^URI uri]
  (let [^String query (.getQuery uri)]
    (if (nil? query)
      {}
      (->> (for [^String kvs (.split query "&")] (into [] (.split kvs "=")))
           (into {})
           (walk/keywordize-keys)))))

(defn- uri->dbspec
  "Parses a dbspec as uri into a plain dbspec. This function accepts
  a `java.net.URI` instane as parameter."
  [^URI uri]
  (let [host (.getHost uri)
        port (.getPort uri)
        path (.getPath uri)
        scheme (.getScheme uri)
        userinfo (.getUserInfo uri)]
    (merge
      {:subname (if (pos? port)
                 (str "//" host ":" port path)
                 (str "//" host path))
       :subprotocol scheme}
      (when userinfo
        (let [[user password] (str/split userinfo #":")]
          {:user user :password password}))
      (querystring->map uri))))

(defrecord DataSource [datasource]
  java.io.Closeable
  (close [_]
    (.close datasource)))

(defn- normalize-dbspec
  "Normalizes a dbspec for connection pool implementations. Accepts a string,
   URI instance or dbspec map as parameter"
  [{:keys [name vendor host port] :as dbspec}]
  (cond
   (string? dbspec) (uri->dbspec (URI. dbspec))
   (instance? URI dbspec) (uri->dbspec)

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
