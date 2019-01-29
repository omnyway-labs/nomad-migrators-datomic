(ns nomad.migrator.datomic-test
  (:require
   [clojure.test :refer :all]
   [nomad.core :as n]
   [nomad.migrator.datomic :as nmd]
   [nomad.migrator.datomic.api :as d]))

(def db-args
  {:db-name "test"
   :access-key "datomic"
   :secret "datomic"
   :region "none"
   :endpoint "localhost:4334"
   :service "peer-server"
   :protocol "mem"})

(def db (atom nil))

(defn fixture-setup []
  (try
    (d/delete-database* db-args)
    (catch Exception ex
      (prn ex)))
  (d/create-database* db-args))

(defn fixture [f]
  (try
    (fixture-setup)
    (reset! db (nmd/connect db-args))
    (f)
    (catch Exception ex
      (prn ex))))

(use-fixtures :each #'fixture)

(def test1-init-schema
  [{:db/ident       :test1/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "The name"}])

(def test1-age-schema
  [{:db/ident       :test1/age
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "The age"}])

(def test1-data
  [{:test1/name "foo"
    :test1/age 42}])

(deftest migrations
  (is (= {:index #{}, :clauses []}
         (n/clear-migrations!)))

  ;; initial-schema
  (is (= :ok
         (n/register-migration!
          "init-schema"
          {:up #(d/transact*
                 (nmd/current-connection)
                 {:tx-data test1-init-schema})})))

  ;; add age schema
  (is (= :ok
         (n/register-migration!
          "add-test1-age"
          {:up #(d/transact*
                 (nmd/current-connection)
                 {:tx-data test1-age-schema})})))
  (is (= :ok (n/migrate! @db)))

  (is (= #{"init-schema" "add-test1-age"}
         (->> (nmd/load-migrations @db)
              (map second)
              set)))

  (is (not-empty
       (nmd/call-with-connection
        @db
        #(-> (d/transact* % {:tx-data test1-data}) :tempids))))

  (is (= #{["foo" 42]}
         (nmd/call-with-connection
          @db
          (fn [conn]
            (d/q* conn
                  {:query '[:find ?name ?age
                            :where
                            [?e :test1/name ?name]
                            [?e :test1/age  ?age]]}))))))
