(ns tunnit.core
  (:gen-class))

(use 'clojure.java.io)
(use '[clojure.string :only (split index-of)])

(require '[clj-time.core :as t])
(require '[clj-time.format :as f])
(require '[clojure.tools.cli :refer [parse-opts]])

(def custom-formatter (f/formatter "yyyy-MM-dd HH:mm"))
(def cli-options [["-f" "--file FILE" "Filename"]
                  ["-d" "--diff DIFF" "Initial diff in minutes" :default 0]])

(defn getTime [timeStr]
  (f/parse custom-formatter (str "2018-02-01 " timeStr)))

(defn getMinutes [times]
  (t/in-minutes
    (t/interval (getTime (get times 0)) (getTime (get times 1)))))

(defn timelength [timeStr]
  (if (boolean timeStr)
    (if (nil? (index-of timeStr "-"))
      (int (* 60 (read-string (clojure.string/replace timeStr #"h" ""))))
      (let [times (split timeStr #"-")]
        (getMinutes times)))
     0))

(defn processLine [line]
  (let [lineData (split line #"\s+")]
    {:time (timelength (get lineData 2))
     :date (get lineData 0)
     :projectCode (get lineData 1)}))

(defn filterEmptyRows [entries]
  (filter #(false? (nil? (:projectCode %))) entries))

(defn time-to-str [minutes]
  (str (quot minutes 60) " h " (mod minutes 60) " min"))

(defn formatTime [totalMinutes]
  (if (neg? totalMinutes)
    (str "-" (time-to-str (Math/abs totalMinutes)))
    (time-to-str totalMinutes)))

(defn calculateDiff [workdays totalMinutes]
  (- totalMinutes (* workdays 450)))

(defn getDayStats [date entries]
  (let [worktime (apply + (map :time (filter #(= date (:date %)) entries)))]
    (zipmap [:date :worktime :diff] [date worktime (- worktime 450)])))

(defn getStatsForDays [dates entries]
  (map #(getDayStats % entries) dates))

(defn print-day-stats [day-stats]
  (doseq [item day-stats]
    (println
      (str (:date item) "\t" (formatTime (:worktime item)) "\t" (formatTime (:diff item))))))

(defn readFile [filename initialDiff]
  (with-open [rdr (reader filename)]
    (let [entries (filterEmptyRows (map processLine (line-seq rdr)))
          totalMinutes (apply + (map :time entries))
          workdays (count (distinct (map :date entries)))
          diff (+ initialDiff (calculateDiff workdays totalMinutes))]
        (print-day-stats (getStatsForDays (distinct (map :date entries)) entries))
        (println)
        (println (str "Total worktime: " (formatTime totalMinutes)))
        (println (str "Difference: " (formatTime diff) " (" diff " min)")))))

(defn -main [& args]
  (let [file (:file (:options (parse-opts args cli-options)))
        diff (read-string (:diff (:options (parse-opts args cli-options))))]
    (readFile file diff)))
