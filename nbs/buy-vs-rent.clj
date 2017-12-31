;; gorilla-repl.fileformat = 1

;; @@
(ns wandering-mountain
  (:require [gorilla-plot.core :as gp]
            [gorilla-repl.table :refer [table-view]]
            [gorilla-repl.html :refer [html-view]]
            
            [plotly-clj.core :as p]
            [acc.io :as io]
            [acc.analysis :as analysis]
            [acc.visualization :as v])
  (:use [clojure.repl]))

(p/offline-init)
;; @@

;; @@
;;
;; Configurations
;;

(def HOME-PRICE 750000)
(def MORTGAGE-DURATION-YEARS 30)

(def BUY-VS-RENT-DATA
  (analysis/buy-vs-rent HOME-PRICE
                        {:rent-ppm 2500
                         :mortgage-ppm 4000
                         :mortgage-duration-years MORTGAGE-DURATION-YEARS
                         :closing-cost (* 0.06 HOME-PRICE)
                         :downpayment (* 0.20 HOME-PRICE)
                         :rent-appreciation-rate 0.025
                         :home-appreciation-rate 0.04
                         :alternate-investments-return-rate 0.07
                         :home-sale-cost-rate 0.06
                         :max-t (* MORTGAGE-DURATION-YEARS 12 1)
                         }))
;; @@

;; @@
(v/gorilla-view-charts BUY-VS-RENT-DATA
                       [:principal :equity-gain :opportunity-cost :profit-from-sale]
                       {:x-column-key :t
                        :x-axis-label "t (months)"
                        :y-axis-label "Amount $"
                        :title "Time Series Chart"
                        :legend {:orientation "h" :x 0.1 :y -0.3}
                        :y-axis-keys [nil nil nil "y2"]
                        :extra-axis-definitions [:yaxis2 {:title "profit-from-sale"
                                                          :side "right"
                                                          :overlaying "y"
                                                          :hoverformat ",.0f"
                                                          :tickfont {:color "rgb(212,42,47)"}}]})
;; @@

;; @@
(v/gorilla-view-table
  (io/format-as-currency BUY-VS-RENT-DATA)
  :t :interest-ppm :rent-ppm :mortgage-ppm :principal-ppm :principal :equity-gain :opportunity-cost :profit-from-sale)
;; @@
