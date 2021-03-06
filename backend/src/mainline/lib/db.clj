(ns mainline.lib.db
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [camel-snake-kebab.core :as csk]
            [migratus.core :as migratus])
  (:import org.postgresql.util.PGobject
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector
           clojure.lang.LazySeq
           java.sql.Array
           java.sql.BatchUpdateException
           java.sql.PreparedStatement))

;; Conversion when getting from JDBC
(extend-protocol jdbc/IResultSetReadColumn
  java.util.UUID
  (result-set-read-column [v _ _] (str v))
  Array
  (result-set-read-column [v _ _] (vec (.getArray v)))
  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json"   (json/parse-string value true)
        "jsonb"  (json/parse-string value true)
        "citext" (str value)
        value)))
  ;; Uncomment for clj-time
  ;Timestamp
  ;(result-set-read-column [val _ _]
  ;  (tc/from-sql-time val))
  )

(defn to-pg-json [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (json/generate-string value))))

;; Conversion when passing to JDBC
(extend-protocol jdbc/ISQLParameter
  IPersistentVector
  (set-parameter [v ^PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (to-pg-json v))))))

;; Conversion when passing to JDBC
(extend-protocol jdbc/ISQLValue
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  IPersistentVector
  (sql-value [value] (to-pg-json value))
  LazySeq
  (sql-value [value] (to-pg-json value))
  ; Uncomment for clj-time
  ;DateTime
  ;(sql-value [v]
  ;  (tc/to-sql-time v))
  )

;; ## This enables automatic case conversion to kebab-case so that no snake_case needs to be used in application code

(defn ->kebab-case-keyword-top-level [coll]
  (if (or (seq? coll) (map? coll))
    (into {} (for [[k v] coll]
               [(csk/->kebab-case-keyword k) v]))
    coll))

(defn result-one-snake->kebab [this result options]
  (->> (hugsql.adapter/result-one this result options)
       ->kebab-case-keyword-top-level))

(defn result-raw-snake->kebab [this result options]
  (->> (hugsql.adapter/result-raw this result options)
       ->kebab-case-keyword-top-level))

(defn result-many-snake->kebab [this result options]
  (->> (hugsql.adapter/result-many this result options)
       (map ->kebab-case-keyword-top-level)))

(defmethod hugsql.core/hugsql-result-fn :1 [sym] 'mainline.lib.db/result-one-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :one [sym] 'mainline.lib.db/result-one-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :* [sym] 'mainline.lib.db/result-many-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :many [sym] 'mainline.lib.db/result-many-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :raw [sym] 'mainline.lib.db/result-raw-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :default [sym] 'mainline.lib.db/result-raw-snake->kebab)
