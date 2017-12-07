(ns acc.dao
  (:require [clojure.spec.alpha :as s]
            [clojure.java.jdbc :as jdbc]
            [acc.db :refer [DB] :as db]
            [acc.time :as t]
            [acc.accounts :as accounts]
            [acc.investments :as investments])
  )

(defn init-db
  "Initializes database as necessary."
  []
  (jdbc/with-db-transaction [tx DB]
    (accounts/create-table tx)
    (investments/create-table tx)
    (investments/create-index-on-account-name tx)
    (investments/create-index-on-date tx)))

(defn add-accounts
  "Adds new accounts."
  [& names]
  (jdbc/with-db-transaction [tx DB]
    (doseq [name names]
      (accounts/add tx {:name (clojure.string/lower-case name)}))))

(s/fdef add-accounts :args (s/cat :names (s/coll-of ::accounts/account-name)))


(defn delete-accounts
  "Deletes account records by names."
  [& names]
  (jdbc/with-db-connection [c DB]
    (db/enable-foreign-keys c)
    (accounts/delete c {:names names})))

(s/fdef delete-accounts :args (s/cat :names (s/coll-of ::accounts/account-name)))

(defn add-investments
  "Adds investment records.

  Each record must be a map with following fields:
      account-name: name of account (required)
      amount: value of investment (required)
      date: date of investments (optional, default to today's date)
      tag: any tag for the record (optional)"
  [& records]
  (jdbc/with-db-connection [c DB]
    (db/enable-foreign-keys c)
    (doseq [r records
            :let [{:keys [account-name amount date tag]
                   :or {date (t/format-date (t/today))
                        tag ""}} r]]
      (investments/add c {:account_name (clojure.string/lower-case account-name)
                          :amount amount
                          :date date
                          :tag (clojure.string/lower-case tag)}))))

(s/fdef add-investments :args (s/cat :records (s/coll-of ::investments/investment)))

(defn add-investment
  "Adds an investment.

  Kwargs:
      account-name: name of account (required)
      amount: value of investment (required)
      date: date of investments (optional, default to today's date)
      tag: any tag for the record (optional)"
  [& record]
  (add-investments record))

(defn delete-investments
  "Deletes investment records by ids."
  [& ids]
  (jdbc/with-db-connection [c DB]
    (db/enable-foreign-keys c)
    (investments/delete c {:ids ids})))

(defn get-investments
  "Gets investment records."
  [& {:keys [cols names order-by]
      :or {cols ["account_name" "amount" "date", "tag", "id"]
           order-by ["account_name" "date" "amount"]
           names []}}]
  (if (seq names)
    (investments/by-account-names DB {:cols cols :names names :order-by order-by})
    (investments/all DB {:cols cols :order-by order-by})))

(defn get-accounts
  "Gets all account names."
  []
  (accounts/all DB))
