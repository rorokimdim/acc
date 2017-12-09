(ns acc.dao.accounts
  (:require [hugsql.core :as hugsql]
            [clojure.spec.alpha :as s]))

(hugsql/def-db-fns "acc/dao/sql/account.sql")
(hugsql/def-sqlvec-fns "acc/dao/sql/account.sql")

(s/def ::account-name (s/and string?
                             #(not (clojure.string/blank? %))
                             #(re-matches #"^[a-zA-Z0-9-]+$" %)))
