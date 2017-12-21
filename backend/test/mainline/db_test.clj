(ns mainline.db-test
  (:require [clojure.test :refer :all]
            [mount.core :as m]
            [clojure.java.jdbc :as jdbc]
            [mainline.db :refer :all]
            [mainline.env :as env]))

#_(defn wipe-db []
  (println "Wiping the DB")
  (jdbc/delete! @*db* :memories ["true"]))

#_(deftest test-memories
  (env/start-with-override {})
  (m/start #'*db*)
  (wipe-db)
  (is (= nil (get-memory {:id "1"})))
  (is (= 1 (create-memory! {:id "1" :memory-text "foo"})))
  ;; Column names are converted from camel_case to kebab-case
  (is (= {:id "1" :memory-text "foo"} (get-memory {:id "1"})))
  (is (= 1 (update-memory! {:id "1" :memory-text "bar"})))
  (is (= {:id "1" :memory-text "bar"} (get-memory {:id "1"})))
  (is (= 1 (delete-memory! {:id "1"})))
  (is (= nil (get-memory {:id "1"})))
  (m/stop))
