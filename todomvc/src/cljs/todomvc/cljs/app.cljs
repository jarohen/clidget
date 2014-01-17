(ns todomvc.cljs.app
  (:require [dommy.core :as d]
            [cljs.core.async :as a]
            [goog.events.KeyCodes :as kc])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go-loop]]
                   [clidget.widget :refer [defwidget]]))

(enable-console-print!)

(defn on-enter [el f]
  (d/listen! el :keyup
    (fn [e]
      (when (= kc/ENTER (.-keyCode e))
        (f e)
        (.preventDefault e)))))

(defn edit-input [{:keys [caption id]} !editing? events-ch]
  (let [input (node [:input.form-control {:value caption, :autofocus true}])]
    (doto input
      (on-enter (fn [e]
                  (a/put! events-ch {:new-caption (d/value input), :updated-id id})
                  (reset! !editing? false))))))

(defwidget todo-item-widget [{:keys [editing? !editing?]
                              :locals {:!editing? (atom false)}}
                             {:keys [caption done? id] :as todo}
                             events-ch]
  (prn "rendering item" id)
  (if-not editing?
    (node
     [:tr ^:attrs (cond-> {}
                    done? (assoc :class "todo-done"))
      (doto (node [:td.check
                   [:img {:src "/img/tick.png"}]])
        (d/listen! :click #(a/put! events-ch {:toggled-id id})))
      
      (doto (node [:td.caption caption])
        (d/listen! :click #(reset! !editing? true)))])
    (node
     [:tr [:td]
      [:td.caption (edit-input todo !editing? events-ch)]])))

(defwidget new-todo-widget [{} events-ch]
  (node
   [:tr [:td]
    [:td
     (let [input (node [:input.form-control {:placeholder "new todo"}])]
       (doto input
         (on-enter (fn [e]
                     (a/put! events-ch {:new-todo (d/value input)})
                     (d/set-value! input nil)))))]]))

(defwidget todo-list-widget [{:keys [todos]} events-ch]
  (prn "rendering list")
  (node
   [:div.todos
    [:table.table.table-striped.table-hover
     (concat (for [[id todo] (->> todos (sort-by key))]
               (todo-item-widget {} (assoc todo :id id) events-ch))
             [(new-todo-widget {} events-ch)])]]))

(defwidget todo-stats-widget [{:keys [todos]} events-ch]
  (prn "rendering stats")
  (let [todos (vals todos)
        completed (count (filter :done? todos))
        total (count todos)
        open (- total completed)]
    (node
     [:div
      [:p completed " completed, " open " open, " total " in total."]
      (when (pos? completed)
        [:p (doto (node [:button.btn.btn-warning "[clear completed]"])
              (d/listen! :click
                  (fn [e]
                    (a/put! events-ch {:clear-completed? true})
                    (.preventDefault e))))])])))

(defn watch-events! [events-ch !todos]
  (go-loop []
    (when-let [{:keys [toggled-id new-caption updated-id new-todo clear-completed?] :as event} (a/<! events-ch)]
      (cond
       toggled-id (swap! !todos update-in [toggled-id :done?] not)
       updated-id (swap! !todos assoc-in [updated-id :caption] new-caption)
       new-todo (swap! !todos #(assoc %
                                 ((fnil inc 0) (apply max (keys %)))
                                 {:caption new-todo, :done? false}))
       clear-completed? (swap! !todos #(into {} (remove (comp :done? val) %))))
      
      (recur))))

(set! (.-onload js/window)
      (fn []
        (let [!todos (atom {0 {:caption "test todo"}})
              events-ch (doto (a/chan)
                          (watch-events! !todos))]

          (d/replace-contents! (.-body js/document)
                               (node [:div.container
                                      [:h3 "Things to do:"]
                                      [:div {:style {:margin-top "2em"}}
                                       (todo-list-widget {:!todos !todos} events-ch)]
                                      [:div {:style {:margin-top "2em"}}
                                       (todo-stats-widget {:!todos !todos} events-ch)]])))))
