(ns tunnit.core-test
  (:require [midje.sweet :refer :all]
            [tunnit.core :refer :all]))

(facts "about counting times"
  (fact "timelength returns correct minutes"
    (timelength "12:00-13:00") => 60
    (timelength "10:50-11:00") => 10)
  (fact "Total time is count correctly"
    (getTotalTime 60) => "1 h 0 min"
    (getTotalTime 65) => "1 h 5 min"
    (getTotalTime 50) => "0 h 50 min")
  (fact "Diff is calculated correctly"
    (calculateDiff 2 900) => 0
    (calculateDiff 2 870) => -30
    (calculateDiff 2 920) => 20))
