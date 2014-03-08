(ns clidget-sample.cljs.app
  (:require [clojure.string :as s]
            [cljs.core.async :as a]
            [dommy.core :as d]
            clojure.browser.repl
            [clidget.widget :refer [defwidget] :include-macros true])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(defwidget counter-widget [{:keys [counter]} events-ch]
  (node
   [:div
    [:h2 "The counter is: " counter]
    [:p
     (doto (node [:button.btn "Increment counter"])
       (d/listen! :click #(a/put! events-ch :inc)))]
    [:p
     (doto (node [:button.btn "Decrement counter"])
       (d/listen! :click #(a/put! events-ch :dec)))]]))

(defn watch-events! [events-ch !counter]
  (go-loop []
    (when-let [event (a/<! events-ch)]
      (case event
        :inc (swap! !counter inc)
        :dec (swap! !counter dec))
      (recur))))

(set! js/window.onload
      (fn []
        (let [!counter (atom 0)
              events-ch (doto (a/chan)
                          (watch-events! !counter))]

          (d/replace-contents! js/document.body
                               (node [:div.container
                                      [:h2 {:style {:margin-top "1em"}}
                                       (counter-widget {:!counter !counter} events-ch)]])))))


