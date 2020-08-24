(ns jdbc.pool.c3p0.middleware
  (:require
   [clojure.java.jdbc :as jdbc]))


(defn wrap-database
  "Creates a new transaction which will be added to the request.
  The transaction will be located under [:database]."
  [handler {:keys [datasource] :as _database}]
  (fn [req]
    (jdbc/with-db-transaction [tx datasource]
      (-> req
          (assoc :database tx)
          (handler)
          (dissoc :database)))))
