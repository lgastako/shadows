(ns shadows.play.users.fn
  (:require [its.log :as log]
            [shadows.core :as shadows :refer [from merge-from]]
            [shadows.play.users :as users]
            [shadows.prelude :refer [not-implemented!]]))

(defn- project-users-sent [state event]
  (log/debug ::project-users-sent)
  (let [to        (users/event->username event :to)
        from      (users/event->username event :from)
        to-user   (users/username->user state to)
        from-user (users/username->user state from)]
    (-> state
        (users/assoc-user to
                          (update to-user
                                  :msgs/received (fnil inc 0)))
        (users/assoc-user from
                          (update from-user
                                  :msgs/sent (fnil inc 0))))))

(defn- project-users-general [state event]
  (log/debug ::project-users-general)
  (let [username (users/event->username event)
        user     (users/username->user state username)
        data     (:event/data event)]
    (users/assoc-user state username
                      (case (:event/type event)
                        :user/Registered      (merge-from user data [:username :bcrypt-pwhash])
                        :user/ChangedPassword (merge-from user data [:bcrypt-pwhash])
                        user))))

(defn project-users [state event]
  (log/debug ::project-user {:state state :event event})
  (let [f (if (= :msg/Sent (:event/type event))
            project-users-sent
            project-users-general)]
    (f state event)))

(defn project-mailboxes [state event]
  (log/debug ::project-mailboxes {:state state :event event})
  (if-not (= :msg/Sent (:event/type event))
    state
    (let [msg  (from (:event/data event) [:to :from :subject :body])
          to   (:to   msg)
          from (:from msg)]
      (-> state
          (users/add-to-box to   :inbox  msg)
          (users/add-to-box from :outbox msg)))))
