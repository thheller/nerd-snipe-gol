(ns app.main
  (:require
    [app.game :as game]
    [clojure.string :as str]
    [hiccup.core :refer (html)]
    [org.httpkit.server :as hk-server]
    [ring.middleware.file :as ring-file]
    [ring.middleware.params :as ring-params]
    [ring.middleware.file-info :as ring-file-info])
  (:import [java.util.concurrent Executors TimeUnit]))

(def page-404
  {:status 404
   :headers {"content-type" "text/plain"}
   :body "Not found."})

(defn page-home [{:keys [dev state-ref] :as req}]
  {:status 200
   :headers {"content-type" "text/html; charset=utf-8"}
   :body
   (html
     [:link {:rel "stylesheet" :type "text/css" :href "/css/main.css"}]
     ;; could just inline the script in prod mode
     [:script {:type "module" :src "/js/main.js"}]
     [:body
      [:main.main
       [:h1 "Game of Life (multiplayer)"]
       [:p "Built with ❤️ using "
        [:a {:href "https://clojure.org/"} "Clojure"]
        " and "
        [:a {:href "https://clojurescript.org/"} "ClojureScript"]
        "🚀"]
       [:p "Source code can be found "
        [:a {:href "https://github.com/andersmurphy/hyperlith/blob/master/examples/game_of_life/src/app/main.clj"} "here"]]
       [:div#root.board {:data-size (:board-size @state-ref) :data-sid (random-uuid)}]]])})

(defn game-tick [{:keys [board-size board] :as state}]
  (assoc state
    :board
    (game/next-gen-board
      {:board board
       :max-rows board-size
       :max-cols board-size})))

(defn sse-update [state-ref]
  (swap! state-ref game-tick)

  (let [{:keys [clients board] :as state} @state-ref]
    (when (seq clients)

      (let [data ;; who needs JSON or EDN for simple data like this
            (->> board
                 (map name)
                 (str/join " "))

            body
            (str "event: game-update\n"
                 "data: " data "\n\n")

            ;; can't be bothered to add compression, not focusing on the server side here
            msg
            {:status 200
             :headers
             {"Content-Type" "text/event-stream"
              "Cache-Control" "no-cache, no-store"}
             :body body}]

        (doseq [ch clients]
          (try
            (hk-server/send! ch msg false)
            (catch Exception e
              (prn [:client-ex e])
              (swap! state-ref update :clients disj ch)
              (hk-server/close ch)
              )))))))

(defn sse-start [{:keys [state-ref] :as req}]
  (hk-server/as-channel req
    {:on-open (fn [ch] (swap! state-ref update :clients conj ch))
     :on-close (fn [ch _] (swap! state-ref update :clients disj ch))}))

(defn fill-cell [board board-size color id]
  (if ;; crude overflow check
    (<= 0 id (dec (* board-size board-size)))
    (assoc board id color)
    board))

(defn fill-cross [{:keys [board-size colors] :as state} id sid]
  (let [user-color (get-in state [:sessions sid])]
    (update state :board
      (fn [board]
        (-> board
            (fill-cell board-size user-color (- id board-size))
            (fill-cell board-size user-color (- id 1))
            (fill-cell board-size user-color id)
            (fill-cell board-size user-color (+ id 1))
            (fill-cell board-size user-color (+ id board-size)))))))

(defn user-hit [{:keys [state-ref] :as req}]
  (let [{:strs [idx sid]} (:query-params req)]

    (swap! state-ref
      (fn [state]
        (-> state
            ;; FIXME: needs "proper" session handling and cleanup
            ;; irrelevant to the point of this example
            (cond->
              (not (get-in state [:sessions sid]))
              (assoc-in [:sessions sid] (rand-nth (:colors state))))
            (fill-cross (parse-long idx) sid))))

    {:status 201
     :body ""}))

(defn handler* [req]
  (case (:uri req)
    "/" (page-home req)
    "/connect" (sse-start req)
    "/hit" (user-hit req)
    page-404))

(def handler
  (-> handler*
      (ring-file/wrap-file "public")
      (ring-file-info/wrap-file-info)
      (ring-params/wrap-params)
      ))

(defn start []
  (let [board-size 50

        state-ref
        (atom {:board (game/empty-board board-size board-size)
               :colors [:red :blue :green :orange :fuchsia :purple]
               :board-size board-size
               :clients #{}
               :sessions {}})

        updater
        (doto (Executors/newSingleThreadScheduledExecutor)
          (.scheduleAtFixedRate ^Runnable #(sse-update state-ref) 0 200 TimeUnit/MILLISECONDS))]

    {:state-ref state-ref
     :updater updater}))

(defn stop [{:keys [state-ref updater] :as svc}]
  (.shutdownNow updater)

  (doseq [client (:clients @state-ref)]
    (hk-server/close client)))

(defn -main [& args]
  (let [app (start)]

    (hk-server/run-server
      (fn [req]
        ;; lazy way to inject state into the ring flow
        (handler (merge app req)))
      {:port 8080})

    ;; should have a clean shutdown option of some sort
    ;; not bothering with it for this example, this entry point is only used to show production mode
    ;; dev is started via (repl/start)
    (loop []
      (Thread/sleep 1000)
      (recur))

    ;; never gets here currently
    (stop app)))