(ns acc.config)

(def DEFAULT-REPL-PORT 9888)
(def CONFIG-DIRECTORY-PATH (str (System/getenv "HOME") "/.acc") )
(def COMPLETIONS-FILE-PATH (str CONFIG-DIRECTORY-PATH "/completions"))

(def DATABASE-FILE-PATH (get (System/getenv)
                             "ACC_DATABASE_FILE_PATH"
                             (str CONFIG-DIRECTORY-PATH "/account.db")))
