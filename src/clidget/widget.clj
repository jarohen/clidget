(ns clidget.widget)

;; TODO does this fit in with the new model?
#_(defmacro defwatcher [name [system-binding & params] & body]
    `(defn ~name [system# & params#]
       (add-watches system# ~atom-keys
                    (fn [resolved-atoms#]
                      (let [~system-binding resolved-atoms#
                            ~(vec params) params#]
                        ~@body)))))

(defn wrap-local-inits [locals]
  (->> (for [[local-key init] locals]
         [local-key `(fn [] ~init)])
       (into {})))

(defmacro defwidget [name [system-binding & params] & body]
  (let [widget-id (gensym "widget")]
    `(defn ~name [system# & params#]
       (updated-widget (assoc system#
                         :clidget/widget-id '~widget-id)
                       '~(update-in system-binding [:local] wrap-local-inits)
                       params#
                       (fn [resolved-state#]
                         (let [~(dissoc system-binding :local) resolved-state#
                               ~(vec params) params#]
                           ~@body))))))

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
