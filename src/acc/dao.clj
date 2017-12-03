(ns acc.dao
  (:require [clojure.java.jdbc :as jdbc]
            [acc.db :refer [DB] :as db]
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

(defn add-account
  "Adds an account with name NAME."
  [name]
  (jdbc/with-db-connection [c DB]
    (db/enable-foreign-keys c)
    (accounts/add c {:name (clojure.string/lower-case name)})))

(defn delete-accounts
  "Deletes account records by names."
  [& names]
  (jdbc/with-db-connection [c DB]
    (db/enable-foreign-keys c)
    (accounts/delete c {:names names})))


(defn add-investment
  "Adds an investment."
  [& {:keys [account-name amount date tag]
      :or {date (.format
                 (java.text.SimpleDateFormat. "yyyy-MM-dd")
                 (new java.util.Date))
           tag ""}}]
  (jdbc/with-db-connection [c DB]
    (db/enable-foreign-keys c)
    (investments/add c {:account_name (clojure.string/lower-case account-name)
                        :amount amount
                        :date date
                        :tag (clojure.string/lower-case tag)})))

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
