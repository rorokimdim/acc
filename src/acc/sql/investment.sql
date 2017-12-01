-- src/acc/sql/investment.sql
-- Investment records

-- :name create-table
-- :command :execute
-- :result :raw
-- :doc Creates investment table
CREATE TABLE IF NOT EXISTS investment (
  id INTEGER PRIMARY KEY,
  account_name TEXT,
  amount REAL CHECK(amount > 0),
  date TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d', 'now')) CHECK (date IS strftime('%Y-%m-%d',date)),
  FOREIGN KEY(account_name) REFERENCES account(name) DEFERRABLE INITIALLY DEFERRED
)

-- :name create-index-on-account-name
-- :command :execute
CREATE INDEX IF NOT EXISTS idx_investment_account_name ON investment(account_name)


-- :name create-index-on-date
-- :command :execute
CREATE INDEX IF NOT EXISTS idx_investment_date ON investment(date)


-- :name drop-table
-- :command :execute
DROP TABLE IF EXISTS account

-- :name add :insert :raw
-- :doc Adds a new account record
INSERT INTO investment
  (account_name, amount, date)
VALUES (:account_name,
        :amount,
        :date)


-- :name all
-- :doc Gets all records
SELECT
  :i*:cols
FROM
  investment
ORDER BY
  :i*:order-by

-- :name by-account-names :? :*
-- :doc Gets all records with specified account names
SELECT
  :i*:cols
FROM
  investment
WHERE
  account_name IN (:v*:NAMES)
ORDER BY
  :i*:order-by
