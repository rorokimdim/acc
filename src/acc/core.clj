(ns acc.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.repl :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as jio]
            [com.rpl.specter :as sp]
            [table.core :refer [table table-str]]
            [com.hypirion.clj-xchart :as c]
            [acc.config :as config]
            [acc.dao.core :as dao]
            [acc.repl :as repl]
            [acc.time :as t]
            [acc.io :as io]
            [acc.analysis :as analysis]))

(def cli-options
  [["-h" "--help"]
   ["-f" "--file INPUT-FILE-PATH" "input file path"
    :default nil
    :parse-fn #(clojure.string/trim %)
    :validate [#(.exists (jio/as-file %))]]
   ["-p" "--port PORT" "port number"
    :default config/DEFAULT-REPL-PORT
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]])

(defn usage [options-summary]
  (->> ["Investment tracking and analysis tool."
        ""
        "Usage: acc [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  init       Initializes things necessary/nice-to-haves for acc"
        "  repl       Starts a repl"
        "  list       Lists all stored accounts or investments"
        "  add        Adds an account or investment record"
        "  analyze    Analyzes a particular account"
        "  summarize  Summarizes all accounts"
        "  delete     Deletes account or investment records"
        "  sql        Runs a sql query"
        "  compute    Runs specific computations"
        ""]
       (clojure.string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}
      errors
      {:exit-message (error-msg errors)}
      (and (< 0 (count arguments))
           (#{"init" "list" "add" "summarize" "analyze" "delete" "sql" "repl" "compute"}
            (first arguments)))
      {:action (first arguments) :options options :arguments (rest arguments)}
      :else {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn handle-add-account [options]
  (if-let [fpath (:file options)]
    (let [names (for [r (io/csv-to-map (slurp fpath))
                      :let [name (:name r)]
                      :when (not (clojure.string/blank? name))]
                  name)]
      (apply dao/add-accounts names))
    (let [name (io/prompt-for-string "Name of account: ")]
      (dao/add-accounts name))))

(defn handle-add-investment [options]
  (if-let [fpath (:file options)]
    (let [all-account-names (map :name (dao/get-accounts))
          records (for [r (io/csv-to-map (slurp fpath))
                        :let [{:keys [account-name amount date tag]
                               :or {tag ""}} r]]
                    (try (-> r
                             (assoc :account-name (io/sluggify account-name))
                             (assoc :amount (Float/parseFloat amount))
                             (assoc :date (t/format-date (t/parse-as-date date))))
                         (catch IllegalArgumentException e
                           (throw (IllegalArgumentException. (str "Invalid date " date))))
                         (catch NumberFormatException _
                           (throw (IllegalArgumentException. (str "Invalid amount " amount))))))
          invalid-account-names (set (for [r records
                                           :let [aname (:account-name r)]
                                           :when (not (some #(= aname %) all-account-names))]
                                       aname))]
      (if (empty? invalid-account-names)
        (apply dao/add-investments records)
        (do (println "The following accounts have not been defined yet:")
            (table invalid-account-names))))
    (let [all-account-names (map :name (dao/get-accounts))
          account-name (io/prompt-from-choices "Account Name: " all-account-names)
          amount (io/prompt-for-float "Amount: ")
          today-str (t/format-date (t/today))
          date (io/prompt-for-date (format "Date (%s): " today-str) today-str)
          tag (io/prompt-for-string "Tag: " true)]
      (dao/add-investment :account-name account-name
                          :amount amount
                          :date date
                          :tag tag))))

(defn handle-list-accounts
  ([] (handle-list-accounts "table"))
  ([oformat] (io/print-formatted (dao/get-accounts) oformat)))

(defn handle-list-investments
  ([] (handle-list-investments "table"))
  ([oformat] (io/print-formatted (io/format-all-floats (dao/get-investments) "%.2f") oformat)))

(defn execute-add-command [arguments options]
  (case (first arguments)
    "account" (handle-add-account options)
    "a" (handle-add-account options)
    "investment" (handle-add-investment options)
    "i" (handle-add-investment options)
    (println "Syntax: add account|a|investment|i"))
  (System/exit 0))

(defn execute-list-command [arguments]
  (let [dtype (first arguments)
        oformat (second arguments)]
    (case dtype
      "account" (handle-list-accounts oformat)
      "a" (handle-list-accounts oformat)
      "investment" (handle-list-investments oformat)
      "i" (handle-list-investments oformat)
      (println "Syntax: list account|a|investment|i"
               "[csv|html|json|org|table|unicode]"))
    (System/exit 0)))

(defn handle-delete-accounts [names]
  (if (seq names)
    (do
      (println "Deleting accounts with names" names)
      (when (io/confirm "Are you sure? (yes/no): ")
        (apply dao/delete-accounts names)))
    (println "Syntax: delete account|a name...")))

(defn handle-delete-investments [ids]
  (if (seq ids)
    (do
      (println "Deleting investment with ids" ids)
      (when (io/confirm "Are you sure? (yes/no): ")
        (apply dao/delete-investments ids)))
    (println "Syntax: delete investment|i id...")))

(defn execute-delete-command [arguments]
  (let [dtype (first arguments)
        params (rest arguments)]
    (case dtype
      "account" (handle-delete-accounts params)
      "a" (handle-delete-accounts params)
      "investment" (handle-delete-investments params)
      "i" (handle-delete-investments params)
      (println "Syntax: delete account|a|investment|i names|ids"))
    (System/exit 0)))

(defn execute-analyze-command [arguments]
  (let [all-account-names (map :name (dao/get-accounts))
        account-name (io/prompt-from-choices "Account Name: " all-account-names)
        total-current-value (io/prompt-for-float "Current value of account: ")
        adata (analysis/analyze account-name total-current-value)]
    (println "*" account-name)
    (println "** Aggregate stats")
    (table (io/format-all-floats (:aggregate-stats adata) "%.2f"))
    (println "** Annual compounding rate stats")
    (table (io/format-all-floats (:annual-compounding-rate-stats adata) "%.2f"))
    (println "** Analysis table")
    (table (io/format-all-floats (:analysis-table adata) "%.2f"))
    (System/exit 0)))

(defn execute-sql-command [arguments options]
  (let [fpath (:file options)
        sql (or (first arguments)
                (if fpath (slurp fpath) nil)
                (io/prompt-for-string "SQL: "))]
    (if sql
      (table (dao/execute-sql sql))
      (println "Syntax: sql \"SQL STRING\"")))
  (System/exit 0))

(defn execute-summarize-command
  "Summarizes all accounts."
  [arguments options]
  (table
   (io/format-all-floats
    (dao/execute-sql
     "SELECT SUM(amount),account_name FROM investment
     GROUP BY account_name
     ORDER BY account_name") "%.2f"))
  (System/exit 0))

(defn handle-compute-growth
  ([] (handle-compute-growth []))
  ([arguments]
   (let [n (or (nth arguments 0 nil) 20)
         starting-balance (io/prompt-for-integer (nth arguments 1 nil)
                                                 "Starting Balance: ")
         compounding-rate (io/prompt-for-float (nth arguments 2 nil)
                                               "Compounding rate: ")
         oformat (nth arguments 3 "table")
         investment-per-year (if (seq arguments) 0
                                 (io/prompt-for-float "Investment per year: "))
         expense-per-year (if (seq arguments) 0
                              (io/prompt-for-float "Expense per year covered from this investment: "))
         investment-sales-tax  (if (zero? expense-per-year) 0
                                   (io/prompt-for-float "Investment sales tax: "))]
     (io/print-formatted
      (take (if (string? n)
              (Integer/parseInt n) n)
            (analysis/compute-investment-growth
             :starting-balance starting-balance
             :compounding-rate compounding-rate
             :investment-per-year investment-per-year
             :expense-per-year expense-per-year
             :investment-sales-tax investment-sales-tax)) oformat))))

(defn execute-compute-command
  "Runs a specific computation."
  [arguments options]
  (let [ctype (first arguments)]
    (case ctype
      "growth" (handle-compute-growth (rest arguments))
      "g" (handle-compute-growth (rest arguments))
      (println "Syntax: compute growth|g"
               "[n=20]"
               "[starting-balance] [compounding-rate] [csv|html|json|org|table|unicode]"))
    (System/exit 0)))

(defn get-completions-for-ns
  ([ns] (get-completions-for-ns ns ""))
  ([ns prefix]
   (map #(str prefix %) ((comp keys ns-publics) (find-ns ns)))))

(defn generate-completions-file [file-path]
  (let [all-account-names (map :name (dao/get-accounts))
        completions (concat (get-completions-for-ns 'clojure.core)
                            (get-completions-for-ns 'clojure.repl)
                            (get-completions-for-ns 'com.hypirion.clj-xchart "c/")
                            (get-completions-for-ns 'acc.core)
                            (get-completions-for-ns 'acc.dao.core "dao/")
                            (get-completions-for-ns 'acc.io "io/")
                            ["table"])]
    (with-open [f (java.io.BufferedWriter. (java.io.FileWriter. file-path))]
      (.write f (apply str (interpose \newline completions)))
      (.write f (str \newline))
      (.write f (apply str (interpose \newline all-account-names))))))

(defn init
  "Initializes things necessary/nice-to-haves for acc."
  []
  (println "Initializing database as necessary...")
  (dao/init-db)
  (.mkdirs (java.io.File. config/CONFIG-DIRECTORY-PATH))
  (println "Generating completions file for repl...")
  (generate-completions-file config/COMPLETIONS-FILE-PATH))

(defn -main
  [& args]
  (let [{:keys [action options arguments exit-message ok?]}
        (validate-args args)]
    (dao/init-db)
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (try
        (case action
          "init" (init)
          "repl" (let [server (repl/start-server (:port options))]
                   (.addShutdownHook (Runtime/getRuntime)
                                     (Thread. (fn [] (repl/stop-server server))))
                   (repl/run-repl (:port options) server))
          "list" (execute-list-command arguments)
          "add" (execute-add-command arguments options)
          "summarize" (execute-summarize-command arguments options)
          "s" (execute-summarize-command arguments options)
          "analyze" (execute-analyze-command arguments)
          "delete" (execute-delete-command arguments)
          "sql" (execute-sql-command arguments options)
          "compute" (execute-compute-command arguments options))
        (catch Exception e
          (throw e)
          (println (str e)))))))
