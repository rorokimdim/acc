(ns acc.repl
  (:require [clojure.tools.nrepl [server :as nrepl-server]]
            [clojure.tools.nrepl :as nrepl]
            [cider.nrepl :refer (cider-nrepl-handler)]))

(def COLORED-OUTPUT
  {:err #(binding [*out* *err*]
           (print "\033[31m")
           (println %)
           (println "\033[m")
           (flush))
   :out print
   :value (fn [x]
            (print "\033[34m")
            (println x)
            (print "\033[m")
            (flush))})

(defn start-server
  "Starts a repl server."
  [port]
  (println (format "Starting repl in port %d..." port))
  (nrepl-server/start-server :port port :handler cider-nrepl-handler))

(defn run-repl
  "Runs a simple repl."
  ([port] (run-repl port nil))
  ([port {:keys [prompt err out value]
          :or {prompt #(print (str % "=> "))
               err println
               out print
               value println}}]
   (let [transport (nrepl/connect :host "localhost" :port port)
         client (nrepl/client-session (nrepl/client transport Long/MAX_VALUE))
         ns (atom "user")
         {:keys [major minor incremental qualifier]} *clojure-version*
         evaluator (fn [code]
                     (doseq [res (nrepl/message client {:op "eval" :code code})]
                       (when (:value res) (value (:value res)))
                       (when (:out res) (out (:out res)))
                       (when (:err res) (err (:err res)))
                       (when (:ns res) (reset! ns (:ns res)))))]
     (println (str "Clojure " (clojure-version)))
     (evaluator "(in-ns 'acc.core)")
     (loop []
       (prompt @ns)
       (flush)
       (let [iobj (read *in* false :end)]
         (if (not= iobj :end)
           (do (evaluator (pr-str iobj))
               (recur))
           (do (println "Goodbye, see you later!")
               (System/exit 0))))))))
