(ns shadows.ids
  #?(:cljs
     (:require [cljs-uuid-utils.core :as uuid])))

(defn gen []
  #?(:clj
     (java.util.UUID/randomUUID)

     :cljs
     (uuid/uuid-string
      (uuid/make-random-uuid))))
