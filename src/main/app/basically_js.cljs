(ns app.basically-js)

;; called via :init-fn in build config

(defn init []
  (let [root (js/document.getElementById "root")
        board-size (js/parseInt (.getAttribute root "data-size") 10)
        sid (.getAttribute root "data-sid")
        total (* board-size board-size)
        state (-> (js/Array. total) (.fill "dead"))
        cells (.map state
                (fn [_ idx]
                  (doto (js/document.createElement "div")
                    (set! -className "tile")
                    (.addEventListener "click"
                      (fn [e]
                        ;; don't care about result for now, updates come from sse
                        (js/fetch (str "/hit?sid=" sid "&idx=" idx)))))))]

    (.forEach cells #(.append root %))

    ;; can't be bothered to swap this to the azure thing, but that would be trivial
    (let [sse (js/EventSource. (str "/connect?sid=" sid))]
      (.addEventListener sse "game-update"
        (fn [e]
          (let [data (.-data e)
                ;; who needs JSON right?
                arr (.split data " ")]

            (.forEach cells
              (fn [cell idx]
                (let [oval (aget state idx)
                      nval (aget arr idx)]

                  (when-not (identical? oval nval)
                    (unchecked-set (.-style cell) "background-color" (if (identical? nval "dead") "white" nval))
                    (aset state idx nval)))))))))))