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

(defn get-time [time-str]
  (f/parse custom-formatter (str "2018-02-01 " time-str)))

(defn get-minutes [times]
  (t/in-minutes (t/interval (get-time (get times 0)) (get-time (get times 1)))))

(defn parse-hour-str [hour-str]
  (int (* 60 (read-string (clojure.string/replace hour-str #"h" "")))))

(defn timelength [time-str]
  (if (boolean time-str)
    (if (nil? (index-of time-str "-"))
      (parse-hour-str time-str)
      (get-minutes (split time-str #"-"))) 0))

(defn process-line [line]
  (let [line-date (split line #"\s+")]
    {:time (timelength (get line-date 2))
     :date (get line-date 0)
     :project-code (get line-date 1)}))

(defn filter-empty-rows [entries]
  (filter #(false? (nil? (:project-code %))) entries))

(defn time-to-str [minutes]
  (str (quot minutes 60) " h " (mod minutes 60) " min"))

(defn format-time [total-minutes]
  (if (neg? total-minutes)
    (str "-" (time-to-str (Math/abs total-minutes)))
    (time-to-str total-minutes)))

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

(defn read-file [filename initial-diff]
  (with-open [rdr (reader filename)]
    (let [entries (filter-empty-rows (map process-line (line-seq rdr)))
          total-minutes (apply + (map :time entries))
          workdays (count (distinct (map :date entries)))
          diff (+ initial-diff (calculate-diff workdays total-minutes))]
        (print-day-stats (get-stats-for-day (distinct (map :date entries)) entries))
        (println)
        (println (str "Total worktime: " (format-time total-minutes)))
        (println (str "Difference: " (format-time diff) " (" diff " min)")))))

(defn -main [& args]
  (let [file (:file (:options (parse-opts args cli-options)))
        diff (read-string (:diff (:options (parse-opts args cli-options))))]
    (read-file file diff)))
