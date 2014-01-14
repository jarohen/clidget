(ns clidget.widget)

(defn is-diff? [atom-key]
  (re-matches #"(.*)-diff" (str atom-key)))

(defn base-key [atom-key]
  (-> (or (second (re-matches #"(.*)-diff" (name atom-key)))
          (name atom-key))
      keyword))

(defn return-format [atom-key [old-value new-value]]
  (cond->> new-value
    (is-diff? atom-key) (conj [old-value])))

(defn deref-atoms [system atom-keys]
  (->> (for [atom-key atom-keys]
         [atom-key (return-format atom-key (repeat 2 @(get system (base-key atom-key))))])
       (into {})))

(defn add-watches [system atom-keys watch-fn]
  (doseq [atom-key atom-keys]
    (let [base (base-key atom-key)]
      (add-watch (get system base) (gensym (str "clidget-watch-" (name base)))
                 (fn [_ _ old-val new-val]
                   (watch-fn (-> (deref-atoms system (remove #{atom-key} atom-keys))
                                 (assoc atom-key (return-format atom-key [old-val new-val]))))))))
  (watch-fn (deref-atoms system atom-keys)))


