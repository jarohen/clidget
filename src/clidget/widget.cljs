(ns clidget.widget
  (:require [dommy.core :as d]))

(def ^:dynamic ^:private *old-widget-cache* nil)
(def ^:dynamic ^:private *!widget-cache* nil)

(defn- resolve-state [system widget-binding]
  (->> (for [atom-key (:keys widget-binding)]
         [(keyword atom-key) (or (get system (keyword atom-key))
                                 @(get system (keyword (str "!" (name atom-key)))))])
       (into {})))

(defn widget-cache-key [system extra-params]
  (assoc (select-keys system [:clidget/widget-id :clidget/widget-key])
    :params extra-params))

(defn get-cached-widget [widget-cache system extra-params]
  (get widget-cache (widget-cache-key system extra-params)))

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
    (let [atom-key (keyword (str "!" (name atom-sym)))
          watched-atom (get system atom-key)]
      (add-watch watched-atom (gensym "clidget")
                 (fn [_ _ _ new-value]
                   (render-widget-fn (assoc system (keyword atom-sym) new-value)))))))

(defn re-render-widget [!widget-cache system widget-binding extra-params render-widget-fn]
  (let [system-with-locals (-> system (init-locals widget-binding))
        !widget (atom nil)
        render-widget (fn [system]
                        (doto (render-widget-fn (-> system
                                                    (merge (resolve-state system widget-binding))
                                                    (dissoc :clidget/widget-key :clidget/widget-id)))
                          (cache-widget! !widget-cache system-with-locals extra-params)
                          
                          (#(when-let [current-widget @!widget]
                              (d/replace! current-widget %)))
                          
                          (->> (reset! !widget))))]
    (add-watches system-with-locals widget-binding render-widget)
    (reset! !widget (render-widget system-with-locals))))

(defn updated-widget [system widget-binding extra-params render-widget-fn]
  ;; this fn is called whenever a parent-widget asks us to reload
  
  (or (get-cached-widget *old-widget-cache* system extra-params)
      (re-render-widget *!widget-cache* system widget-binding extra-params render-widget-fn)))

