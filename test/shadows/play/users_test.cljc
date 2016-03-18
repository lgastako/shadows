(ns shadows.play.users-test
  (:require #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test    :refer-macros [are deftest is testing]])
            [its.log :as log]
            [shadows.play.users :as users]
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

(def msg-1-event
  {:event/id (ids/gen)
   :event/type :msg/Sent
   :event/data {:to "john"
                :from "bill"}})

(def msg-2-event
  {:event/id (ids/gen)
   :event/type :msg/Sent
   :event/data {:to "bill"
                :from "john"}})

(def msg-3-event
  {:event/id (ids/gen)
   :event/type :msg/Sent
   :event/data {:to "john"
                :from "bill"}}  )

(def events
  [create-john-event
   change-password-1-event
   msg-1-event
   change-password-2-event
   msg-2-event
   msg-3-event])

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

(def expected-users
  {:users/by-username {"john" {:username "john"
                               :bcrypt-pwhash "secret2"
                               :msgs/received 2
                               :msgs/sent 1}
                       "bill" {:msgs/sent 2
                               :msgs/received 1}}})

(deftest test-users-mm
  (let [state (reduce users/users {} events)]
    ;; (log/warn ::test-users-mm {:state state})
    (is (= expected-users
           state))))

(deftest test-project-user-factory
  (let [state (reduce users/project-users {} events)]
    ;; (log/warn ::test-project-user-factory {:state state})
    (is (= expected-users
           state))))

;; (log/set-level! :warn)
;; (log/set-level! :debug)

;; (test-project-user-factory)
(run-tests)
