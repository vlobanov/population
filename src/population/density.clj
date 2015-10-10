(ns population.density
  (:gen-class)
  (:require [taoensso.timbre :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(defonce density-description (atom {}))
(defonce density-data (atom []))

(def earth-r-km 6371)

(def max-radius 500)

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

(defn latitude->row-index
  "latitude is between -90 and 90,
   first row is at the bottom of the grid,
   so row index = (Nrows - (latitude - yllcorner) / cellsize)"
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
    (long (/ (- longtitude x-corner)
             cell-size))))

(defn latitude-sin [latitude]
  (-> latitude
      (+ 90.0)
      Math/toRadians
      Math/sin))

(defn cell-size-no-distortion-km []
  (* (/ (:cell-size @density-description)
        180.0)
      Math/PI
      earth-r-km))

(defn- density-in-cell [r c]
  (let [{:keys [rows cols]} @density-description]
    (get-in @density-data [(mod r rows)
                           (mod c cols)])))

(defn offsets-distance
  "Measures distance in some arbitrary units between cells.
   width of cell is smaller than height by sin(latitude),
   so we account for thar"
  [lat-sin r c]
  (let [c-k (* lat-sin c)]
    (+ (* r r)
       (* c-k c-k))))

(defn offsets-for
  "Figures out how many cells up/down and right/left we have to look
   to cover circle on given radius"
  [lat-sin radius-km]
  (let [row-offset (Math/ceil
                     (/ radius-km
                        (cell-size-no-distortion-km)))
        column-offset (Math/ceil
                        (/ row-offset lat-sin))]
    [(long row-offset) (long column-offset)]))

(defn densities-around
  "get a list of pairs (population density, distance to given point)"
  [latitude longtitude radius-km]
  (let [lat-sin (latitude-sin latitude)
        [r-cells-offset c-cells-offset] (offsets-for lat-sin radius-km)
        c (longtitude->column-index longtitude)
        r (latitude->row-index latitude)]
    (for [r-offset (range (- r-cells-offset) (+ r-cells-offset 1))
          c-offset (range (- c-cells-offset) (+ c-cells-offset 1))]
      (let [dist (offsets-distance lat-sin r-offset c-offset)
            density (density-in-cell (+ r-offset r)
                                     (+ c-offset c))]
        [dist density]))))

(defn circle-surface-area [radius-km]
  (* Math/PI (* radius-km radius-km)))

(defn cell-surface-area
  "we know size of grid cell in degrees,
   but may need to get its surface area"
  [latitude]
  (let [k (latitude-sin latitude)
        c-s (cell-size-no-distortion-km)]
    (* c-s c-s k)))

(defn population-in-cell [area cell-area cell-density]
  (* (if (> area cell-area) cell-area area)
     cell-density))

(defn count-population
  "Actually does what it says.
   First we get all cells close to given point,
   Sort by distance and in greedy manner while we
   still have some area left multiply density by area"
  [latitude longtitude radius-km]
  (let [densities (->> (densities-around latitude longtitude radius-km)
                       (sort-by first)
                       (map last))
        cell-area (cell-surface-area latitude)]
    (loop [left-area (circle-surface-area radius-km)
           population 0
           densities densities]
      (if (or (<= left-area 0) (empty? densities))
          (long population)
          (recur (- left-area cell-area)
                 (+ population
                    (population-in-cell left-area
                                        cell-area
                                        (first densities)))
                 (rest densities))))))
