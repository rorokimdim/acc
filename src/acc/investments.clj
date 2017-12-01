(ns acc.investments)

(ns acc.investments
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "acc/sql/investment.sql")
(hugsql/def-sqlvec-fns "acc/sql/investment.sql")
