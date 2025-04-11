(ns app.grove
  (:require [shadow.grove :as sg :refer (<<)]))

(def root (js/document.getElementById "root"))
(def sid (.getAttribute root "data-sid"))

(defn ui-tile [color idx]
  (<< [:div {:class "tile"
             :style/background-color (if (identical? color "dead") "white" color)
             :on-click (fn [e]
                         (js/fetch (str "/hit?sid=" sid "&idx=" idx)))}]))

(def rt-ref (sg/get-runtime :app))

(defn init []
  ;; can't be bothered to swap this to the azure thing, but that would be trivial
  (let [sse (js/EventSource. (str "/connect?sid=" sid))]

    (.addEventListener sse "game-update"
      (fn [e]
        (let [data (.-data e)
              ;; who needs JSON right?
              arr (vec (.split data " "))]

          (sg/render rt-ref root
            (sg/simple-seq arr ui-tile)))))))
