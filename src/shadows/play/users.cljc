(ns shadows.play.users
  (:require [its.log :as log]
            [shadows.core :as shadows :refer [from merge-from]]
            [shadows.prelude :refer [not-implemented!]]))

;; Issues... need a way to parameterize over indivudal users ... we could have
;; factory functions that take opts and produce the reduction functions but
;; then we can't use multimethods.  do we care about multimethods?

;; These are implemented assuming they are already attached to a stream that
;; only has the events for the right username... which leaves them drastically
;; underpowered compared to the project-user version below.

;; Could go with opts as a third arg but then you're breaking the reduction
;; cleanliness, etc.

(defn event->username
  ([event]
   (event->username event :username))
  ([event field]
   (let [result (get-in event [:event/data field])]
     (log/debug :event->username {:event event :field field :result result})
     result)))

(defn username->user
  ([state username]
   (username->user state username nil))
  ([state username default]
   (log/debug ::username->user {:username username})
   (get-in state [:users/by-username username] default)))

(defn event->user [event]
  (-> event
      event->user
      get-user))

(defn assoc-user [state username user]
  (assoc-in state [:users/by-username username] user))

(defmulti users (fn [_ event] (:event/type event)))

(defn update-user-by-username [state event fields]
  (let [username (event->username event)
        user     (username->user state username)]
    (assoc-user state username
                (merge-from user (:event/data event) fields))))

(defmethod users :user/Registered
  [state event]
  (update-user-by-username state event
                           [:username
                            :bcrypt-pwhash]))

(defmethod users :user/ChangedPassword
  [state event]
  (update-user-by-username state event [:bcrypt-pwhash]))

(defmethod users :msg/Sent
  [state event]
  (let [to         (event->username event :to)
        from       (event->username event :from)
        to-user    (username->user state to)
        from-user  (username->user state from)]
    (-> state
        (assoc-user to
                    (update to-user
                            :msgs/received
                            (fnil inc 0)))
        (assoc-user from
                    (update from-user
                            :msgs/sent
                            (fnil inc 0))))))

;; Ok, lets try the same thing but with factory fns

(defn project-users [state event]
  (log/debug ::project-user {:state state :event event})

  (if (= :msg/Sent (:event/type event))
    (let [to        (event->username event :to)
          from      (event->username event :from)
          to-user   (username->user state to)
          from-user (username->user state from)]
      (-> state
          (assoc-user to
                      (update to-user
                              :msgs/received (fnil inc 0)))
          (assoc-user from
                      (update from-user
                              :msgs/sent (fnil inc 0)))))
    (let [username (event->username event)
          user     (username->user state username)
          data     (:event/data event)]
      (assoc-user state username
                  (case (:event/type event)
                    :user/Registered      (merge-from user data [:username :bcrypt-pwhash])
                    :user/ChangedPassword (merge-from user data [:bcrypt-pwhash])
                    user)))))
