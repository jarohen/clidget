(ns todomvc.cljs.app
  (:require [dommy.core :as d]
            [cljs.core.async :as a]
            [clojure.string :as s]
            [goog.events.KeyCodes :as kc])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go-loop]]
                   [clidget.widget :refer [defwidget]]))

(enable-console-print!)

(defwidget toggle-all-widget [{:keys [todos]} events-ch]
  (let [all-done? (every? (comp :done? val) todos)]
    (doto (node [:input#toggle-all {:type "checkbox"
                                    :checked all-done?}])
      (d/listen! :change #(a/put! events-ch {:set-all? (not all-done?)})))))

(defn on-enter [el f]
  (d/listen! el :keyup
    (fn [e]
      (when (= kc/ENTER (.-keyCode e))
        (f e)
        (.preventDefault e)))))

(defn edit-input [{:keys [caption id]} !editing? events-ch]
  (let [input (node [:input.edit {:value caption,
                                  :autofocus true
                                  :type "text"}])]
    (doto input
      (on-enter (fn [e]
                  (a/put! events-ch {:new-caption (d/value input), :updated-id id})
                  (reset! !editing? false))))))

(defwidget todo-item-widget [{:keys [editing? !editing?]
                              :locals {:!editing? (atom false)}}
                             {:keys [caption done? id] :as todo}
                             events-ch]
  (prn "rendering item" id)
  (node
   [:li ^:attrs {:class (s/join " " [(when done? "completed")
                                     (when editing? "editing")])}
    (if-not editing?
      [:div.view
       (doto (node [:input.toggle {:type "checkbox", :checked done?}])
         (d/listen! :change #(a/put! events-ch {:toggled-id id})))
       (doto (node [:label caption])
         (d/listen! :dblclick #(reset! !editing? true)))
       (doto (node [:button.destroy])
         (d/listen! :click #(a/put! events-ch {:clear-id id})))]
      
      (edit-input todo !editing? events-ch))]))

(defwidget new-todo-widget [{} events-ch]
  (let [input (node [:input#new-todo {:placeholder "What needs to be done?" :type "text"}])]
    (doto input
      (on-enter #(do (a/put! events-ch {:new-todo (d/value input)})
                     (d/set-value! input nil))))))

(def filter-todos
  {:all identity
   :active (complement :done?)
   :completed :done?})
 
(defwidget todo-list-widget [{:keys [todos todo-filter]} events-ch]
  (node
   [:ul#todo-list
    (for [[id todo] (filter (comp (filter-todos todo-filter) val) todos)]
      (todo-item-widget {} (assoc todo :id id) events-ch))]))

(defwidget stats-widget [{:keys [todos]}]
  (node
   [:span#todo-count
    [:strong (count (remove :done? todos))]
    [:span " items left"]]))

(def filter-label
  {:all "All"
   :active "Active"
   :completed "Completed"})

(defwidget filters-widget [{:keys [todo-filter !todo-filter]}]
  (node
   [:ul#filters
    (for [filter-option [:all :active :completed]]
      (node
       [:li {:style {:cursor "pointer"}}
        (doto (node [:a ^:attrs (when (= todo-filter filter-option)
                                  {:class "selected"})
                     (filter-label filter-option)])
          (d/listen! :click #(reset! !todo-filter filter-option)))]))]))

(defwidget clear-completed-widget [{:keys [todos]} events-ch]
  (node
   [:div
    (let [completed-count (count (filter :done? (vals todos)))]
      (when-not (zero? completed-count)
        (doto (node [:button#clear-completed
                     (str "Clear completed " completed-count)])
          (d/listen! :click #(a/put! events-ch {:clear-completed? true})))))]))

(defn watch-events! [events-ch !todos]
  (go-loop []
    (when-let [{:keys [toggled-id
                       updated-id new-caption
                       clear-id
                       new-todo
                       clear-completed?
                       set-all?] :as event} (a/<! events-ch)]
      (cond
       toggled-id (swap! !todos update-in [toggled-id :done?] not)
       updated-id (swap! !todos assoc-in [updated-id :caption] new-caption)
       new-todo (swap! !todos #(assoc %
                                 ((fnil inc 0) (apply max (keys %)))
                                 {:caption new-todo, :done? false}))
       clear-completed? (swap! !todos #(into {} (remove (comp :done? val) %)))
       clear-id (swap! !todos dissoc clear-id)
       (not (nil? set-all?)) (swap! !todos (fn [todos]
                                             (->> todos
                                                  (map #(assoc-in % [1 :done?] set-all?))
                                                  (into {})))))
      
      (recur))))

(defn test-todos []
  (->> (for [x (range 5)]
         [x {:caption (str "Test todo " x)}])
       (into {})))

(set! (.-onload js/window)
      (fn []
        (let [!todos (atom (test-todos))
              !todo-filter (atom :all)
              events-ch (doto (a/chan)
                          (watch-events! !todos))]

          (d/replace-contents! (.-body js/document)
                               (node [:section#todoapp
                                      [:header#header
                                       [:h1 "todos"]
                                       (new-todo-widget {} events-ch)]
                                      [:section#main
                                       (toggle-all-widget {:!todos !todos} events-ch)
                                       [:label {:for "toggle-all"} "Mark all as complete"]

                                       (todo-list-widget {:!todos !todos
                                                          :!todo-filter !todo-filter}
                                                         events-ch)]
                                      [:footer#info
                                       [:p "Double-click to edit a todo"]]
                                      [:footer#footer
                                       (stats-widget {:!todos !todos})
                                       (filters-widget {:!todo-filter !todo-filter})
                                       (clear-completed-widget {:!todos !todos} events-ch)]])))))
