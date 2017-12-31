(ns acc.io
  (:require [table.core :refer [table table-str]]
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

(defn sluggify [s]
  "Sluggifies a string."
  (-> s
      clojure.string/trim
      clojure.string/lower-case
      (clojure.string/replace #"[^A-Za-z0-9]" "-")
      (clojure.string/replace #"-+" "-")
      (#(if (> (count %) 1) (clojure.string/replace % #"^-+" "") %))
      (#(if (> (count %) 1) (clojure.string/replace % #"-+$" "") %))))

(defn keywordize-map
  "Keywordizes map keys."
  [m]
  (into {} (for [[k v] m]
             [(keyword (sluggify k)) v])))

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

(defn formatted-str
  "Returns formatted form of any data.
  The following formats are supported:

  * table tabular format (default)
  * org table in org format
  * unicode table with unicode characters
  * unicode-3d table that looks 3Dish
  * csv Comma delimited csv string
  * html HTML table
  * json Prettiefied JSON string"
  ([data] (formatted-str data "table"))
  ([data oformat]
   (case oformat
     "csv" (doric/table {:format doric/csv} data)
     "html" (doric/table {:format doric/html} data)
     "json" (c/generate-string data {:pretty JSON-PRETTY-PRINTER})
     "org" (table-str data :style :org)
     "table" (table-str data)
     "unicode" (table-str data :style :unicode)
     "unicode-3d" (table-str data :style :unicode-3d)
     (table-str data))))

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
   (println (formatted-str data oformat))))

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

(defn- prompt-for-number
  "Prompts for a number."
  [initial-input to-number-parser error-message message]
  (let [parser #(try (to-number-parser %1)
                     (catch NumberFormatException e
                       (do (println error-message)
                           (prompt-for-number nil to-number-parser error-message message))))]
    (if initial-input
      (parser initial-input)
      (do
        (print message)
        (flush)
        (let [s (clojure.string/trim (read-line))]
          (parser s))))))


(defn prompt-for-float
  "Prompts for a float."
  ([message] (prompt-for-float nil message))
  ([initial-input message]
   (prompt-for-number initial-input
                      #(Float/parseFloat %)
                      "Invalid input. Must be a valid real number."
                      message)))

(defn prompt-for-integer
  "Prompts for an integer."
  ([message] (prompt-for-integer nil message))
  ([initial-input message]
   (prompt-for-number initial-input
                      #(Integer/parseInt %)
                      "Invalid input. Must be a valid integer."
                      message)))

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

(defn format-all-ints
  "Formats all ints in data."
  [data format-string]
  (s/transform [(s/walker #(and (int? %)
                                (>= % 1000)))]
               #(format format-string %) data))

(defn format-as-currency
  "Formats all numbers as currency."
  [data]
  (-> data
      (format-all-floats "%,12.2f")
      (format-all-ints "%,12d")))

(defn table-int-str
  "Formats all floats in data as integers and returns table as string."
  [data]
  (table-str (format-all-floats data "%.0f")))

(defn table-currency-str
  "Formats all numbers as currency + adds commas + returns table as a string."
  [data]
  (table-str (format-as-currency data)))

(defn table-int
  "Formats all floats in data as integers and prints as a table."
  [data]
  (println (table-int-str data)))

(defn table-currency
  "Formats all numbers as currency + adds commas + prints as a table."
  [data]
  (println (table-currency-str data)))

(defn round-all-floats
  "Rounds all floats in data."
  [data]
  (s/transform [(s/walker float?)]
               #(Math/round %) data))
