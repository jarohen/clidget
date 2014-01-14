(ns clidget-sample.cljs.app
  (:require [clojure.string :as s]
            [cljs.core.async :as a]
            [dommy.core :as d]
            clojure.browser.repl)
  (:require-macros [dommy.macros :refer [node sel1]]
                   [clidget.widget :refer [defwidget defwatcher]]
                   [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(defwidget counter-widget [{:keys [counter1 counter2]} events-ch]
  (node
   [:div
    [:h2 "counters are now: " (pr-str [counter1 counter2])]
    [:p
     (doto (node [:button.btn "Inc counter1"])
       (d/listen! :click #(a/put! events-ch :inc-1)))]
    [:p
     (doto (node [:button.btn "Dec counter2"])
       (d/listen! :click #(a/put! events-ch :dec-2)))]]))

(defwidget diff-widget [{:keys [counter1 counter2]}]
  (node
   [:p "The difference is: " (js/Math.abs (apply - [counter1 counter2]))]))

(defwatcher console-watcher [{:keys [counter1 counter2]}]
  (println "Counters:" [counter1 counter2]))

(defn watch-events! [events-ch !counter1 !counter2]
  (go-loop []
    (when-let [event (a/<! events-ch)]
      (case event
        :inc-1 (swap! !counter1 inc)
        :dec-2 (swap! !counter2 dec))
      (recur))))

(set! (.-onload js/window)
      (fn []
        (let [!counter1 (atom 0)
              !counter2 (atom 999)
              events-ch (doto (a/chan)
                          (watch-events! !counter1 !counter2))]

          (console-watcher {:counter1 !counter1
                            :counter2 !counter2})
          
          (d/replace-contents! (sel1 :#content)
                               (node [:div.container
                                      [:h2 {:style {:margin-top "1em"}}
                                       (counter-widget {:counter1 !counter1
                                                        :counter2 !counter2}
                                                       events-ch)]
                                      [:div {:style {:margin-top "2em"}}
                                       (diff-widget {:counter1 !counter1
                                                        :counter2 !counter2})]])))))


