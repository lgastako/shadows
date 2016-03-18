(ns shadows.play.users
  (:require [its.log :as log]
            [shadows.core :as shadows :refer [from merge-from]]
            [shadows.prelude :refer [not-implemented!]]))

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
      event->username
      username->user))

(defn assoc-user [state username user]
  (assoc-in state [:users/by-username username] user))

(defn update<-event [state event fields]
  (let [username (event->username event)
        user     (username->user state username)]
    (assoc-user state username
                (merge-from user (:event/data event) fields))))

(defn add-to-box [state username box msg]
  (update-in state [:mailboxes/by-username username box] (fnil conj []) msg))
