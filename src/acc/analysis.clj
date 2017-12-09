(ns acc.analysis
  (:require [acc.dao.core :as dao]
            [acc.time :as t]))

(defn get-stats [numbers]
  (let [sorted-numbers (sort numbers)
        min-value (first sorted-numbers)
        max-value (last sorted-numbers)
        count-values (count numbers)
        average-value (/ (apply + numbers) count-values)
        mi (quot count-values 2)
        median-value (if (even? count-values)
                       (/ (+ (nth sorted-numbers mi)
                             (nth sorted-numbers (dec mi)))
                          2)
                       (nth sorted-numbers mi))
        square (fn [x] (* x x))
        variance (/ (apply + (map #(square (- % average-value)) numbers))
                    (dec count-values))
        std-dev (Math/sqrt variance)]
    {:min min-value
     :max max-value
     :count count-values
     :average average-value
     :median median-value
     :std-dev std-dev}))

(defn analyze
  "Analyzes rate of investment of a particular account."
  [account-name total-current-value]
  (let [records (dao/get-investments :cols ["amount" "date" "tag"]
                                     :names [account-name])
        total-invested-value (apply + (map :amount records))
        total-gain (- total-current-value total-invested-value)
        sum-of-investment-and-years (apply +
                                           (map
                                            #(* (:amount %)
                                                (t/date-str-to-years-past (:date %)))
                                            records))
        total-gain-over-sum-of-investment-and-years (/ total-gain
                                                       sum-of-investment-and-years)
        analysis-table (for [m records]
                         (let [years (t/date-str-to-years-past (:date m))
                               initial-value (:amount m)
                               gain (* total-gain-over-sum-of-investment-and-years
                                       initial-value
                                       years)
                               current-value (+ initial-value gain)
                               annual-compounding-rate (if (zero? years)
                                                         0
                                                         (* 100 (dec (Math/pow
                                                                      (/ current-value initial-value)
                                                                      (/ 1 years)))))]
                           (-> m
                               (assoc :years years)
                               (assoc :gain gain)
                               (assoc :current-value current-value)
                               (assoc :annual-compounding-rate annual-compounding-rate))))]
    {:aggregate-stats {:total-invested-value total-invested-value
                       :total-current-value total-current-value
                       :total-gain total-gain}
     :annual-compounding-rate-stats (get-stats
                                     (map #(:annual-compounding-rate %) analysis-table))
     :analysis-table analysis-table}))
