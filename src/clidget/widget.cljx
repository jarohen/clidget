(ns clidget.widget
  #+cljs (:require-macros [clidget.widget :refer [with-widget-cache]]))

(def ^:dynamic ^:private *context* nil)

(defn- get-widget-key [system keys-binding]
  (select-keys system (map :val-key keys-binding)))

(defn- resolve-state [system keys-binding]
  (->> (for [{:keys [val-key atom-key]} keys-binding]
         [val-key (or (get system val-key)
                      (some->> atom-key
                               (get system)
                               deref))])
       (into {})))

(defn get-cached-widget [{:keys [from-cache !to-cache]} widget-key]
  (when from-cache
    (let [cached-widget (get from-cache widget-key)]
      (when (and cached-widget !to-cache)
        (swap! !to-cache assoc widget-key cached-widget)
        cached-widget))))

(defn cache-widget! [widget !widget-cache widget-key]
  (when !widget-cache
    (swap! !widget-cache assoc widget-key widget)))

(defn- init-locals [system locals-binding]
  (reduce (fn [system [atom-key init-fn]]
            (assoc system
              atom-key (init-fn)))
          system
          locals-binding))

(defn add-watches [system keys-binding render-widget-fn]
  (doseq [{:keys [val-key atom-key]} keys-binding]
    (when-let [watched-atom (some->> atom-key
                                     (get system))]
      (add-watch watched-atom (gensym "clidget")
                 (fn [_ _ _ new-value]
                   (render-widget-fn (assoc system val-key new-value)))))))

(defmacro with-widget-cache [!cache & body]
  `(let [from-cache# @~!cache]
     (binding [*context* {:from-cache from-cache#
                          :!to-cache (doto ~!cache
                                       (reset! {}))}]
       ~@body)))

(defn re-render-widget [{!parent-widget-cache :!to-cache} widget-key system keys-binding render-widget-fn]
  (let [!widget (atom nil)
        !widget-cache (atom {})
        render-widget (fn [system]
                        (doto (with-widget-cache !widget-cache
                                (render-widget-fn (-> system
                                                      (merge (resolve-state system keys-binding))
                                                      (dissoc :clidget/widget-key
                                                              :clidget/widget-type))))
                          
                          (cache-widget! !parent-widget-cache widget-key)

                          (#(when-let [current-widget @!widget]
                              ;; This is called when an atom that
                              ;; we're watching changes - our parent
                              ;; may not have updated.
                              (.. current-widget -parentNode (replaceChild % current-widget))))
                          
                          (->> (reset! !widget))))]

    (add-watches system keys-binding render-widget)
    (reset! !widget (render-widget system))))

(defn updated-widget [system keys-binding locals-binding render-widget-fn]
  ;; this fn is called whenever a parent-widget asks us to reload

  (let [widget-key (get-widget-key system keys-binding)]
    (or (get-cached-widget *context* widget-key)
        (re-render-widget *context*
                          widget-key
                          (-> system
                              (init-locals locals-binding))
                          keys-binding
                          render-widget-fn))))

#+clj
(defn parse-keys-binding [binding]
  (->> (for [sym binding]
         {:val-key (keyword sym)
          :atom-key (keyword (format "!%s" sym))})
       vec))

#+clj
(defn wrap-local-inits [locals]
  (->> (for [[local-key init] locals]
         [local-key `(fn [] ~init)])
       (into {})))

#+clj
(defmacro defwidget [name [system-binding & params] & body]
  (let [widget-type (gensym "widget")]
    `(defn ~name [system# & params#]
       (updated-widget (assoc system#
                         :clidget/widget-type '~widget-type)
                       ~(-> system-binding :keys parse-keys-binding)
                       ~(-> system-binding :locals wrap-local-inits)
                       (fn [resolved-state#]
                         (let [~(dissoc system-binding :locals) resolved-state#
                               ~(vec params) params#]
                           ~@body))))))
