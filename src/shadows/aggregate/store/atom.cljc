(ns shadows.aggregate.store.atom
  (:require [shadows.aggregate.store :as store]))

;; TODO: should we pass either to the store or the fn's the state, so for example
;; we could implement an AggStore that stores the aggregates in the projection state?
;; would we want to do this?
;; if we did, we would need to have assoc-agg be called to return a new value for state?

(defrecord AtomAggStore [data key->path]
  store/AggStore
  (get-agg [_ agg-key]
    (get-in @data (key->path agg-key)))
  (assoc-agg [_ agg-key agg]
    (swap! data assoc-in (key->path agg-key) agg)))

(defn make [key->path]
  (AtomAggStore. (atom {}) key->path))
