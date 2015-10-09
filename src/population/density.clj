(ns population.density
  (:gen-class)
  (:require [taoensso.timbre :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(defonce density-description (atom {}))
(defonce density-data (atom []))

(def earth-r-km 6371)

(defn- read-ascii-density-file-data
  "reads lines, splits by space and converts to vector of vectors"
  [nodata-flag lines]
  (vec (map (fn [line]
              (print ".")
              (->> (.split line " ")
                   vec
                   (mapv #(if (= % nodata-flag)
                              0.0
                              (Double. %)))))
            lines)))

(defn read-description
  "read first 6 lines of file as description in format
   ncols         8640
   nrows         3432
   xllcorner     -180
   yllcorner     -58
   cellsize      0.0416666666667
   NODATA_value  -9999"
  [lines]
  (let [descr-parsed (->> lines
                          (map #(string/split % #" +"))
                          (into {}))
        long-value #(Long. (get descr-parsed %))
        double-value  #(Double. (get descr-parsed %))]
    {:cols      (long-value "ncols")
     :rows      (long-value "nrows")
     :x-corner  (double-value "xllcorner")
     :y-corner  (double-value "yllcorner")
     :cell-size (double-value "cellsize")
     :nodata (get descr-parsed "NODATA_value")}))

(defn read-ascii-density-file!
  "read density data from given path in ascii format
  (by NASA, from http://sedac.ciesin.columbia.edu/data/set/gpw-v3-population-density-future-estimates/data-download
  awhahaa) and set it in density-data and density-description atoms,
  so it can be later read by all threads concurrently.
  Should be called once on startup."
  [p]
  (with-open [reader (io/reader p)]
    (let [lines (line-seq reader)
          description-lines 6
          description (read-description (take description-lines lines))
          nodata (:nodata description)
          _ (log/info "reading density data...")
          data (read-ascii-density-file-data nodata
                                             (drop description-lines
                                                   lines))
          description (dissoc description :nodata)]
      (reset! density-data data)
      (reset! density-description description)
      (assert (= (count data)
                 (:rows description)))
      (assert (= (count (first data))
                 (:cols description))))))

(defn test-case [city-name lat lng expected-density]
  (let []))

(defn latitude->row-index
  "latitude is between -90 and 90,
   so row index = ((latitude - yllcorner) / cellsize)"
  [latitude]
  (let [{:keys [cell-size y-corner rows]} @density-description]
    (- rows
       (long (/ (- latitude y-corner)
                 cell-size)))))

(defn longtitude->column-index
  "longtitude is between -180 and 180,
   so column index = ((longtitude - xllcorner) / cellsize)"
  [longtitude]
  (let [{:keys [cell-size x-corner]} @density-description]
    (log/info [cell-size x-corner])
    (long (/ (- longtitude x-corner)
             cell-size))))

(defn cell-surface-area [latitude]
  (let [k (-> latitude
              (+ 90.0)
              Math/toRadians
              Math/sin)
        c-s (* (/ (:cell-size @density-description)
                  180.0)
                Math/PI
                earth-r-km)]
    (* c-s c-s k)))

; (defn coverage-s [km lat]
;   (let [a (Math/sqrt km)
;         n (/ a
;              (cell-surface-area lat))]
;     (log/info "n=" n)
;     (int (/ n 2))))

; (defn at [lat longt km]
;   (let [r (latitude->row-index lat)
;         c (longtitude->column-index longt)
;         d (coverage-s km lat)
;         rg (range (- d) (+ d 1))]
;     (mapv
;       (fn [x]
;           (mapv
;             (fn [y]
;               (get-in @density-data [(+ x r) (+ y c)]))
;             rg))
;       rg)))

; (defn find-density-at [latitude longtitude])
