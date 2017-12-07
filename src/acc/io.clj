(ns acc.io
  (:require [table.core :refer [table]]
            [com.rpl.specter :as s]
            [doric.core :as doric]
            [cheshire.core :as c]
            [clojure-csv.core :as csv]
            [acc.time :as t]
            [clojure.java.io :as io]))

(def JSON-PRETTY-PRINTER
  (c/create-pretty-printer
   (assoc c/default-pretty-print-options
          :indent-arrays? true)))

(defn keywordize-map
  "Keywordizes map keys."
  [m]
  (into {} (for [[k v] m]
             [(keyword (-> k
                           clojure.string/lower-case
                           (clojure.string/replace #"[^A-Za-z0-9]" "-"))) v])))

(defn csv-to-map
  "Parses a csv to a map.

  Kwargs:
      :delimiter - A character that contains the cell separator for
                   each column in a row.  Default value: \\,
      :end-of-line - A string containing the end-of-line character
                     for reading CSV files. If this setting is nil then
                     \\n and \\r\\n are both accepted.  Default value: nil
      :quote-char - A character that is used to begin and end a quoted cell.
                    Default value: \\\"
      :strict - If this variable is true, the parser will throw an
                exception on parse errors that are recoverable but
                not to spec or otherwise nonsensical.  Default value: false"
  [data & {:keys [delimiter end-of-line quote-char strict]
           :or {delimiter \,
                end-of-line nil
                quote-char \"
                strict false}}]
  (let [parsed (csv/parse-csv
                data
                :delimiter delimiter
                :end-of-line end-of-line
                :quote-char quote-char
                :strict strict)
        output (map (partial zipmap (first parsed))
                    (rest parsed))]
    (map keywordize-map output)))


(defn print-formatted
  "Prints formatted form of any data.
  The following formats are supported:

  * table tabular format (default)
  * org table in org format
  * unicode table with unicode characters
  * unicode-3d table that looks 3Dish
  * csv Comma delimited csv string
  * html HTML table
  * json Prettiefied JSON string"
  ([data] (print-formatted data "table"))
  ([data oformat]
   (case oformat
     "csv" (println (doric/table {:format doric/csv} data))
     "html" (println (doric/table {:format doric/html} data))
     "json" (println (c/generate-string data {:pretty JSON-PRETTY-PRINTER}))
     "org" (table data :style :org)
     "table" (table data)
     "unicode" (table data :style :unicode)
     "unicode-3d" (table data :style :unicode-3d)
     (table data))))

(defn prompt-for-string
  "Prompts for a string."
  ([message] (prompt-for-string message false))
  ([message blank-ok?]
   (print message)
   (flush)
   (let [s (clojure.string/trim (read-line))]
     (if (not (clojure.string/blank? s))
       s
       (if blank-ok? ""
           (do
             (println "Invalid input. Cannot be empty.")
             (prompt-for-string message)))))))

(defn prompt-for-float
  "Prompts for a float."
  [message]
  (print message)
  (flush)
  (let [s (clojure.string/trim (read-line))]
    (try (Float/parseFloat s)
         (catch NumberFormatException e
           (do (println "Invalid input. Must be a valid real number.")
               (prompt-for-float message))))))

(defn prompt-for-date
  "Prompts for a date string."
  [message default]
  (print message)
  (flush)
  (let [s (clojure.string/trim (read-line))]
    (if (clojure.string/blank? s)
      default
      (try
        (t/format-date (t/parse-as-date s))
        (catch IllegalArgumentException e
          (do (println "Invalid date. Must be for format" t/STANDARD-DATE-FORMAT)
              (prompt-for-date message default)))))))

(defn prompt-from-choices [message choices]
  "Prompts for a string which should be one of the given CHOICES."
  (let [choice (clojure.string/lower-case
                (prompt-for-string message))]
    (if (some #(= choice %) choices)
      choice
      (do
        (println "Input must be one of the following:")
        (print-formatted choices)
        (prompt-from-choices message choices)))))

(defn confirm
  "Confirms action with given message.
  Returns true if user inputs 'yes', false otherwise."
  [message]
  (flush)
  (let [input (prompt-from-choices message ["yes" "no"])]
    (= input "yes")))


(defn format-all-floats
  "Formats all floats in data."
  [data format-string]
  (s/transform [(s/walker float?)]
               #(format format-string %) data))
