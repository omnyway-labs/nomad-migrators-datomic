(ns nomad.migrator.datomic.api
  (:require
   [datomic.api :as d]))

(def db d/db)

(def connect d/connect)

(defn make-uri [{:keys [db-name endpoint protocol]}]
  (str "datomic"
       ":" protocol
       "://" endpoint
       "/" db-name))

(defn connect* [arg-map]
  (connect (make-uri arg-map)))

(def transact d/transact)

(defn transact* [conn {:keys [tx-data]}]
  (deref (transact conn tx-data)))

(def q d/q)

(defn q* [conn {:keys [query args]}]
  (let [args (or (not-empty args) [(db conn)])]
    (apply q query args)))

(def create-database d/create-database)
(defn create-database* [arg-map]
  (create-database (make-uri arg-map)))

(def delete-database d/delete-database)
(defn delete-database* [arg-map]
  (delete-database (make-uri arg-map)))
