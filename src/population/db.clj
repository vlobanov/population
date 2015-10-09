(ns population.db
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as j]
            [taoensso.timbre :as log]
            [environ.core :refer [env]]))

(defn- pooled-db []
  (let [url (env :db-url)
        user (env :db-user)
        password (env :db-password)
        cpds (doto (ComboPooledDataSource.)
                   (.setJdbcUrl url)
                   (.setUser user)
                   (.setPassword password)
                   (.setMinPoolSize 10)
                   (.setIdleConnectionTestPeriod 30)
                   ;; expire excess connections after 30 minutes of inactivity:
                   (.setMaxIdleTimeExcessConnections (* 30 60))
                   ;; expire connections after 3 hours of inactivity:
                   (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(defonce db-connection (delay (pooled-db)))

(defn select-query [q & params]
  (j/query @db-connection
           (list* q params)))
