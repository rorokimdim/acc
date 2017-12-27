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

(defn compute-amortized-loan-payment-per-month
  "Computes amortized payment per month.

  See derivation in https://en.wikipedia.org/wiki/Amortization_calculator."
  [loan-amount annual-interest-rate loan-duration-years]
  (let [r (inc (/ annual-interest-rate 12))
        n (* loan-duration-years 12)
        r_pow_n (Math/pow r n)
        payment-per-month (-> loan-amount
                              (* r_pow_n)
                              (* (dec r))
                              (/ (dec r_pow_n))
                              (Math/round))]
    payment-per-month))

(defn compute-mortgage-balance
  "Lazily computes mortgage balance over time."
  [& {:keys [n
             cumulative-interest-paid
             cumulative-net-other-costs
             property-tax-rate
             income-tax-rate

             purchase-price
             downpayment
             principal-remaining
             duration-years
             interest-rate
             payment-per-month]
      :or {n 0
           cumulative-interest-paid 0
           cumulative-net-other-costs 0
           property-tax-rate 0.01
           income-tax-rate 0.33
           duration-years 30}}]
  (let [downpayment (or downpayment (* 0.20 purchase-price))
        principal-remaining (or principal-remaining
                                (Math/round (- purchase-price downpayment)))
        interest-paid-per-month (-> principal-remaining
                                    (* interest-rate)
                                    (/ 12)
                                    (Math/round))
        payment-per-month (or payment-per-month
                              (compute-amortized-loan-payment-per-month
                               principal-remaining
                               interest-rate
                               duration-years))
        principal-paid-per-month (- payment-per-month interest-paid-per-month)
        principal-paid (* 12 principal-paid-per-month)
        interest-paid (* 12 interest-paid-per-month)
        cost-property-tax (Math/round (* property-tax-rate purchase-price))
        cost-maintenance (Math/round (* 0.01 purchase-price))
        cost-insurance (Math/round (* 0.002 purchase-price))
        tax-deductible (+ interest-paid
                          (* property-tax-rate purchase-price))
        tax-savings (Math/round (* income-tax-rate tax-deductible))
        net-other-costs (+ cost-property-tax
                           cost-insurance
                           cost-maintenance
                           (- tax-savings))
        net-other-costs-ppm (Math/round (/ net-other-costs 12.0))
        cumulative-net-other-costs (+ cumulative-net-other-costs net-other-costs)
        cumulative-interest-paid (+ cumulative-interest-paid interest-paid)]
    (when (pos? principal-remaining)
      (lazy-seq (cons {:t n
                       :principal principal-remaining
                       :ppm payment-per-month
                       :interest-ppm interest-paid-per-month
                       :principal-ppm principal-paid-per-month
                       :net-other-costs-ppm net-other-costs-ppm
                       :cumulative-net-other-costs cumulative-net-other-costs
                       :cumulative-interest-paid cumulative-interest-paid}
                      (compute-mortgage-balance :n (inc n)
                                                :cumulative-interest-paid cumulative-interest-paid
                                                :cumulative-net-other-costs cumulative-net-other-costs
                                                :purchase-price purchase-price
                                                :downpayment downpayment
                                                :principal-remaining (-> principal-remaining
                                                                         (- (* principal-paid-per-month 12))
                                                                         (float)
                                                                         (Math/round))
                                                :interest-rate interest-rate
                                                :duration-years duration-years
                                                :payment-per-month payment-per-month))))))

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
