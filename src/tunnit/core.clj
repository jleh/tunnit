(ns tunnit.core
  (:gen-class))

(use 'clojure.java.io)
(use '[clojure.string :only (split index-of)])

(require '[clj-time.core :as t])
(require '[clj-time.format :as f])
(require '[clojure.tools.cli :refer [parse-opts]])
(require '[clojure.java.io :as io])

(def saldovapaa 909)

(def custom-formatter (f/formatter "HH:mm"))
(def cli-options [["-f" "--file FILE" "Filename"]
                  ["-d" "--diff DIFF" "Initial diff in minutes"
                   :default 0
                   :parse-fn #(Integer/parseInt %)]])

(defn get-time [time-str]
  (f/parse custom-formatter time-str))

(defn get-minutes [times]
  "Get minutes between two time values"
  (t/in-minutes (t/interval (get-time (get times 0)) (get-time (get times 1)))))

(defn parse-hour-str [hour-str]
  (let [hours (re-find #"\d*\.?\d*(?=h)" hour-str)
        minutes (re-find #"\d*\.?\d*(?=m)" hour-str)]
  (int (+ (* (if (nil? hours) 0 (read-string hours)) 60) (if (nil? minutes) 0 (read-string minutes))))))

(defn timelength [time-str]
  "Get worktime for row in minutes. Two different format types are supported: 5h30min === 10:00-15:30"
  (if (boolean time-str)
    (if (nil? (index-of time-str "-"))
      (parse-hour-str time-str)
      (get-minutes (split time-str #"-"))) 0))

(defn parse-project-code [project-code]
  (read-string (clojure.string/replace project-code #"p" "")))

(defn empty-line? [line]
  (= 0 (count line)))

(defn comment? [line]
  (= (subs line 0 1) "#"))

(defn empty-or-comment? [line]
  (or (empty-line? line) (comment? line)))

(defn get-project-code [line-data]
  (if (nil? (get line-data 1)) nil (parse-project-code (get line-data 1))))

(defn remove-saldovapaa-hours [hours project-code]
  "Removes hours with saldovapaa project code so difference calculating works correctly"
  (if (= project-code saldovapaa) (* 0 hours) hours))

(defn process-line [line]
  (if (empty-or-comment? line) {:project-code nil}
    (let [line-data (split line #"\s+")
          project-code (get-project-code line-data)]
      {:time (remove-saldovapaa-hours (timelength (get line-data 2)) project-code)
       :date (get line-data 0)
       :project-code project-code})))

(defn filter-empty-rows [entries]
  "Filter entries that have nil as project code"
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

(defn project-hours-percentage [project total-minutes]
  (format "%.2f" (float (* 100 (/ (:worktime project) total-minutes)))))

(defn project-hours-row [project, total-minutes]
  (str "p" (:project-code project) "\t" (format-time (:worktime project)) "\t" (project-hours-percentage project total-minutes) " %"))

(defn count-workdays [entries]
  (count (distinct (map :date entries))))

(defn file-exists? [filename]
  (.exists (io/as-file filename)))

(defn read-file [filename initial-diff]
  "Reads file and print statistics."
  (with-open [rdr (reader filename)]
    (let [entries (filter-empty-rows (map process-line (line-seq rdr)))
          total-minutes (get-total-minutes entries)
          workdays (count-workdays entries)
          diff (+ initial-diff (calculate-diff workdays total-minutes))
          projects (get-projects entries)]
        (print-day-stats (get-stats-for-day (distinct (map :date entries)) entries))
        (println)
        (doseq [row (map #(project-hours-row % total-minutes) projects)] (println row))
        (println)
        (println (str "Billed hours: " (format-time (billed-hours projects))))
        (println (str "Billed %: " (billed-percentage projects)) "% ")
        (println (str "Total worktime: " (format-time total-minutes)))
        (println (str "Difference: " (format-time diff) " (" diff " min)")))))

(defn -main [& args]
  (let [{file :file diff :diff} (:options (parse-opts args cli-options))]
    (if (file-exists? file)
      (read-file file diff)
      (println "Provide existing file with -f option"))))
