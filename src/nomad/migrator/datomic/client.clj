(ns nomad.migrator.datomic.client
  (:require
   [clojure.core.async :refer [<!!]]
   [datomic.client :as d]
   [datomic.client.admin :as da]))

(def db d/db)

(def connect d/connect)

(defn connect* [arg-map]
  (let [arg-map (merge {:account-id d/PRO_ACCOUNT} arg-map)]
    (<!! (connect arg-map))))

(def transact d/transact)

(defn transact* [conn arg-map]
  (<!! (transact conn arg-map)))

(def q d/q)

(defn q* [conn arg-map]
  (<!! (q conn arg-map)))

(def create-database* da/create-database)
(def delete-database* da/delete-database)
