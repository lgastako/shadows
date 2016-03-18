(ns shadows.play.users.mm
  (:require [its.log :as log]
            [shadows.core :as shadows :refer [from merge-from]]
            [shadows.play.users :as users]
            [shadows.prelude :refer [not-implemented!]]))

(defmulti users (fn [_ event] (:event/type event)))

(defmethod users :default
  [state event]
  state)

(defmethod users :user/Registered
  [state event]
  (users/update<-event state event
                       [:username
                        :bcrypt-pwhash]))

(defmethod users :user/ChangedPassword
  [state event]
  (users/update<-event state event [:bcrypt-pwhash]))

(defmethod users :msg/Sent
  [state event]
  (let [to         (users/event->username event :to)
        from       (users/event->username event :from)
        to-user    (users/username->user state to)
        from-user  (users/username->user state from)]
    (-> state
        (users/assoc-user to
                          (update to-user
                                  :msgs/received
                                  (fnil inc 0)))
        (users/assoc-user from
                          (update from-user
                                  :msgs/sent
                                  (fnil inc 0))))))

(defmulti mailboxes (fn [_ event] (:event/type event)))

(defmethod mailboxes :default
  [state event]
  state)

(defmethod mailboxes :msg/Sent
  [state event]
  (let [msg  (from (:event/data event) [:to :from :subject :body])
        to   (:to   msg)
        from (:from msg)]
    (-> state
        (users/add-to-box to   :inbox  msg)
        (users/add-to-box from :outbox msg))))
