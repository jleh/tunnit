(ns tunnit.core-test
  (:require [midje.sweet :refer :all]
            [tunnit.core :refer :all]))

 (defn sample-data []
   (list {:date "2018-02-01" :project-code 1 :time 135}
         {:date "2018-02-01" :project-code 2 :time 315}
         {:date "2018-02-02" :project-code 2 :time 100}
         {:date "2018-02-03" :project-code 1 :time 450}
         {:date "2018-02-03" :project-code 2 :time 10}))
 
 (defn sample-project-data []
   (list {:project-code 1 :worktime 10} {:project-code 1234 :worktime 100}))

(facts "about counting times"
  (fact "timelength returns correct minutes"
    (timelength "12:00-13:00") => 60
    (timelength "10:50-11:00") => 10
    (timelength "7.5h") => 450
    (timelength "2h") => 120)
    (+ (timelength "09:20-11:00") (timelength "11:30-17:00")) => 430
  (fact "Format time is count correctly"
    (format-time 60) => "1 h 0 min"
    (format-time 65) => "1 h 5 min"
    (format-time 50) => "0 h 50 min"
    (format-time -20) => "-0 h 20 min")
  (fact "Diff is calculated correctly"
    (calculate-diff 2 900) => 0
    (calculate-diff 2 870) => -30
    (calculate-diff 2 920) => 20)
  (fact "get-day-stats correctly"
    (get-day-stats "2018-02-01" (sample-data)) => {:date "2018-02-01" :worktime 450 :diff 0}
    (get-day-stats "2018-02-02" (sample-data)) => {:date "2018-02-02" :worktime 100 :diff -350}
    (get-day-stats "2018-02-03" (sample-data)) => {:date "2018-02-03" :worktime 460 :diff 10})
  (fact "get-total-minutes"
    (get-total-minutes (sample-data)) => 1010)
  (fact "Process line produces map"
    (process-line "2017-02-11 p1 10:00-12:00 foo") => {:time 120 :date "2017-02-11" :project-code 1}
    (process-line "2017-02-11 p1 2h foo") => {:time 120 :date "2017-02-11" :project-code 1}
    (process-line "") => {:project-code nil}
    (process-line "# comment") => {:project-code nil})
  (fact "empty records are filtered"
    (filter-empty-rows (list
                         {:project-code nil :time 0}
                         {:project-code 1 :time 10})) => (list {:project-code 1 :time 10}))
  (fact "gets stats for given dates correctly"
    (get-stats-for-day
                      ["2018-02-01" "2018-02-02"]
                      (sample-data)) => (list {
                                               :date "2018-02-01"
                                               :worktime 450
                                               :diff 0}
                                             {
                                               :date "2018-02-02" 
                                               :worktime 100
                                               :diff -350}))
  (fact "diff renders correctly"
    (format-time
      (calculate-diff
        1 (+ (timelength "09:20-11:00") (timelength "11:30-17:00")))) => "-0 h 20 min")
  (fact "project hours are calculated"
    (get-project-hours 1 (sample-data)) => {:project-code 1 :worktime 585})
  (fact "billed hours are calculated"
    (billed-hours (sample-project-data)) => 100)
  (fact "non-billed hours are calculated"
    (non-billed-hours (sample-project-data)) => 10)
  (fact "calculate percentage"
    (billed-percentage (sample-project-data))  => "90.91")
  (fact "project hours are counted"
    (project-hours (list 1 2) (sample-data)) => (list
                                                  {:project-code 1 :worktime 585}
                                                  {:project-code 2 :worktime 425})))
