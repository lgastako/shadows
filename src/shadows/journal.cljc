(ns shadows.journal)

(defprotocol Journal
  (write [this event]))

(defn make-journaled [f journal]
  (fn [state event]
    (journal/write event)
    (f state event)))
