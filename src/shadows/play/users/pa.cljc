(ns shadows.play.users.pa
  (:require [its.log :as log]
            [shadows.aggregate :as agg]
            [shadows.aggregate.store :as store]
            [shadows.aggregate.store.atom :as atomstore]
            [shadows.core :as shadows :refer [from merge-from]]
            [shadows.play.users :as users]
            [shadows.prelude :refer [not-implemented!]]))

(defn aggregate-projecton [store aggregator update-agg-fn]
  (fn [state event]
    ;; (log/debug :aggregate-project {:state state :event event})
    (letfn [(f [state agg-key]
              (log/debug :aggregate-project :f :executing {:agg-key agg-key
                                                           :update-agg-fn update-agg-fn})
              (let [aggregate  (store/get-agg store agg-key)
                    _          (log/debug :executing {:on event})
                    aggregate' (update-agg-fn aggregate event)]
                (log/debug :aggregate-project :f :aggregate' aggregate')
                (store/assoc-agg store agg-key aggregate')))]
      (let [agg-keys (agg/agg-keys aggregator event)
            agg-keys (if (sequential? agg-keys)
                       agg-keys
                       [agg-keys])]
        ;; (log/debug :aggregate-project :pre-reduce {:agg-keys agg-keys})
        (let [result
              (reduce f state agg-keys)]
          (log/debug :aggregate-project {:result result})
          result)))))

(defmulti user
  "Update the user aggregate."
  (fn [_ event]
    (let [result (:event/type event)]
      (log/debug :pa/user {:result result :event event})
      result)))

(defmethod user :default
  [user event]
  user)

(defmethod user :user/Registered
  [user event]
  (merge-from user event [:username
                          :bcrypt-pwhash]))

(defmethod user :user/ChangedPassword
  [user event]
  (merge-from user event [:bcrypt-pwhash]))

(defmethod user :msg/Sent
  [user event]
  (let [to       (users/event->username event :to)
        from     (users/event->username event :from)
        field    (case (:username user)
                   to   :msgs/received
                   from :msgs/sent
                   nil)]
    (log/debug ::user :msg/Sent {:to to
                                 :from from
                                 :field field})
    (if-not field
      user
      (update user field (fnil inc 0)))))

(defrecord UserAggregator []
  agg/Aggregator
  (agg-keys [_ event]
    (if (= :msg/Sent (:event/type event))
      [{:username (get-in event [:event/data :to])}
       {:username (get-in event [:event/data :from])}]
      (when-let [username (get-in event [:event/data :username])]
        {:username username}))))

(defn user-aggregator []
  (UserAggregator.))

(let [store      (atomstore/make
                  (fn [agg-key]
                    [:users/by-username (:username agg-key)]))
      aggregator (user-aggregator)]
  (def project-user-aggregate
    (aggregate-projecton store
                         aggregator
                         user)))
