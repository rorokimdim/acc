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

(def HOME-PRICE 800000)

(def BUY-VS-RENT-DATA
  (analysis/buy-vs-rent HOME-PRICE
                        {:monthly-rent 3000
                         :mortgage-monthly-payment 4000
                         :mortgage-duration-years 30
                         :closing-cost (* 0.06 HOME-PRICE)
                         :downpayment (* 0.20 HOME-PRICE)
                         :rent-appreciation-rate 0.025
                         :home-appreciation-rate 0.03
                         :alternate-investments-return-rate 0.03
                         :home-sale-cost-rate 0.0
                         :max-t 30
                         }))
;; @@

;; @@
(v/gorilla-view-charts BUY-VS-RENT-DATA
                       [:principal :equity-gain :opportunity-cost :net-profit-after-home-sale]
                       {:x-column-key :t})
;; @@

;; @@
(v/gorilla-view-table
  (io/format-as-currency BUY-VS-RENT-DATA)
  :t :interest-ppm :rent-ppm :mortgage-ppm :principal :equity-gain :opportunity-cost :net-profit-after-home-sale)
;; @@

;; @@

;; @@
