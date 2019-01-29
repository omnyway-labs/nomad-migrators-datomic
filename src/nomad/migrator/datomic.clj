(ns nomad.migrator.datomic
  (:require
   [clojure.string :as str]
   [nomad.core :as nomad :refer [defmigration]]
   [nomad.migrator.datomic.api :as d]))

(defrecord DatomicStore [db-map])

(defn connect [db-map]
  (DatomicStore. db-map))

(def nomad-schema
  [{:db/ident       :nomad/tag
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Migration tag"}
   {:db/ident       :nomad/applied
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "Timestamp when migration was applied"}])

(defn call-with-connection [{:keys [db-map]} f]
  (let [conn (d/connect* db-map)]
    (f conn)))

(defn call-with-db [store f]
  (call-with-connection
   store
   (fn [conn]
     (let [db (d/db conn)]
       (f db)))))

(defn init [store]
  (call-with-connection
   store
   (fn [conn]
     (d/transact* conn {:tx-data nomad-schema}))))

(defn throw-not-implemented [f]
  (throw (ex-info "Not implemented" {:fn f})))

;; FIXME: add IMigrator protocol fn for list-all-tables
(defn list-all-tables [{:keys [db-map]}]
  (throw-not-implemented 'list-all-tables))

;; FIXME: add IMigrator protocol fn for drop-all-tables
(defn drop-all-tables [{:keys [db-map]}]
  (throw-not-implemented 'drop-all-tables))

(defn load-migrations [store]
  (call-with-connection
   store
   (fn [conn]
     (d/q* conn
           {:query '[:find ?e ?tag ?applied
                     :where
                     [?e :nomad/tag ?tag]
                     [?e :nomad/applied ?applied]]}))))

(defn applied? [store tag]
  (call-with-connection
   store
   (fn [conn]
     (d/q* conn
           {:query '[:find ?e ?tag ?applied
                     :in $ ?tag
                     :where
                     [?e :nomad/tag ?tag]
                     [?e :nomad/applied ?applied]]
            :args [(d/db conn) tag]}))))

(def ^:dynamic *connection* nil)

(defn current-connection []
  *connection*)

(defn apply! [store tag migration-fn]
  (call-with-connection
   store
   (fn [conn]
     (binding [*connection* conn]
       (migration-fn)
       (d/transact* conn
                    {:tx-data [{:nomad/tag (name tag)
                                :nomad/applied (java.util.Date.)}]})))))

(defn fini [_])

(extend DatomicStore
  nomad/IMigrator
  {:-init init
   :-fini fini
   :-load-migrations load-migrations
   :-applied? applied?
   :-apply! apply!})
