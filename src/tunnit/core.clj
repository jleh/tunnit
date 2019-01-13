(ns tunnit.core
  (:gen-class)
  (:require [clojure.string :as str]))

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

(def previous-line (atom {}))

(defn get-time [time-str]
  (f/parse custom-formatter time-str))

(defn get-minutes [times]
  "Get minutes between two time values"
  (t/in-minutes (t/interval (get-time (get times 0)) (get-time (get times 1)))))

(defn parse-hours [hour-str]
  (let [hours (re-find #"\d*\.?\d*(?=h)" hour-str)]
    (if (nil? hours) 0 (read-string hours))))

(defn parse-minutes [hour-str]
  (let [minutes (re-find #"\d*\.?\d*(?=m)" hour-str)]
    (if (nil? minutes) 0 (read-string minutes))))

(defn parse-hour-str [hour-str]
  (let [hours (parse-hours hour-str)
        minutes (parse-minutes hour-str)]
  (int (+ (* hours 60) minutes))))

(defn timelength [time-str]
  "Get worktime for row in minutes. Two different format types are supported: 5h30min === 10:00-15:30"
  (if (boolean time-str)
    (if (nil? (index-of time-str "-"))
      (parse-hour-str time-str)
      (get-minutes (split time-str #"-"))) 0))

(defn parse-project-code [project-code]
  (read-string (str/replace project-code #"p" "")))

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

(defn is-project-code? [str]
  (str/starts-with? str "p"))

(defn is-missing-date? [line-data]
  (str/blank? (get line-data 0)))

(defn is-missing-project-code? [line-data]
  (or (str/blank? (get line-data 1)) (not(is-project-code? (get line-data 1)))))

(defn get-line-date [line-data]
  (if (is-missing-date? line-data)
    (:date @previous-line)
    (get line-data 0)))

(defn get-line-project-code [line-data]
  (if (is-missing-project-code? line-data)
    (:project-code @previous-line)
    (get line-data 1)))

(defn replace-missing-values [line-data]
  [(get-line-date line-data)
   (get-line-project-code line-data)
   (if (is-missing-project-code? line-data)
     (get line-data 1)
     (get line-data 2))
    (get line-data 3)
   ])

(defn get-line-values [line]
  (if (empty-or-comment? line) {:project-code nil}
    (let [line-data (replace-missing-values (split line #"\s+"))
          project-code (get-project-code line-data)]
      {:time (remove-saldovapaa-hours (timelength (get line-data 2)) project-code)
       :date (get line-data 0)
       :project-code project-code
       :comment (get line-data 3)})))

(defn process-line[line]
  (swap! previous-line merge (get-line-values line)))

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

(defn is-extra-day? [entry]
  (boolean (re-find #"extra-day" (:comment entry))) )

(defn get-day-stats [date entries]
  (let [worktime (apply + (map :time (filter #(= date (:date %)) entries)))
        extra-day? (some is-extra-day? (filter #(= date (:date %)) entries))]
    (zipmap [:date :extra-day? :worktime :diff] [date extra-day? worktime (if extra-day? worktime (- worktime 450))])))

(defn get-stats-for-day [dates entries]
  (map #(get-day-stats % entries) dates))

(defn print-day-stats [day-stats]
  (doseq [item day-stats]
    (println
      (str (:date item) "\t" (format-time (:worktime item)) "\t" (format-time (:diff item)) (if (:extra-day? item) "\t extra day")))))

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

(defn count-extra-days [entries]
  (count (filter is-extra-day? entries)))

(defn file-exists? [filename]
  (.exists (io/as-file filename)))

(defn is-directory? [filename]
  (.isDirectory (io/as-file filename)))

(defn read-file [filename initial-diff]
  "Reads file, prints statistics and returns diff in minutes."
  (with-open [rdr (reader filename)]
    (let [entries (filter-empty-rows (map process-line (line-seq rdr)))
          total-minutes (get-total-minutes entries)
          workdays (count-workdays entries)
          extra-days (count-extra-days entries)
          diff (+ initial-diff (calculate-diff (- workdays extra-days) total-minutes))
          projects (get-projects entries)]
        (print-day-stats (get-stats-for-day (distinct (map :date entries)) entries))
        (println)
        (doseq [row (map #(project-hours-row % total-minutes) projects)] (println row))
        (println)
        (println (str "Billed hours: " (format-time (billed-hours projects))))
        (println (str "Billed %: " (billed-percentage projects)) "% ")
        (println (str "Total worktime: " (format-time total-minutes)))
        (println (str "Extra days: " extra-days))
        (println (str "Difference: " (format-time diff) " (" diff " min)"))
        diff)))

(defn print-total-diff [total-diff]
  (newline)
  (println (str "Total difference: " (format-time total-diff) " (" total-diff " min)")))

(defn read-files [files initial-diff]
  (print-total-diff
    (reduce + initial-diff
      (for [file files]
        (read-file file 0)))))

(defn read-dir [dir-name]
  (file-seq (io/file dir-name)))

(defn get-file-paths [files]
  (map #(.getPath %) files))

(defn only-files [files]
  (filter #(.isFile %) files))

(defn read-dir-files [dir-name initial-diff]
  (read-files
    (get-file-paths
      (only-files
        (read-dir dir-name))) initial-diff))

(defn -main [& args]
  (let [{file :file diff :diff} (:options (parse-opts args cli-options))]
    (if (file-exists? file)
        (if (is-directory? file)
          (read-dir-files file diff)
          (read-file file diff))
      (println "Provide existing file or directory with -f option"))))
