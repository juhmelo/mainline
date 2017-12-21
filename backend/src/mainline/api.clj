(ns mainline.api
  (:require [io.sarnowski.swagger1st.core :as s1st]
            [io.sarnowski.swagger1st.executor :as s1stexec]
            [mainline.authenticator :as authenticator]
            [mainline.db :as db]
            [mainline.lib.http :as httplib]
            [mount.core :as m]))

(defn get-messages [{:keys [limit offset]} req]
  (let [messages (db/get-messages {:limit  limit
                                   :offset (or offset 0)})]
    {:status 200 :body {:messages messages}}))

(defn post-messages [{:keys [message]} req]
  (let [created-message (db/create-message! {:body (:body message)})]
    {:status 200 :body created-message}))

(defn resolve-operation
  "Calls operationId function with flattened request params and raw request map."
  [request-definition]
  (when-let [operation-fn (s1stexec/operationId-to-function request-definition)]
    (fn [request]
      (operation-fn (apply merge (vals (:parameters request))) request))))

(m/defstate handler
  :start (-> (s1st/context :yaml-cp "api.yaml")
             (s1st/discoverer :definition-path "/api/swagger.json" :ui-path "/api/ui/")
             ;; Given a path, figures out the spec part describing it
             (s1st/mapper)
             (s1st/ring authenticator/wrap-reason-logger)
             ;; Enforces security according to the requirements per endpoint, depends on the mapper
             (s1st/protector {"oauth2" @authenticator/oauth2-s1st-security-handler})
             ;; Extracts parameter values from path, query and body of the request
             (s1st/parser)
             ;; Now we also know the user, replace request info
             (s1st/ring httplib/wrap-request-log-context)
             ;; Calls the handler function for the request. Customizable through :resolver
             (s1st/executor :resolver resolve-operation)))
