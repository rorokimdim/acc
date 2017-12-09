(ns acc.dao.db
  (:require [hugsql.core :as hugsql]
            [acc.config :as config]))

(def DB {:classname "org.sqlite.JDBC"
         :subprotocol "sqlite"
         :subname config/DATABASE-FILE-PATH})

(hugsql/def-db-fns "acc/dao/sql/common.sql")
