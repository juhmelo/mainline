(ns mainline.core
  (:gen-class)
  (:require [environ.core :as environ]
            [mainline.lib.logging :as log]
            [mainline.nrepl :as nrepl]
            [mount.core :as m]
            [google-apps-clj.credentials :as c]))

;; HINT: After adding a new defstate restart the REPL

;; Make states explicitly derefable: @server, @*db* @env
(m/in-cljc-mode)

(defn implementation-version []
  (or
    ;; When running in a REPL
    (System/getProperty "mainline.version")
    ;; When running as `java -jar ...`
    (-> (eval 'mainline.core) .getPackage .getImplementationVersion)))

(defn -main [& args]
  (log/disable-console-logging-colors)
  (log/set-level! :info)
  (log/set-log-level-from-env! (System/getenv "LOG_LEVEL"))
  (log/info "Starting mainline version %s" (implementation-version))
  (try
    (when (= "true" (System/getenv "NREPL_ENABLED"))
      (nrepl/start-nrepl environ/env))
    (m/start)
    (log/info "Application started")
    ;; Prevent -main from exiting to keep the application running, unless it's a special test run
    (if-let [test-timeout (System/getenv "TEST_TIMEOUT")]
      (do
        (log/warn "Test mode: terminating after %s ms" test-timeout)
        (Thread/sleep (bigint test-timeout))
        (System/exit 0))
      @(promise))
    (catch Exception e
      (log/error e "Could not start the application because of %s." (str e))
      (System/exit 1))))

(log/set-ns-log-levels!
  {"mainline.*" :debug
   :all :info})

(log/set-default-output-fn!)

(comment
  ;; Starting and stopping the application during development and NREPL access
  (m/start)
  (m/stop)
  ;; Override some environment variables
  (m/start-with-args {:http-port 8888})
  )
