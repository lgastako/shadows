(ns shadows.aggregate.store)

(defprotocol AggStore
  (get-agg [this agg-key])
  (assoc-agg [this agg-key agg]))
