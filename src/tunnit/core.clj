(ns tunnit.core
  (:gen-class))

(use 'clojure.java.io)
(use '[clojure.string :only (split index-of)])

(require '[clj-time.core :as t])
(require '[clj-time.format :as f])
(require '[clojure.tools.cli :refer [parse-opts]])

(def custom-formatter (f/formatter "HH:mm"))
(def cli-options [["-f" "--file FILE" "Filename"]
                  ["-d" "--diff DIFF" "Initial diff in minutes" :default 0]])

(defn get-time [time-str]
  (f/parse custom-formatter time-str))

(defn get-minutes [times]
  (t/in-minutes (t/interval (get-time (get times 0)) (get-time (get times 1)))))

(defn parse-hour-str [hour-str]
  (let [hours (re-find #"\d*\.?\d*(?=h)" hour-str)
        minutes (re-find #"\d*\.?\d*(?=m)" hour-str)]
  (int (+ (* (if (nil? hours) 0 (read-string hours)) 60) (if (nil? minutes) 0 (read-string minutes))))))

(defn timelength [time-str]
  (if (boolean time-str)
    (if (nil? (index-of time-str "-"))
      (parse-hour-str time-str)
      (get-minutes (split time-str #"-"))) 0))

(defn parse-project-code [project-code]
  (read-string (clojure.string/replace project-code #"p" "")))

(defn process-line [line]
  (let [line-data (split line #"\s+")]
    {:time (timelength (get line-data 2))
     :date (get line-data 0)
     :project-code (if (nil? (get line-data 1)) nil (parse-project-code (get line-data 1)))}))

(defn filter-empty-rows [entries]
  (filter #(false? (nil? (:project-code %))) entries))

(defn time->to-str [minutes]
  (str (quot minutes 60) " h " (mod minutes 60) " min"))

(defn format-time [total-minutes]
  (if (neg? total-minutes)
    (str "-" (time->to-str (Math/abs total-minutes)))
    (time->to-str total-minutes)))

(defn calculate-diff [workdays total-minutes]
  (- total-minutes (* workdays 450)))

(defn get-day-stats [date entries]
  (let [worktime (apply + (map :time (filter #(= date (:date %)) entries)))]
    (zipmap [:date :worktime :diff] [date worktime (- worktime 450)])))

(defn get-stats-for-day [dates entries]
  (map #(get-day-stats % entries) dates))

(defn print-day-stats [day-stats]
  (doseq [item day-stats]
    (println
      (str (:date item) "\t" (format-time (:worktime item)) "\t" (format-time (:diff item))))))

(defn get-project-hours [project-code entries]
  (zipmap [:project-code :worktime]
          [project-code (apply + (map :time (filter #(= project-code (:project-code %)) entries)))]))

(defn project-hours [project-codes entries]
  (map #(get-project-hours % entries) project-codes))

(defn billed-hours [projects]
  (apply + (map :worktime (filter #(< 1000 (:project-code %)) projects))))

(defn non-billed-hours [projects]
  (apply + (map :worktime (filter #(> 1000 (:project-code %)) projects))))

(defn billed-percentage [projects]
  (let [billed (billed-hours projects)
        non-billed (non-billed-hours projects)]
    (format "%.2f" (float (* 100 (/ billed (+ billed non-billed)))))))

(defn get-total-minutes [entries]
  (apply + (map :time entries)))

(defn get-projects [entries]
  (sort #(compare (:project-code %1) (:project-code %2)) (project-hours (distinct (map :project-code entries)) entries)))

(defn project-hours-total [project]
  (str "p" (:project-code project) "\t" (format-time (:worktime project))))

(defn count-workdays [entries]
  (count (distinct (map :date entries))))

(defn read-file [filename initial-diff]
  (with-open [rdr (reader filename)]
    (let [entries (filter-empty-rows (map process-line (line-seq rdr)))
          total-minutes (get-total-minutes entries)
          workdays (count-workdays entries)
          diff (+ initial-diff (calculate-diff workdays total-minutes))
          projects (get-projects entries)]
        (print-day-stats (get-stats-for-day (distinct (map :date entries)) entries))
        (println)
        (doseq [row (map project-hours-total projects)] (println row))
        (println)
        (println (str "Billed hours: " (billed-percentage projects)) " %")
        (println (str "Total worktime: " (format-time total-minutes)))
        (println (str "Difference: " (format-time diff) " (" diff " min)")))))

(defn -main [& args]
  (let [file (:file (:options (parse-opts args cli-options)))
        diff (read-string (:diff (:options (parse-opts args cli-options))))]
    (read-file file diff)))
