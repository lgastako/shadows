(ns shadows.prelude
  (:require [its.log :as log]))

(defn not-implemented! [& args]
  (log/error :not-implemented {:args args})
  (throw (ex-info (str :not-implemented) {:args args})))
