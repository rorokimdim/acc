(ns acc.investments)

(ns acc.investments
  (:require [hugsql.core :as hugsql]
            [clojure.spec.alpha :as s]
            [acc.accounts :as accounts]
            [acc.time :as t]))

(hugsql/def-db-fns "acc/sql/investment.sql")
(hugsql/def-sqlvec-fns "acc/sql/investment.sql")

(s/def ::amount (s/and number? #(> % 0)))
(s/def ::tag string?)
(s/def ::date (s/and string?
                     t/valid-date-str?))


(s/def ::investment (s/keys :req-un [::accounts/account-name
                                     ::amount
                                     ::date]
                            :opt [::tag]))
