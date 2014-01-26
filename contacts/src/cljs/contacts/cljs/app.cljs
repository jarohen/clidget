(ns contacts.cljs.app
  (:require [clidget.widget :refer [defwidget] :include-macros true]
            [cljs.core.async :as a]
            [contacts.cljx.formatter :as f]
            [dommy.core :as d]
            [goog.events.KeyCodes :as kc])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(defwidget contact-widget [{} contact event-ch]
  (node
   [:li
    [:span (f/display-name contact)]
    (doto (node [:button.btn.btn-link "[delete]"])
      (d/listen! :click #(a/put! event-ch {:type :delete
                                           :contact contact})))]))

(defwidget contact-list-widget [{:keys [contacts]} event-ch]
  (node
   [:div
    [:h1 "Contact List:"]
    [:ul
     (for [contact (sort-by :last contacts)]
       (contact-widget {} contact event-ch))]]))

(defn new-contact-box [event-ch]
  (let [name-input (node [:input#new-contact.form-control
                          {:type "text"
                           :placeholder "New Contact"
                           :autofocus true}])]
    (doto name-input
      (d/listen! :keyup
          (fn [e]
            (when (= kc/ENTER (.-keyCode e))
              (a/put! event-ch {:type :create
                                :name (d/value name-input)})
              (d/set-value! name-input nil)))))))

(defn handle-events! [event-ch !contacts]
  (go-loop []
    (when-let [{:keys [type] :as event} (a/<! event-ch)]
      (case type
        :create
        (swap! !contacts conj (f/parse-contact (:name event)))

        :delete
        (swap! !contacts disj (:contact event)))
      (recur))))

(def test-contacts
  #{{:first "Ben" :last "Bitdiddle" :email "benb@mit.edu"}
    {:first "Alyssa" :middle-initial "P" :last "Hacker" :email "aphacker@mit.edu"}
    {:first "Eva" :middle "Lu" :last "Ator" :email "eval@mit.edu"}
    {:first "Louis" :last "Reasoner" :email "prolog@mit.edu"}
    {:first "Cy" :middle-initial "D" :last "Effect" :email "bugs@mit.edu"}
    {:first "Lem" :middle-initial "E" :last "Tweakit" :email "morebugs@mit.edu"}})

(set! (.-onload js/window)
      (fn []
        (let [!contacts (atom test-contacts)
              event-ch (doto (a/chan)
                         (handle-events! !contacts))]
          (d/replace-contents! (sel1 :#content)
                               (node
                                [:div.container
                                 (contact-list-widget {:!contacts !contacts} event-ch)
                                 (new-contact-box event-ch)])))))


