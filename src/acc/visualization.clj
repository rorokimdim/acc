(ns acc.visualization
  (:require [gorilla-repl.table :refer [table-view]]
            [gorilla-repl.html :refer [html-view]]
            [plotly-clj.core :as p]
            [acc.io :as io :refer [table-int table-currency]]))

(defn format-for-plotly-chart
  "Formats time-series data for creating a plotly chart."
  ([data column-key] (format-for-plotly-chart data column-key {}))
  ([data column-key {:keys [x-column-key
                            x-axis-key
                            y-axis-key]
                     :or {x-column-key :x
                          x-axis-key nil
                          y-axis-key nil}}]
   (let [x-values (map #(get % x-column-key) data)
         series [:x x-values
                 :y (map column-key data)
                 :name column-key
                 :mode "lines+markers"
                 :marker {:size 1}
                 :type "scatter"
                 :hoverinfo "x+y"
                 :xaxis x-axis-key
                 :yaxis y-axis-key]]
     series)))

(defn gorilla-view-table
  "Shows table data (list of hash-maps) in gorilla notebook."
  ([data] (gorilla-view-table data nil nil))
  ([data column-keys] (gorilla-view-table data column-keys nil))
  ([data column-keys column-names]
   (let [column-keys (or column-keys
                         (keys (first data)))
         column-names (or column-names
                          (map (comp html-view name) column-keys))
         values (map (apply juxt (map #(comp html-view %) column-keys)) data)]
     (table-view values :columns column-names))))

(defn gorilla-view-scatter-plots
  "Shows charts in gorilla notebook for given tabular data (list of hash-maps)."
  ([data column-keys] (gorilla-view-scatter-plots data column-keys {}))
  ([data column-keys {:keys [title
                             x-column-key
                             x-axis-label
                             y-axis-label
                             x-axis-keys
                             y-axis-keys
                             x-number-format
                             y-number-format
                             extra-axis-definitions
                             legend]
                      :or {title ""
                           x-column-key :x
                           x-axis-label :x
                           y-axis-label :y
                           x-axis-keys nil
                           y-axis-keys nil
                           x-number-format ",.0f"
                           y-number-format ",.0f"
                           extra-axis-definitions []
                           legend nil}}]
   (let [num-columns (count column-keys)]
     (loop [w (p/plotly)
            f (partial p/add-scatter w)
            ckeys column-keys
            x-axis-keys (or x-axis-keys
                            (take num-columns (cycle [nil])))
            y-axis-keys (or y-axis-keys
                            (take num-columns (cycle [nil])))]
       (if (seq ckeys)
         (let [cdata (format-for-plotly-chart data
                                              (first ckeys)
                                              {:x-column-key x-column-key
                                               :x-axis-key (first x-axis-keys)
                                               :y-axis-key (first y-axis-keys)})
               wc (apply f cdata)]
           (recur wc
                  (partial p/add-scatter wc)
                  (rest ckeys)
                  (rest x-axis-keys)
                  (rest y-axis-keys)))
         (-> (apply (partial p/set-layout w)
                    (concat [:title title
                             :xaxis {:title x-axis-label :hoverformat x-number-format}
                             :yaxis {:title y-axis-label :hoverformat y-number-format}
                             :legend legend] extra-axis-definitions))
             p/iplot))))))
