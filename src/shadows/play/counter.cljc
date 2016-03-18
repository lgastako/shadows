(ns shadows.play.counter
  (:require [its.log :as log]
            [shadows.core :as shadows :refer [from merge-from]]
            [shadows.prelude :refer [not-implemented!]]))

(def events
  [:event1
   :event2
   :event3])

;; As Greg Young says, projections are left folds over a stream of events.

(defn counter [state event]
  (inc state))

(let [initial 0
      current (reduce counter initial events)]
  (log/debug :======================================================================)
  (log/debug ::counter)
  (log/debug :initial initial)
  (log/debug :current current)
  (log/debug (if (= 3 current)
               :pass
               :fail))
  (log/debug :======================================================================))

(log/set-level! :debug)
