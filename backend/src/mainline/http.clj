(ns mainline.http
  (:require [mount.core :as m]
            [schema.core :as s]
            [aleph.http]
            [aleph.netty]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.middleware :refer [wrap-canonical-redirect]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params wrap-json-body]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.util.response :as resp]
            [manifold.deferred :as md]
            [squeeze.core :as squeeze]
            [mainline.lib.logging :as log]
            [mainline.lib.http :as httplib]
            [mainline.env :as env]
            [mainline.api :as api]))

(def config-defaults
  {:http-port 8090})

(s/defschema Config
  {(s/optional-key :http-port) s/Int})

(defn get-json [req]
  {:status 200 :body {:message "Hello"}})

(defn remove-trailing-slash
  "Remove the trailing '/' from a URI string, if it exists, unless the URI is just '/'"
  [^String uri]
  (if (= "/" uri)
    uri
    (compojure.middleware/remove-trailing-slash uri)))

(def api-defaults
  (-> ring.middleware.defaults/api-defaults
      (assoc-in [:security :hsts] true)))

;; Middleware rule of thumb: Request goes bottom to top, response goes top to bottom
(defn make-handler []
  (-> (routes
        ;; Swagger1st API implementation: https://github.com/zalando-stups/swagger1st
        (-> (routes
              (ANY "/api" req (resp/redirect "/api/ui/" 301))
              (ANY "/api/" req (resp/redirect "/api/ui/" 301))
              (ANY "/api/ui" req (resp/redirect "/api/ui/" 301))
              (ANY "/api/*" req (@api/handler req)))
            (wrap-json-response)
            (wrap-defaults api-defaults))
        (route/not-found nil))
      ;; It never hurts to gzip
      (wrap-gzip)
      (httplib/wrap-request-log-context)))

(m/defstate server
  :start (do
           (log/info "Starting HTTP server")
           (let [config         (squeeze/coerce-config Config (merge config-defaults @env/env))
                 started-server (aleph.http/start-server (make-handler) (squeeze/remove-key-prefix :http- config))]
             (log/info "HTTP server is listening on port %s" (aleph.netty/port started-server))
             started-server))
  :stop (.close @server))
