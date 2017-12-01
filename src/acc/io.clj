(ns acc.io
  (:require [table.core :refer [table]]
            [doric.core :as doric]
            [cheshire.core :as c]))

(def JSON-PRETTY-PRINTER
  (c/create-pretty-printer
   (assoc c/default-pretty-print-options
          :indent-arrays? true)))

(defn print-formatted
  ([data] (print-formatted data "table"))
  ([data oformat]
   (case oformat
     "csv" (println (doric/table {:format doric/csv} data))
     "html" (println (doric/table {:format doric/html} data))
     "json" (println (c/generate-string data {:pretty JSON-PRETTY-PRINTER}))
     "org" (table data :style :org)
     "unicode" (table data :style :unicode)
     "unicode-3d" (table data :style :unicode-3d)
     (table data))))

(defn prompt-for-string [message]
  (print message)
  (flush)
  (let [s (clojure.string/trim (read-line))]
    (if (not (clojure.string/blank? s))
      s
      (do
        (println "Invalid input. Cannot be empty.")
        (prompt-for-string message)))))

(defn prompt-for-float [message]
  (print message)
  (flush)
  (let [s (clojure.string/trim (read-line))]
    (try (Float/parseFloat s)
         (catch NumberFormatException e
           (do (println "Invalid input. Must be a valid real number.")
               (prompt-for-float message))))))

(defn prompt-for-date [message date-format default]
  (print message)
  (flush)
  (let [s (clojure.string/trim (read-line))]
    (if (clojure.string/blank? s)
      default
      (try
        (.format (java.text.SimpleDateFormat. date-format)
                 (.parse (java.text.SimpleDateFormat. date-format) s))
        (catch java.text.ParseException e
          (do (println "Invalid date. Must be for format" date-format)
              (prompt-for-date message date-format default)))))))

(defn prompt-from-choices [message choices]
  (let [choice (clojure.string/lower-case
                (prompt-for-string message))]
    (if (some #(= choice %) choices)
      choice
      (do
        (println "Input must be one of the following:")
        (print-formatted choices)
        (prompt-from-choices message choices)))))
