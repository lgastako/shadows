(ns shadows.snapshots)

(defprotocol Snapshotter
  (restore [this agg agg-key])
  (snapshot [this agg agg-key snapshot]))

(defn make-snapshotted [f snapshotter should-snapshot?]
  ;; TODO: we need to do per-agg-key snapshots, no?
  ;; Well, we are applying this to a projection function...
  ;; So with mm or fn versions we would snapshot the whole big table as is...
  ;; But with aggregate-projection...
  (let [seqnum (atom 0)]
    (fn [state event]
      (let [result (f state event)]
        (when (should-snapshot? seqnum)
          (snapshot snapshotter result))
        result))))
