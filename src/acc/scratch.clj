(ns acc.scratch
  (:require
   [com.rpl.specter :as sp]
   [com.hypirion.clj-xchart :as c]
   [acc.dao.core :as dao]
   [acc.time :as t]
   [acc.io :as io :refer [table-int table-currency]]
   [acc.analysis :as a]
   [acc.visualization :as v]))

(def HOME-PRICE 800000)

(def OPTIONS-BUY-VS-RENT
  {:monthly-rent 3000
   :mortgage-monthly-payment 4000
   :mortgage-duration-years 30
   :closing-cost (* 0.06 HOME-PRICE)
   :downpayment (* 0.20 HOME-PRICE)
   :rent-appreciation-rate 0.025
   :home-appreciation-rate 0.03
   :alternate-investments-return-rate 0.07})

(v/view-buy-vs-rent
 (a/buy-vs-rent HOME-PRICE OPTIONS-BUY-VS-RENT))

(table-currency (a/buy-vs-rent HOME-PRICE OPTIONS-BUY-VS-RENT))
