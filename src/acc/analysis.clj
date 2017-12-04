(ns acc.analysis
  (:require [acc.dao :as dao]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(def DATE-FORMATTER (f/formatter "yyyy-MM-dd"))

(defn date-str-to-years-past [s]
  (let [then (f/parse DATE-FORMATTER s)
        diff (t/interval then (t/today-at-midnight))]
    (/ (t/in-days diff) 365.0)))

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
                       (nth sorted-numbers mi))]
    {:min min-value
     :max max-value
     :count count-values
     :average average-value
     :median median-value}))

(defn analyze
  "Analyzes rate of investment of a particular account."
  [account-name total-current-value]
  (let [records (dao/get-investments :cols ["amount" "date" "tag"]
                                     :names [account-name])
        total-invested-value (apply + (map :amount records))
        total-gain (- total-current-value total-invested-value)
        sum-of-investment-and-years (apply +
                                           (map #(* (:amount %)
                                                    (date-str-to-years-past (:date %)))
                                                records))
        total-gain-over-sum-of-investment-and-years (/ total-gain
                                                       sum-of-investment-and-years)
        analysis-table (for [m records]
                         (let [years (date-str-to-years-past (:date m))
                               initial-value (:amount m)
                               gain (* total-gain-over-sum-of-investment-and-years
                                       initial-value
                                       years)
                               current-value (+ initial-value gain)
                               annual-compounding-rate (dec (Math/pow
                                                             (/ current-value initial-value)
                                                             (/ 1 years)))]
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
