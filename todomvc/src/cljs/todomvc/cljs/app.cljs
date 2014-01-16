(ns todomvc.cljs.app
  (:require [clojure.string :as s]
            [dommy.core :as d]
            [cljs.core.async :as a]
            clojure.browser.repl)
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go-loop]]
                   [clidget.widget :refer [defwidget defwatcher]]))

(enable-console-print!)

(defwidget todo-item-widget [{} {:keys [caption done? id] :as todo} events-ch]
  (node
   [:tr ^:attrs (cond-> {}
                  done? (assoc :class "todo-done"))
    (doto (node [:td.check
                 [:img {:src "/img/tick.png"}]])
      (d/listen! :click #(a/put! events-ch {:toggled id})))
    [:td.caption caption]]))

(defwidget todo-list-widget [{:keys [todos]
                              :as system} events-ch]
  (node
   [:div.todos
    [:table.table.table-striped.table-hover
     (for [[id todo] (->> todos (sort-by key))]
       (todo-item-widget {} (assoc todo :id id) events-ch))]]))

(defn watch-events! [events-ch !todos]
  (go-loop []
    (when-let [{:keys [toggled]} (a/<! events-ch)]
      (swap! !todos update-in [toggled :done?] not)
      (recur))))

(set! (.-onload js/window)
      (fn []
        (let [!todos (atom {0 {:caption "test"}
                            1 {:caption "test 2"}})
              events-ch (doto (a/chan)
                          (watch-events! !todos))]

          (defn add-todo! []
            (swap! !todos assoc (rand-int 10000) {:caption (str "test " (rand-int 10000))}))
          
          (d/replace-contents! (.-body js/document)
                               (node [:div.container
                                      [:h3 "Things to do:"]
                                      [:div.row
                                       [:div.col-md-6
                                        [:div {:style {:margin-top "2em"}}
                                         (todo-list-widget {:!todos !todos} events-ch)]]]])))))
