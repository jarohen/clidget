(ns clidget.widget)

(def ^:dynamic ^:private *from-widget-cache* nil)
(def ^:dynamic ^:private *!to-widget-cache* nil)

(defn- resolve-state [system widget-binding]
  (->> (for [atom-key (:keys widget-binding)]
         [(keyword atom-key) (or (get system (keyword atom-key))
                                 (some-> (get system (keyword (str "!" (name atom-key)))) deref))])
       (into {})))

(defn widget-cache-key [system extra-params]
  (assoc (select-keys system [:clidget/widget-id :clidget/widget-key])
    :params extra-params))

(defn get-cached-widget [from-widget-cache !to-widget-cache system extra-params]
  (let [cache-key (widget-cache-key system extra-params)
        cached-widget (get from-widget-cache cache-key)]
    (when (and cached-widget !to-widget-cache)
      (swap! !to-widget-cache assoc cache-key cached-widget)
      cached-widget)))

(defn cache-widget! [widget !widget-cache system extra-params]
  (when !widget-cache
    (swap! !widget-cache assoc (widget-cache-key system extra-params) widget)))

(defn- init-locals [system widget-binding]
  (reduce (fn [system [atom-key init-fn]]
            (assoc system
              atom-key (init-fn)))
          system
          (:locals widget-binding)))

(defn add-watches [system widget-binding render-widget-fn]
  (doseq [atom-sym (:keys widget-binding)]
    (let [atom-key (keyword (str "!" (name atom-sym)))]
      (when-let [watched-atom (get system atom-key)]
        (add-watch watched-atom (gensym "clidget")
                   (fn [_ _ _ new-value]
                     (render-widget-fn (assoc system (keyword atom-sym) new-value))))))))

(defn re-render-widget [!parent-widget-cache system widget-binding extra-params render-widget-fn]
  (let [system-with-locals (-> system (init-locals widget-binding))
        !widget (atom nil)
        !!from-widget-cache (atom (atom {}))
        render-widget (fn [system]
                        (let [!to-widget-cache (atom {})]
                          (let [widget (binding [*from-widget-cache* @@!!from-widget-cache
                                                 *!to-widget-cache* !to-widget-cache]
                                         (render-widget-fn (-> system
                                                               (merge (resolve-state system widget-binding))
                                                               (dissoc :clidget/widget-key
                                                                       :clidget/widget-id))))]

                            (reset! !!from-widget-cache !to-widget-cache)
                            
                            (doto widget
                              (cache-widget! !parent-widget-cache
                                             system-with-locals
                                             extra-params)
                              
                              (#(when-let [current-widget @!widget]
                                  (.. current-widget -parentNode (replaceChild % current-widget))))
                              
                              (->> (reset! !widget))))))]

    (add-watches system-with-locals widget-binding render-widget)
    (reset! !widget (render-widget system-with-locals))))

(defn updated-widget [system widget-binding extra-params render-widget-fn]
  ;; this fn is called whenever a parent-widget asks us to reload
  
  (or (get-cached-widget *from-widget-cache* *!to-widget-cache* system extra-params)
      (re-render-widget *!to-widget-cache* system widget-binding extra-params render-widget-fn)))
