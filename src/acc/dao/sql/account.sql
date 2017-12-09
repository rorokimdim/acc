-- src/acc/dao/sql/account.sql
-- Investment Accounts

-- :name create-table
-- :command :execute
-- :result :raw
-- :doc Creates account table
CREATE TABLE IF NOT EXISTS account (
  name TEXT PRIMARY KEY CHECK(name <> '')
)

-- :name drop-table
-- :command :execute
-- :doc Drops account table
DROP TABLE IF EXISTS account

-- :name delete :! :*
-- :doc Deletes investment records
DELETE FROM account WHERE name IN (:v*:names)

-- :name add :insert :raw
-- :doc Adds a new account record
INSERT INTO account (name) VALUES (:name)

-- :name all
-- :doc Gets all records
SELECT name FROM account ORDER BY name
