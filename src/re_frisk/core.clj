(ns re-frisk.core
  (:require [cljs.env]))

(defmacro export-debugger! []
  (when @cljs.env/*compiler*
    (let [p (get-in @cljs.env/*compiler* [:options :external-config :re-frisk :script-src-path])
          m (get-in @cljs.env/*compiler* [:options :main])]
      `(do
         (re-frisk.debugger/register ~p #(~(symbol (str "." m ".runrefriskdebbuger")) %1 %2))
         (cljs.core/defn ~(vary-meta 'runrefriskdebbuger assoc :tag :export)
           [~'data]
           (re-frisk.debugger/run ~'data))))))

(defmacro def-view [fname params body]
  (if (and @cljs.env/*compiler* (get-in @cljs.env/*compiler* [:options :external-config :re-frisk :enabled]))
    (let [v (keyword (str (:name (:ns &env)) "/" (name fname)))
          t (tree-seq #(or (seq? %) (vector? %) (map? %)) identity body)
          x (filter #(and (or (seq? %) (vector? %) (map? %))
                          (symbol? (first %))
                          (case (name (first %)) ("subscribe" "dispatch") true false)) ;;if someone has not re-frame functions with the same name, something bad can happen, how to check namespace?
                    t)
          s (mapv #(first (second %)) (filter #(= (name (first %)) "subscribe") x))
          d (mapv #(first (second %)) (filter #(= (name (first %)) "dispatch") x))
          custom? (> (count (filter #(and (or (seq? %) (vector? %) (map? %))
                                          (symbol? (first %))
                                          (= (name (first %)) "create-class")) ;;if someone has not reagent functions with the same name, something bad can happen, how to check namespace?
                                    t)) 0)]
      `(cljs.core/defn ~fname
         ~params
         (do
           (re-frisk.core/reg-view ~v ~s ~d)
           ~(if custom?
              body
              `(reagent.core/create-class
                 {:component-will-unmount #(re-frisk.core/unmount-view ~v)
                  :display-name  ~v
                  :reagent-render (fn ~params ~body)})))))
    `(cljs.core/defn ~fname ~params ~body)))