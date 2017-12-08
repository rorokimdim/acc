(ns acc.time
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(def STANDARD-DATE-FORMAT "yyyy-MM-dd")
(def DATE-FORMATTER (f/formatter (t/default-time-zone)
                                 STANDARD-DATE-FORMAT
                                 "yyyy-MM-dd H:m:s Z"
                                 "MM/dd/yyyy"))

(defn parse-as-date
  "Parses a string into org.joda.time.DateTime object."
  [s]
  (if (string? s)
    (f/parse DATE-FORMATTER s)
    (throw (IllegalArgumentException. "Date string cannot be nil"))))

(defn valid-date-str?
  "Checks if a string is a valid date string."
  [s]
  (try
    (let [d (parse-as-date s)]
      true)
    (catch IllegalArgumentException _ false)))

(defn date-str-to-years-past
  "Gets years past since given date string."
  [s]
  (let [then (parse-as-date s)
        midnight-today (t/today-at-midnight)
        is-then-in-future (t/after? then midnight-today)
        diff (if is-then-in-future
               (t/interval (t/today-at-midnight) then)
               (t/interval then (t/today-at-midnight)))
        days (/ (t/in-days diff) 365.0)]
    (if is-then-in-future (- days) days)))

(defn format-date
  "Formats data object as a string in STANDARD-DATE-FORMAT."
  [date]
  (f/unparse DATE-FORMATTER date))

(defn today
  "Gets today's date."
  []
  (t/today-at-midnight))
