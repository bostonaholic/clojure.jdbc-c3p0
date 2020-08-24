(ns jdbc.pool.c3p0.component
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as c]
   [jdbc.pool.c3p0 :as pool]))


(defrecord DBPool [dbspec]
  c/Lifecycle

  (start [this]
    (log/info "[DB Pool] Started" dbspec)
    (assoc this :datasource (pool/make-datasource-spec dbspec)))

  (stop [this]
    (log/info "[DB Pool] Stopped")
    (assoc this :datasource nil)))

(defn new-db-pool
  "Creates a new db pool component via `make-datasource-spec`
   with `dbspec` to use with `com.stuartsierra.component`."
  [dbspec]
  (map->DBPool {:dbspec dbspec}))
