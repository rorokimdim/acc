(defproject acc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[org.clojars.benfb/lein-gorilla "0.4.2-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/tools.cli "0.3.5"]
                 [cider/cider-nrepl "0.15.1"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [com.layerware/hugsql "0.4.8"]
                 [org.xerial/sqlite-jdbc "3.21.0"]
                 [com.rpl/specter "1.0.5"]
                 [cheshire "5.8.0"]
                 [table "0.5.0"]
                 [doric "0.9.0"]
                 [plotly-clj "0.1.1"]
                 [clj-time "0.14.2"]
                 [org.clojars.benfb/gorilla-repl "0.4.3-SNAPSHOT"]]
  :main ^:skip-aot acc.core
  :target-path "target/%s"
  :bin {:name "acc"
        :custom-preamble "#!/bin/sh\nmkdir -p \"$HOME/.acc\"\ntouch \"$HOME/.acc/completions\"\nbreakchars=\"(){}[],^%$#@\\\"\\\";:''|\"\nif ! [ -x \"$(command -v rlwrap)\" ]; then\n exec java {{{jvm-opts}}} -jar $0 \"$@\"\nelse\nexec rlwrap -a,, -pBlue -r -c -b \"$breakchars\" -f \"$HOME/.acc/completions\" -H \"$HOME/.acc/history\" java {{{jvm-opts}}} -jar $0 \"$@\"\nfi\n"
        :jvm-opts ["-server" "-Dfile.encoding=utf-8" "-Dapple.awt.UIElement=true" "$JVM_OPTS" ]}
  :profiles {:uberjar {:aot :all}})
