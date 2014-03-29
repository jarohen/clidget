(ns todomvc.cljs.todomvc-widget
  (:require [dommy.core :as d]
            [cljs.core.async :as a]
            [clojure.string :as s]
            [goog.events.KeyCodes :as kc]
            [clidget.widget :refer-macros [defwidget]])
  (:require-macros [dommy.macros :refer [node sel1]]))

(defwidget toggle-all-widget [{:keys [todos]} events-ch]
  (let [all-done? (every? (comp :done? val) todos)]
    (doto (node [:input#toggle-all {:type "checkbox"
                                    :checked all-done?}])
      (d/listen! :change #(a/put! events-ch {:type :toggle-all
                                             :done? (not all-done?)})))))

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
                  (a/put! events-ch {:type :update
                                     :caption (d/value input)
                                     :updated-id id})
                  (reset! !editing? false))))))

(defwidget todo-item-widget [{:keys [editing? !editing? todo]
                              :locals {:!editing? (atom false)}}
                             events-ch]
  (let [{:keys [caption done? id]} todo]
    (node
     [:li ^:attrs {:class (s/join " " [(when done? "completed")
                                       (when editing? "editing")])}
      (if-not editing?
        [:div.view
         (doto (node [:input.toggle {:type "checkbox", :checked done?}])
           (d/listen! :change #(a/put! events-ch {:type :toggle
                                                  :toggled-id id})))
         (doto (node [:label caption])
           (d/listen! :dblclick #(reset! !editing? true)))
         (doto (node [:button.destroy])
           (d/listen! :click #(a/put! events-ch {:type :delete
                                                 :deleted-id id})))]
        
        (edit-input todo !editing? events-ch))])))

(defwidget new-todo-widget [{} events-ch]
  (let [input (node [:input#new-todo {:placeholder "What needs to be done?" :type "text"}])]
    (doto input
      (on-enter #(do (a/put! events-ch {:type :new-todo
                                        :caption (d/value input)})
                     (d/set-value! input nil))))))

(def filter-todos
  {:all identity
   :active (complement :done?)
   :completed :done?})

(defwidget todo-list-widget [{:keys [todos todo-filter]} events-ch]
  (node
   [:ul#todo-list
    (for [[id todo] (filter (comp (filter-todos todo-filter) val) todos)]
      (todo-item-widget {:todo (assoc todo :id id)} events-ch))]))

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
          (d/listen! :click #(a/put! events-ch {:type :clear-completed})))))]))

(defn make-todomvc [!todos events-ch]
  (let [!todo-filter (atom :all)]
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
            (clear-completed-widget {:!todos !todos} events-ch)]])))
