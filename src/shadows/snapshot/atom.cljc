(ns shadows.snapshot.atom)

(defrecord ^:private AtomSnapshotter [ref]
  (restore [_ agg agg-key]
    (get-in @ref [agg agg-key]))
  (snapshot [_ agg agg-key snapshot]
    (swap! ref assoc-in ref [agg agg-key] snapshot)))

(defn snapshotter []
  (AtomSnapshotter. (atom {})))
