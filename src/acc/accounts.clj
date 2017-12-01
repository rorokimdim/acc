(ns acc.accounts
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "acc/sql/account.sql")
(hugsql/def-sqlvec-fns "acc/sql/account.sql")
