(ns acc.db
  (:require [hugsql.core :as hugsql]))

(def DB {:classname "org.sqlite.JDBC"
         :subprotocol "sqlite"
         :subname "account.db"})

(hugsql/def-db-fns "acc/sql/common.sql")
