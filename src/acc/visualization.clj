(ns acc.visualization
  (:require [gorilla-repl.table :refer [table-view]]
            [gorilla-repl.html :refer [html-view]]
            [plotly-clj.core :as p]
            [acc.io :as io :refer [table-int table-currency]]))

(defn format-for-plotly-chart
  "Formats time-series data for creating a plotly chart."
  ([data column-key] (format-for-plotly-chart data column-key {}))
  ([data column-key {:keys [x-column-key]
                     :or {x-column-key :x}}]
   (let [x-values (map #(get % x-column-key) data)
         series [:x x-values
                 :y (map column-key data)
                 :name column-key
                 :mode "lines+markers"
                 :type "scatter"]]
     series)))

(defn gorilla-view-table
  "Shows table data (list of hash-maps) in gorilla notebook."
  [data & column-keys]
  (let [column-keys (or column-keys
                        (keys (first data)))
        column-names (map (comp html-view name) column-keys)
        values (map (apply juxt (map #(comp html-view %) column-keys)) data)]
    (table-view values :columns column-names)))

(defn gorilla-view-charts
  "Shows charts in gorilla notebook for given tabular data (list of hash-maps)."
  ([data column-keys] (gorilla-view-charts data column-keys {}))
  ([data column-keys {:keys [x-column-key]
                      :or {x-column-key :x}}]
   (loop [w (p/plotly)
          f (partial p/add-scatter w)
          ckeys column-keys]
     (if (seq ckeys)
       (let [cdata (format-for-plotly-chart data (first ckeys) {:x-column-key x-column-key})
             wc (apply f cdata)]
         (recur wc (partial p/add-scatter wc) (rest ckeys)))
       (p/iplot w)))))
