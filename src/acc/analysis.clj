(ns acc.analysis
  (:require [acc.dao.core :as dao]
            [acc.time :as t]))

(defn get-stats [numbers]
  "Gets stats on give list of numbers."
  (if (empty? numbers)
    {}
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
       :std-dev std-dev})))

(defn compute-investment-growth
  "Lazily computes investment growth over time."
  [& {:keys [n
             starting-balance
             compounding-rate
             investment-per-year
             expense-per-year
             investment-sales-tax]
      :or {n 0
           starting-balance 100000
           compounding-rate 0.05
           investment-per-year 0
           expense-per-year 0
           investment-sales-tax 0}}]
  (let [investment-lost-per-year (-> expense-per-year
                                     (/ (- 1 investment-sales-tax))
                                     float
                                     Math/round)
        balance (-> starting-balance
                    (* (Math/pow (inc compounding-rate) 1))
                    (- investment-lost-per-year)
                    (+ investment-per-year)
                    Math/round)]
    (lazy-seq (cons
               (let [all {:t n
                          :starting-balance starting-balance
                          :investment-per-year (Math/round investment-per-year)
                          :expense (Math/round expense-per-year)
                          :investment-lost-per-year investment-lost-per-year
                          :ending-balance balance}
                     keys-to-exclude (if (> 1 expense-per-year)
                                       [:expense :investment-lost-per-year] [])
                     keys-to-exclude (if (> 1 investment-per-year)
                                       (concat keys-to-exclude [:investment-per-year])
                                       keys-to-exclude)]
                 (apply (partial dissoc all) keys-to-exclude))
               (compute-investment-growth :n (inc n)
                                          :starting-balance balance
                                          :compounding-rate compounding-rate
                                          :investment-per-year investment-per-year
                                          :expense-per-year expense-per-year
                                          :investment-sales-tax investment-sales-tax)))))

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
        total-gain-over-sum-of-investment-and-years (if (zero? sum-of-investment-and-years)
                                                      0
                                                      (/ total-gain
                                                         sum-of-investment-and-years))
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
                                     (map #(:annual-compounding-rate %)
                                          (filter #(pos? (:amount %))
                                                  analysis-table)))
     :analysis-table analysis-table}))
