(ns population.density
  (:gen-class)
  (:require [taoensso.timbre :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(defonce density-description (atom {}))
(defonce density-data (atom []))

(def not-found-flag -1)

(defn- read-ascii-density-file-data
  "reads lines, splits by space and converts to vector of vectors"
  [nodata-flag lines]
  (vec (map (fn [line]
              (print ".")
              (->> (.split line " ")
                   vec
                   (mapv #(if (= % nodata-flag)
                              not-found-flag
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

; (defn latitude->index )

; (defn find-density-at [latitude longtitude])
