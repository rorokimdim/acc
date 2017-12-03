(ns acc.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.repl :refer :all]
            [table.core :refer [table table-str]]
            [com.hypirion.clj-xchart :as c]
            [acc.config :as config]
            [acc.repl :as repl]
            [acc.io :as io]
            [acc.dao :as dao]))

(def cli-options
  [["-h" "--help"]
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
        "  init     Initializes things necessary/nice-to-haves for acc"
        "  repl     Starts a repl"
        "  list     Lists all stored accounts or investments"
        "  add      Adds an account or investment record"
        "  delete   Deletes account or investment records"
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
           (#{"init" "list" "add" "delete" "repl"} (first arguments)))
      {:action (first arguments) :options options :arguments (rest arguments)}
      :else {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn handle-add-account []
  (let [name (io/prompt-for-string "Name of account: ")]
    (dao/add-account name)))

(defn handle-add-investment []
  (let [all-account-names (map :name (dao/get-accounts))
        account-name (io/prompt-from-choices "Account Name: " all-account-names)
        amount (io/prompt-for-float "Amount: ")
        today (.format
               (java.text.SimpleDateFormat. "yyyy-MM-dd")
               (new java.util.Date))
        date (io/prompt-for-date (format "Date (%s): " today) "yyyy-MM-dd" today)
        tag (io/prompt-for-string "Tag: " true)]
    (dao/add-investment :account-name account-name
                        :amount amount
                        :date date
                        :tag tag)))

(defn handle-list-accounts
  ([] (handle-list-accounts "table"))
  ([oformat] (io/print-formatted (dao/get-accounts) oformat)))

(defn handle-list-investments
  ([] (handle-list-investments "table"))
  ([oformat] (io/print-formatted (dao/get-investments) oformat)))

(defn execute-add-command [arguments]
  (case (first arguments)
    "account" (handle-add-account)
    "a" (handle-add-account)
    "investment" (handle-add-investment)
    "i" (handle-add-investment)
    (println "Syntax: add account|a|investment|i"))
  (System/exit 0))

(defn execute-list-command [arguments]
  (let [dtype (first arguments)
        oformat (second arguments)]
    (case dtype
      "account" (handle-list-accounts)
      "a" (handle-list-accounts oformat)
      "investment" (handle-list-investments oformat)
      "i" (handle-list-investments oformat)
      (println "Syntax: list account|a|investment|i"
               "[csv|html|json|org|table|unicode]")))
  (System/exit 0))

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
      (println "Syntax: delete account|a|investment|i names|ids"))))

(defn get-completions-for-ns
  ([ns] (get-completions-for-ns ns ""))
  ([ns prefix]
   (map #(str prefix %) ((comp keys ns-publics) (find-ns ns)))))

(defn generate-completions-file [file-path]
  (let [completions (concat (get-completions-for-ns 'clojure.core)
                            (get-completions-for-ns 'clojure.repl)
                            (get-completions-for-ns 'com.hypirion.clj-xchart "c/")
                            (get-completions-for-ns 'acc.core)
                            (get-completions-for-ns 'acc.dao "dao/")
                            (get-completions-for-ns 'acc.io "io/")
                            ["table"])]
    (with-open [f (java.io.BufferedWriter. (java.io.FileWriter. file-path))]
      (.write f (apply str (interpose \newline completions))))))

(defn init
  "Initializes things necessary/nice-to-haves for acc."
  []
  (println "Initializing database as necessary...")
  (dao/init-db)
  (.mkdirs (java.io.File. config/CONFIG-DIRECTORY-PATH))
  (println "Generating completions file for repl...")
  (generate-completions-file config/COMPLETIONS-FILE-PATH)
  (System/exit 0))

(defn -main
  [& args]
  (let [{:keys [action options arguments exit-message ok?]}
        (validate-args args)]
    (dao/init-db)
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "init" (init)
        "repl" (let [server (repl/start-server (:port options))]
                 (.addShutdownHook (Runtime/getRuntime)
                                   (Thread. (fn [] (repl/stop-server server))))
                 (repl/run-repl (:port options) server))
        "list" (execute-list-command arguments)
        "add" (execute-add-command arguments)
        "delete" (execute-delete-command arguments)))))
