(ns population.core
  (:gen-class)
  (:require [population.density :as density]
            [population.cities :as cities]
            [taoensso.timbre :as log]
            [clojure.core.match :refer [match]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [immutant.web :refer [run run-dmc]]
            [clj-json.core :refer [generate-string]]
            [ring.middleware.params :refer :all]))

(defn too-many-cities-found [cities]
  (map (fn [{:keys [city country region] :as city-data}]
         (assoc city-data :full_query (str country "," region "," city)))
       cities))

(defn get-population-with-radius [city radius]
  (let [cities (cities/find-city city)]
    (if (= (count cities) 1)
      (let [[{:keys [latitude longitude] :as city}] cities
            population (density/count-population (double latitude)
                                                 (double longitude)
                                                 radius)]
        [:ok {:population population
              :radius radius
              :city city}])
      [:error {:description (str "found " (count cities) " results, expected 1")
               :results (too-many-cities-found cities)}])))

(defn get-population [city-search radius-str]
  (if-let [radius (and (string? radius-str)
                       (re-matches #"^\d+$" radius-str)
                       (Long. radius-str))]
    (if (< radius density/max-radius)
      (if (not-empty city-search)
          (get-population-with-radius city-search radius)
          [:error "city is incorrect"])
      [:error ("radius must be less than " density/max-radius " km")])
    [:error "radius is incorrect"]))

(defn json [status data]
  {:status status
   :headers {"Content-Type" "application/json; charset=UTF-8"}
   :body (generate-string data)})

(defn handler [{{:strs [city radius]} :params}]
  (log/info [city radius])
  (match (get-population city radius)
         [:error error-explanation] (json 422 {:error error-explanation})
         [:ok result] (json 200 result)))

(defroutes app
  (GET "/city/population" request (handler request))
  (route/resources "/")
  (route/not-found {:status 404 :body "not found"}))

(defn -main
  [& args]
  (density/read-ascii-density-file! "data/glds15ag.asc")
  (-> app
      wrap-params
      (run {"host" "0.0.0.0"})))
