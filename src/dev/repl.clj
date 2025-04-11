(ns repl
  (:require
    [app.main :as app]
    [org.httpkit.server :as hk-server]
    [shadow.cljs.devtools.api :as shadow]
    [shadow.cljs.devtools.server :as srv]
    ))

(defonce http-ref (atom nil))
(defonce app-ref (atom nil))

(defn start [_]
  (srv/start!)

  ;; (shadow/watch :app)
  ;; (shadow/watch :grove-app)

  (reset! app-ref (app/start))

  (reset! http-ref
    (hk-server/run-server
      #(app/handler (assoc % :dev true :state-ref (:state-ref @app-ref)))
      {:port 8080}))

  ::started)

(defn stop []
  (when-some [srv @http-ref]
    (reset! http-ref nil)
    (srv))

  (when-some [app @app-ref]
    (app/stop app)
    (reset! app-ref nil))
  ::stopped)

(defn go []
  (stop)
  (start nil))