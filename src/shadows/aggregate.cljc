(ns shadows.aggregate)

(defprotocol Aggregator
  (agg-keys [this event]))
