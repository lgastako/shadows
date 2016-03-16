(ns shadows.core
  (:require [its.log :as log]))

(defn- select-with-rename [m mappings]
  (log/debug ::select-with-rename {:m m :mappings mappings})
  (if-not mappings
    nil
    (let [m (select-keys m (keys mappings))]
      (into {}
            (for [mapping mappings]
              (do
                (log/debug :mapping mapping)
                (assert (= 1 (count (keys mapping))))
                (let [k (first (keys mapping))
                      v (first (vals mapping))]
                  (log/debug :mapping {:k k :v v})
                  [v (get m k)])))))))

(defn from [m morks]
  (log/debug ::from :***** {:m m :morks morks})
  (let [{keys true mappings false} (group-by keyword? morks)]
    (log/debug :optimized :select-keys :keys keys)
    (log/debug :full-treatment mappings)
    (let [result
          (merge (select-keys m keys)
                 (select-with-rename m mappings))]
      (log/debug ::from :result result)
      result)))