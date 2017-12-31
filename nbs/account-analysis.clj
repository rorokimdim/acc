;; gorilla-repl.fileformat = 1

;; **
;;; # Accounts analysis
;; **

;; @@
(ns sensitive-snowflake
  (:require [clojure.set :as s]
            [gorilla-plot.core :as gp]
            [gorilla-repl.table :refer [table-view]]
            [gorilla-repl.html :refer [html-view]]

            [plotly-clj.core :as p]
            [acc.dao.core :as dao]
            [acc.io :as io]
            [acc.analysis :as analysis]
            [acc.visualization :as v])
  (:use [clojure.repl]))

(p/offline-init)

(def add-pie (fn [p & {:as params}] (p/add-fn p "pie" params)))
;; @@

;; @@
;; Configurations

(def ACCOUNTS-SUMMARY-SQL
  "SELECT
     round(SUM(amount), 0) AS balance,
     account_name AS 'Account Name',
     MAX(date) AS 'Last Investment Date'
   FROM
     investment
   GROUP BY
     account_name
   ORDER BY
     account_name")

(def ACCOUNTS-SUMMARY (dao/execute-sql ACCOUNTS-SUMMARY-SQL))

(def ACCOUNT-TO-ANALYZE "betterment-build-wealth")
(def CURRENT-ACCOUNT-VALUE 90000)

(def ANALYSIS-DATA (io/format-as-currency
                     (analysis/analyze ACCOUNT-TO-ANALYZE
                                       CURRENT-ACCOUNT-VALUE)))
;; @@

;; @@
(-> (p/plotly)
    (add-pie :labels (map (keyword "account name") ACCOUNTS-SUMMARY)
             :values (map :balance ACCOUNTS-SUMMARY))
    (p/set-layout :title "Investments by account"
                  :legend {:orientation "v"
                           :x -0.1})
    p/iplot)

(html-view (io/formatted-str ACCOUNTS-SUMMARY "html"))
;; @@

;; @@
(html-view "<strong>Aggregate stats<strong>")
(html-view (io/formatted-str [(:aggregate-stats ANALYSIS-DATA)] "html"))

(html-view "<strong>Annual compounding rate stats</strong>")
(html-view (io/formatted-str  [(:annual-compounding-rate-stats ANALYSIS-DATA)] "html"))

(html-view "<strong>Analysis Table<strong>")
(html-view (io/formatted-str (:analysis-table ANALYSIS-DATA) "html"))
;; @@
