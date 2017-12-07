(ns acc.time
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(def STANDARD-DATE-FORMAT "yyyy-MM-dd")
(def DATE-FORMATTER (f/formatter (t/default-time-zone)
                                 STANDARD-DATE-FORMAT
                                 "MM/dd/yyyy"))

(defn parse-as-date
  "Parses a string into org.joda.time.DateTime object."
  [s]
  (f/parse DATE-FORMATTER s))

(defn valid-date-str?
  "Checks if a string is a valid date string."
  [s]
  (try
    (let [d (parse-as-date s)]
      true)
    (catch IllegalArgumentException _ false)))

(defn date-str-to-years-past [s]
  "Gets years past since given date string."
  (let [then (parse-as-date s)
        diff (t/interval then (t/today-at-midnight))]
    (/ (t/in-days diff) 365.0)))

(defn format-date
  "Formats data object as a string in STANDARD-DATE-FORMAT."
  [date]
  (f/unparse DATE-FORMATTER date))

(defn today
  "Gets today's date."
  []
  (t/today-at-midnight))
