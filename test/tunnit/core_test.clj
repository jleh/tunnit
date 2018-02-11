(ns tunnit.core-test
  (:require [midje.sweet :refer :all]
            [tunnit.core :refer :all]))

 (defn sampleData []
   (list {:date "2018-02-01" :projectCode "p1" :time 135}
         {:date "2018-02-01" :projectCode "p1" :time 315}
         {:date "2018-02-02" :projectCode "p1" :time 100}
         {:date "2018-02-03" :projectCode "p1" :time 450}
         {:date "2018-02-03" :projectCode "p1" :time 10}))

(facts "about counting times"
  (fact "timelength returns correct minutes"
    (timelength "12:00-13:00") => 60
    (timelength "10:50-11:00") => 10)
  (fact "Total time is count correctly"
    (formatTime 60) => "1 h 0 min"
    (formatTime 65) => "1 h 5 min"
    (formatTime 50) => "0 h 50 min")
  (fact "Diff is calculated correctly"
    (calculateDiff 2 900) => 0
    (calculateDiff 2 870) => -30
    (calculateDiff 2 920) => 20)
  (fact "getDayStats correctly"
    (getDayStats "2018-02-01" (sampleData)) => {:date "2018-02-01" :worktime 450 :diff 0}
    (getDayStats "2018-02-02" (sampleData)) => {:date "2018-02-02" :worktime 100 :diff -350}
    (getDayStats "2018-02-03" (sampleData)) => {:date "2018-02-03" :worktime 460 :diff 10})
  (fact "gets stats for given dates correctly"
    (getStatsForDays
                      ["2018-02-01" "2018-02-02"]
                      (sampleData)) => (list {
                                               :date "2018-02-01"
                                               :worktime 450
                                               :diff 0}
                                             {
                                               :date "2018-02-02" 
                                               :worktime 100
                                               :diff -350})))
