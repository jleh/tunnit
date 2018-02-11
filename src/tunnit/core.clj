(ns tunnit.core)

(use 'clojure.java.io)
(use '[clojure.string :only (split)])

(require '[clj-time.core :as t])
(require '[clj-time.format :as f])

(def custom-formatter (f/formatter "yyyy-MM-dd HH:mm"))

(defn getTime [timeStr]
  (f/parse custom-formatter (str "2018-02-01 " timeStr)))

(defn getMinutes [times]
  (t/in-minutes
    (t/interval (getTime (get times 0)) (getTime (get times 1)))))

(defn timelength [timeStr]
  (if (boolean timeStr)
    (let [times (split timeStr #"-")]
      (getMinutes times)) 0))

(defn processLine [line]
  (let [lineData (split line #"\s+")]
    {:time (timelength (get lineData 2))
     :date (get lineData 0)
     :projectCode (get lineData 1)}))

(defn filterEmptyRows [entries]
  (filter #(false? (nil? (:projectCode %))) entries))

(defn getTotalTime [totalMinutes]
  (str (quot totalMinutes 60) " h " (mod totalMinutes 60) " min"))

(defn calculateDiff [workdays totalMinutes]
  (- totalMinutes (* workdays 450)))

(defn readFile []
  (with-open [rdr (reader "./data/2018-01.txt")]
    (let [entries (filterEmptyRows (map processLine (line-seq rdr)))
          totalMinutes (apply + (map :time entries))
          workdays (count (distinct (map :date entries)))]
        (println (str "Total worktime: " (getTotalTime totalMinutes)))
        (println (str "Difference: " (getTotalTime (calculateDiff workdays totalMinutes)))))))

(defn -main [] (readFile))
