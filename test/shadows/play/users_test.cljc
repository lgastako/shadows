(ns shadows.play.users-test
  (:require #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test    :refer-macros [are deftest is testing]])
            [its.log :as log]
            [shadows.play.users :as users]
            [shadows.play.users.fn :as fn-users]
            [shadows.play.users.mm :as mm-users]
            [shadows.play.users.pa :as pa-users]
            [shadows.ids :as ids]))

(def create-john-event
  {:event/id (ids/gen)
   :event/type :user/Registered
   :event/data {:username "john"}})

(def change-password-1-event
  {:event/id (ids/gen)
   :event/type :user/ChangedPassword
   :event/data {:username "john"
                :bcrypt-pwhash "secret"}})

(def change-password-2-event
  {:event/id (ids/gen)
   :event/type :user/ChangedPassword
   :event/data {:username "john"
                :bcrypt-pwhash "secret2"}})

(def msg-1
  {:to "john"
   :from "bill"
   :subject "yo"
   :body "what it do, kid?"})

(def msg-1-event
  {:event/id (ids/gen)
   :event/type :msg/Sent
   :event/data msg-1})

(def msg-2
  {:to "bill"
   :from "john"
   :subject "re: yo"
   :body "no much, what's good, fam?"})

(def msg-2-event
  {:event/id (ids/gen)
   :event/type :msg/Sent
   :event/data msg-2})

(def msg-3
  {:to "john"
   :from "bill"
   :subject "re: re: yo"
   :body "..."})

(def msg-3-event
  {:event/id (ids/gen)
   :event/type :msg/Sent
   :event/data msg-3})

(def events
  [create-john-event
   change-password-1-event
   msg-1-event
   change-password-2-event
   msg-2-event
   msg-3-event])

(def expected-users
  {:users/by-username {"john" {:username "john"
                               :bcrypt-pwhash "secret2"
                               :msgs/received 2
                               :msgs/sent 1}
                       "bill" {:msgs/sent 2
                               :msgs/received 1}}})

(def expected-mailboxes
  {:mailboxes/by-username {"john" {:inbox [msg-1
                                           msg-3]
                                   :outbox [msg-2]}
                           "bill" {:inbox [msg-2]
                                   :outbox [msg-1
                                            msg-3]}}})

(deftest test-event->username

  (testing "1-arity version"

    (testing "defaults field to `:username`."
      (is (= "john"
             (users/event->username create-john-event))))

    (testing "returns nil if no username field"
      (is (= nil
             (users/event->username msg-1-event)))))

  (testing "2-arity version"

    (testing "lets you choose field"
      (is (= "john"
             (users/event->username msg-1-event :to)))
      (is (= "bill"
             (users/event->username msg-1-event :from))))

    (testing "returns nil if field is missing"
      (is (= nil
             (users/event->username create-john-event :zorp))))))


;; XXX
;; The problem? with both of the following approaches in that they build up
;; (potentially) huge user and mailboxes tables per projection instead of
;; individual per-instance projections/aggregates.

(deftest test-users-mm
  (let [state (reduce mm-users/users {} events)]
    ;; (log/warn ::test-users-mm {:state state})
    (is (= expected-users
           state))))

(deftest test-project-user-fn
  (let [state (reduce fn-users/project-users {} events)]
    ;; (log/warn ::test-project-user-factory {:state state})
    (is (= expected-users
           state))))

(deftest test-users-pa
  (let [state (reduce pa-users/project-user-aggregate {} events)]
    (is (= expected-users
           state))))

(deftest test-mailboxes-mm
  (let [state (reduce mm-users/mailboxes {} events)]
    (is (= expected-mailboxes
           state))))

(deftest test-project-mailboxes-fn
  (let [state (reduce fn-users/project-mailboxes {} events)]
    (is (= expected-mailboxes
           state))))

;; (log/set-level! :warn)
;; (log/set-level! :debug)

;; (test-project-user-factory)
(run-tests)
