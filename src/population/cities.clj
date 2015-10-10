(ns population.cities
  (:require [taoensso.timbre :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [population.db :as db]
            [clojure.core.match :refer [match]]))

(defn- find-city-by
  ([city]
    (db/select-query "select * from cities where city = ?"
                     city))
  ([country city]
    (db/select-query "select * from cities where country = ? AND city = ?"
                    country
                    city))
  ([country region city]
   (db/select-query "select * from cities where country = ? AND region = ? AND city = ?"
                     country
                     region
                     city)))

(defn find-city [search-str]
  (match (string/split search-str #",")
         [city] (find-city-by city)
         [country city] (find-city-by country city)
         [country region city] (find-city-by country region city)
         :else :not-recognized-search))
