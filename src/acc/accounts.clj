(ns acc.accounts
  (:require [hugsql.core :as hugsql]
            [clojure.spec.alpha :as s]))

(hugsql/def-db-fns "acc/sql/account.sql")
(hugsql/def-sqlvec-fns "acc/sql/account.sql")

(s/def ::account-name (s/and string?
                             #(not (clojure.string/blank? %))
                             #(re-matches #"^[a-zA-Z0-9-]+$" %)))
