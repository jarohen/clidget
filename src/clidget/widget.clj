(ns clidget.widget
  (:require [dommy.macros :refer [node]]))

(defmacro defwatcher [name [system-binding & params] & body]
  (let [atom-keys (mapv keyword (:keys system-binding))
        system-sym (gensym "system")]
    `(defn ~name [~system-sym & params#]
       (add-watches ~system-sym ~atom-keys
                    (fn [resolved-atoms#]
                      (let [~system-binding resolved-atoms#
                            ~(vec params) params#]
                        ~@body))))))

(defmacro defwidget [name [system-binding & params] & body]
  (let [atom-keys (mapv keyword (:keys system-binding))
        system-sym (gensym "system")
        node-sym (gensym "node")]
    `(defn ~name [~system-sym & params#]
       (let [~node-sym (node [:div])]
         (add-watches ~system-sym ~atom-keys
                      (fn [resolved-atoms#]
                        (let [~system-binding resolved-atoms#
                              ~(vec params) params#
                              new-content# (do ~@body)]
                          (dommy.core/replace-contents! ~node-sym new-content#))))
         ~node-sym))))

(comment                                ; tests
  
  (require '[clojure.core.async :as a])

  (intern (doto 'dommy.core create-ns) 'replace-contents!
          (fn [el contents]
            (prn "replacing" el "with" contents)))

  (intern (doto 'dommy.macros create-ns) 'node
          identity)

  (macroexpand-1 '(defwidget test-widget [{:keys [counters]}]
                    [:h2 "Hello world!"]))

  (defwidget test-widget [{:keys [counter]}]
    (let [[old-count new-count] counter]
      [:h2 "counter was: " old-count ", is now:" new-count]))

  (def !counter (atom 0))

  (def foo-widget
    (test-widget {:counter !counter}))

  (swap! !counter inc)

  )


