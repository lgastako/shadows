(ns shadows.play.users.pa
  (:require [its.log :as log]
            [shadows.core :as shadows :refer [from merge-from]]
            [shadows.play.users :as users]
            [shadows.prelude :refer [not-implemented!]]))

(defn aggregate-projecton [get-agg-fn get-agg-keys assoc-agg-fn update-agg-fn]
  (fn [state event]
    (log/warn :aggregate-project :fn)
    (letfn [(f [state agg-key]
              (log/warn :executing :agg-key agg-key)
              (let [aggregate  (get-agg-fn state agg-key)
                    aggregate' (update aggregate update-agg-fn event)]
                (assoc-agg-fn state aggregate')))]
      (let [agg-keys (get-agg-keys event)
            agg-keys (if (sequential? agg-keys)
                       agg-keys
                       [agg-keys])]
        (log/warn :agg-keys agg-keys)
        (reduce state f agg-keys)))))

(defmulti user (fn [_ event] (:event/type event)))

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
  (let [to         (users/event->username event :to)
        from       (users/event->username event :from)
        username   (:username user)
        target     (if (= to username)
                     to
                     from)
        field      (if (= to username)
                     :msgs/received
                     :msgs/sent)]
    (users/assoc-user target
                      (update user
                              field
                              (fnil inc 0)))))

(letfn [(get-agg-fn [state agg-key]
          (get-in state [:users/by-username (:username agg-key)]))
        (get-agg-keys [event]
          (let [result
                (if (= :msg/Sent (:event/type event))
                  [{:username (get-in event [:event/data :to])}
                   {:username (get-in event [:event/data :from])}]
                  {:username (get-in event [:event/data :username])})]
            ;; (log/warn :get-agg-keys {:result result :event event})
            result))
        (assoc-agg-fn [state agg-key agg]
          (assoc-in state [:users/by-username (:username agg-key)] agg))
        (update-agg-fn [agg event]
          (let [result
                (user agg event)]
            (log/warn :update-agg-fn {:agg agg :event event :result result})
            result))]
  (def project-user-aggregate
    (aggregate-projecton get-agg-fn
                         get-agg-keys
                         assoc-agg-fn
                         update-agg-fn)))
