(ns todomvc.css
  (:require [gaka.core :as gaka]))

(def todo-css
  (gaka/css [:div.todos
             {:padding "0.8em"}
             [:table
              [:td
               {:padding "0.3em"}]
              [:td.check
               {:width "2.5em"
                :height "2.5em"
                :cursor "pointer"}
               [:img
                {:width "100%"
                 :height "100%"}]]
              [:td.caption
               {:font-size "1.3em"}]
              [:tr.todo-done
               [:td.caption
                {:text-decoration "line-through"
                 :color "#77c06c"}]]]]))

