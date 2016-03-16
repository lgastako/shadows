(ns shadows.core-test
  (:require #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test    :refer-macros [are deftest is testing]])
            [its.log :as log]
            [shadows.core :as shadows]))

(def select-with-rename #'shadows/select-with-rename)

(deftest test-select-with-rename

  (testing "basics"
    (is (= {:c 1 :d 2}
           (select-with-rename {:a 1 :b 2}
                               [{:a :c}
                                {:b :d}])))))

(deftest test-from

  (testing "select-keys"

    (are [m ks] (= (select-keys m ks) (shadows/from m ks))
      {:a 1 :b 2} [:a]
      {:a 1 :b 2} [:b]
      {:a 1 :b 2} [:a :b]
      {:a 1 :b 2} [:a :b :c]))

  (testing "mappings"

    (are [m m' maps] (= m (shadows/from m' maps))
      {:b 1}        {:a 1} [{:a :b}]
      {:a 1}        {:b 1} [{:b :a}]
      {:a nil :d 1} {:c 1} [{:a :b :c :d}]
      {:d nil}      {:a 1} [{:c :d}]))

  (testing "mixed"))

;; (log/set-level! :warn)
;; (log/set-level! :debug)

;; (test-from)
(run-tests)
