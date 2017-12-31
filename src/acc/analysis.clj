(ns acc.analysis
  (:require [acc.dao.core :as dao]
            [acc.time :as t]))

(defn get-stats [numbers]
  "Gets stats on given list of numbers."
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
                              (/ (dec r_pow_n)))]
    payment-per-month))

(defn compute-mortgage-balance
  "Computes mortgage balance over time.

  options:
      :downpayment downpayment made (defaults to 20% of purchase-price)
      :duration-years duration of mortgage in years (defaults to 30 years)
      :interest-rate annual rate of interest (defaults to 0.04)
      :ppm payments to make every month
           (defaults to minimum amount to pay off mortgage in 30 years)"
  ([purchase-price] (compute-mortgage-balance purchase-price {}))
  ([purchase-price options] (compute-mortgage-balance
                             purchase-price
                             {:n 0
                              :cumulative-interest-paid 0
                              :principal-remaining nil}
                             options))
  ([purchase-price
    {:keys [n cumulative-interest-paid principal-remaining]}
    {:keys [downpayment
            duration-years
            interest-rate
            ppm]
     :or {downpayment (* 0.20 purchase-price)
          duration-years 30
          interest-rate 0.04}
     :as options}]
   (let [principal-remaining (or principal-remaining
                                 (- purchase-price downpayment))
         interest-ppm (-> principal-remaining
                          (* interest-rate)
                          (/ 12))
         ppm (min (or ppm
                      (compute-amortized-loan-payment-per-month
                       (- purchase-price downpayment)
                       interest-rate
                       duration-years))
                  principal-remaining)
         principal-ppm (- ppm interest-ppm)
         cumulative-interest-paid (+ cumulative-interest-paid interest-ppm)]
     (when (and (pos? principal-remaining)
                (< n (* 12 duration-years)))
       (lazy-seq (cons {:t n
                        :principal principal-remaining
                        :ppm ppm
                        :interest-ppm interest-ppm
                        :principal-ppm principal-ppm
                        :cumulative-interest-paid cumulative-interest-paid}
                       (compute-mortgage-balance purchase-price
                                                 {:n (inc n)
                                                  :cumulative-interest-paid cumulative-interest-paid
                                                  :principal-remaining (- principal-remaining principal-ppm)}
                                                 (assoc options :ppm ppm))))))))

(defn buy-vs-rent
  "Computes buy-vs-rent analysis over time.

  options:
      :downpayment downpayment made for home (defaults to 20% of home-price)
      :closing-cost closing cost when buying home (defaults to 4% of home-price)
      :mortgage-duration-years duration of mortgage in years (defaults to 30 years)
      :mortgage-monthly-payment payment made per month on mortgage
                                (defaults to minimum amount to pay off mortgage in mortgage-duration-years)
      :mortgage-interest-rate annual interest rate on mortgage (defaults to 0.04)
      :monthly-rent expected rent if renting instead of buying a home
                    (defaults to 2500)
      :monthly-cost-hoa HOA cost for home if any (defaults to 0)
      :home-appreciation-rate rate in percentage at which home value appreciates annually
                              (defaults to 0)
      :rent-appreciation-rate rate in percentage at which rent increases annually
                              (defaults to 0)
      :property-tax-rate annual property tax in percentage of home purchase value
                         (defaults to 0.01)
      :maintenance-cost-rate maintenance cost in percentage of home purchase value
                             (defaults to 0.01)
      :insurance-cost-rate insurance cost in percentage of home purchase value
                           (defaults to 0.002)
      :home-sale-cost-rate cost in percentage of home value when home is sold
                           (defaults to 0.06)
      :alternate-investments-return-rate rate of return of alternate investments
                           (defaults to 0.05)
      :income-tax-rate income tax rate to compute tax savings
                       (defaults to 0.33)
      :max-t max number of time units to return data for"
  ([home-price] (buy-vs-rent home-price {}))
  ([home-price options]
   (buy-vs-rent home-price
                {:n 0
                 :mortgage-computations nil
                 :cumulative-opportunity-cost-of-home 0}
                options))
  ([home-price
    {:keys [n
            mortgage-computations
            cumulative-opportunity-cost-of-home]}
    {:keys [downpayment
            closing-cost
            mortgage-ppm
            mortgage-duration-years
            mortgage-interest-rate
            rent-ppm
            hoa-ppm
            home-appreciation-rate
            rent-appreciation-rate
            property-tax-rate
            maintenance-cost-rate
            insurance-cost-rate
            home-sale-cost-rate
            income-tax-rate
            alternate-investments-return-rate
            max-t]
     :or {downpayment (* 0.20 home-price)
          closing-cost (* 0.04 home-price)
          mortgage-monthly-payment nil
          mortgage-duration-years 30
          mortgage-interest-rate 0.04
          rent-ppm 2500
          hoa-ppm 0
          home-appreciation-rate 0
          rent-appreciation-rate 0
          property-tax-rate 0.01
          maintenance-cost-rate 0.01
          insurance-cost-rate 0.002
          home-sale-cost-rate 0.06
          income-tax-rate 0.33
          alternate-investments-return-rate 0.05
          max-t nil}
     :as options}]
   (let [mortgage-computations (or mortgage-computations
                                   (compute-mortgage-balance
                                    home-price
                                    {:downpayment downpayment
                                     :ppm mortgage-ppm
                                     :interest-rate mortgage-interest-rate
                                     :duration-years mortgage-duration-years}))
         cost-property-tax (-> home-price
                               (* property-tax-rate)
                               (/ 12))
         cost-maintenance (-> home-price
                              (* maintenance-cost-rate)
                              (/ 12))
         cost-insurance (-> home-price
                            (* insurance-cost-rate)
                            (/ 12))
         nth-mortgage-computation (nth mortgage-computations n
                                       {:principal 0 :ppm 0 :interest-ppm 0 :principal-ppm 0})
         savings-tax (* (+ (:interest-ppm nth-mortgage-computation)
                           cost-property-tax)
                        income-tax-rate)
         principal-remaining (:principal nth-mortgage-computation)
         mortgage-ppm (if (pos? principal-remaining)
                        (:ppm nth-mortgage-computation) 0)
         rent-ppm (if (zero? n) rent-ppm (* rent-ppm (inc (/ rent-appreciation-rate 12))))
         interest-ppm (:interest-ppm nth-mortgage-computation)
         principal-ppm (:principal-ppm nth-mortgage-computation)
         home-value (* home-price (Math/pow (inc (/ home-appreciation-rate 12)) n))
         equity-gain (- home-value principal-remaining)
         opportunity-cost-of-downpayment-and-closing-cost
         (* (+ downpayment closing-cost)
            (Math/pow (inc (/ alternate-investments-return-rate 12)) n))
         opportunity-cost-of-home-in-month-n (- (+ mortgage-ppm
                                                   cost-property-tax
                                                   cost-maintenance
                                                   cost-insurance
                                                   hoa-ppm)
                                                rent-ppm
                                                savings-tax)
         cumulative-opportunity-cost-of-home (+ (* cumulative-opportunity-cost-of-home
                                                   (inc (/ alternate-investments-return-rate 12)))
                                                opportunity-cost-of-home-in-month-n)
         opportunity-cost (+ opportunity-cost-of-downpayment-and-closing-cost
                             cumulative-opportunity-cost-of-home)
         continue? (cond
                     (nil? max-t) (pos? principal-remaining)
                     :else (< n max-t))]
     (when continue?
       (lazy-seq (cons (apply array-map [:t n
                                         :y (-> n
                                                (/ 12.0)
                                                Math/floor
                                                Math/round)
                                         :rent-ppm rent-ppm
                                         :savings-tax savings-tax
                                         :mortgage-ppm mortgage-ppm
                                         :interest-ppm interest-ppm
                                         :principal-ppm principal-ppm
                                         :principal principal-remaining
                                         :equity-gain equity-gain
                                         :home-value home-value
                                         :opportunity-cost opportunity-cost
                                         :profit-from-sale (- (* (- 1 home-sale-cost-rate) home-value)
                                                              principal-remaining
                                                              opportunity-cost)])
                       (buy-vs-rent
                        home-price
                        {:n (inc n)
                         :mortgage-computations mortgage-computations
                         :cumulative-opportunity-cost-of-home cumulative-opportunity-cost-of-home}
                        (assoc options :rent-ppm rent-ppm))))))))

(defn compute-investment-growth
  "Computes investment growth over time.

  options:
      :compounding-rate annual compounding rate (defaults to 0.05)
      :investment-per-year annual additional investments made (defaults to 0)
      :expense-per-year annual expenses covered by this investments (defaults to 0)
      :investment-sales-tax-rate tax rate for sold investments (defaults to 0)"
  ([starting-balance] (compute-investment-growth starting-balance {}))
  ([starting-balance options] (compute-investment-growth starting-balance
                                                         {:n 0}
                                                         options))
  ([starting-balance
    {:keys [n]}
    {:keys [compounding-rate
            investment-per-year
            expense-per-year
            investment-sales-tax-rate]
     :or {compounding-rate 0.05
          investment-per-year 0
          expense-per-year 0
          investment-sales-tax-rate 0}
     :as options}]
   (let [investment-lost-per-year (-> expense-per-year
                                      (/ (- 1 investment-sales-tax-rate)))
         balance (-> starting-balance
                     (* (inc compounding-rate))
                     (- investment-lost-per-year)
                     (+ investment-per-year))]
     (lazy-seq (cons
                (let [all {:t n
                           :starting-balance starting-balance
                           :investment-per-year investment-per-year
                           :expense expense-per-year
                           :investment-lost-per-year investment-lost-per-year
                           :ending-balance balance}
                      keys-to-exclude (if (> 1 expense-per-year)
                                        [:expense :investment-lost-per-year] [])
                      keys-to-exclude (if (> 1 investment-per-year)
                                        (concat keys-to-exclude [:investment-per-year])
                                        keys-to-exclude)]
                  (apply (partial dissoc all) keys-to-exclude))
                (compute-investment-growth balance
                                           {:n (inc n)}
                                           options))))))

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
