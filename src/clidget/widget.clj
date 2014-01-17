(ns clidget.widget)

(defn wrap-local-inits [locals]
  (->> (for [[local-key init] locals]
         [local-key `(fn [] ~init)])
       (into {})))

(defmacro defwidget [name [system-binding & params] & body]
  (let [widget-id (gensym "widget")]
    `(defn ~name [system# & params#]
       (updated-widget (assoc system#
                         :clidget/widget-id '~widget-id)
                       ~(-> system-binding
                            (select-keys [:keys :locals])
                            (update-in [:keys] (fn [keys] `'~keys))
                            (update-in [:locals] wrap-local-inits))
                       params#
                       (fn [resolved-state#]
                         (let [~(dissoc system-binding :locals) resolved-state#
                               ~(vec params) params#]
                           ~@body))))))
