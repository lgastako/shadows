(ns shadows.projections-test
  (:require #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test    :refer-macros [are deftest is testing]])
            [its.log :as log]
            [shadows.core :as shadows]))

(def test-events
  [{:event/id 1
    :event/type :Foo}
   {:event/id 2
    :event/type :Bar}
   {:event/id 3
    :event/type :Baz}])

(def username            "john")
(def bcrypt-password     "secret")
(def new-bcrypt-password "secret2")
(def test-user-events
  [{:event/id 1
    :event/type :UserRegistered
    :event/data {:username username
                 :bcrypt-password bcrypt-password}}
   {:event/id 2
    :event/type :UserPostedMessage
    :event/data {:username username
                 :forum-id 1
                 :title "Who's new?"
                 :body "This guy."}}
   {:event/id 3
    :event/type :UserChangedPassword
    :event/data {:username username
                 :new-bcrypt-password new-bcrypt-password}}])

(defn counter [state event]
  (inc (or state 0)))

(def empty-user {})

(defn user-projection [state event]
  (letfn [(count-users [state]
            (assoc state :total-users (count (keys (:users/by-username state)))))]
    (let [username    (get-in event [:event/data :username])
          user        (get-in state [:users/by-username username] empty-user)]
      (-> state
          (assoc-in [:users/by-username username]
                    (case (:event/type event)
                      :UserRegistered
                      (merge user (from event [:username :bcrypt-password]))
                      :UserChangedPassword
                      (merge user (from event [{:new-bcrypt-password :bcrypt-password}]))
                      :UserPostedMessage
                      (update user :num-posts (fnil inc 0))
                      user))
          count-users))))

(deftest test-one

  (testing "a basic counting projection"
    (is (= 3
           (reduce counter 0 test-events)))
    (is (= 9
           (reduce counter 0 (concat test-events
                                     test-events
                                     test-events))))))

(deftest test-user-example
  (is (= {:total-users 1
          :users/by-username
          {"john" {:username "john"
                   :bcrypt-password new-bcrypt-password
                   :num-posts 1}}}
         (reduce user-projection {} test-user-events))))

(test-user-example)
;; (run-tests)
