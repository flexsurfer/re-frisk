(ns ^{:mranderson/inlined true} re-frisk.inlined-deps.reagent.v1v0v0.reagent.core
  (:require [re-frisk.inlined-deps.reagent.v1v0v0.reagent.ratom :as ra]))

(defmacro with-let
  "Bind variables as with let, except that when used in a component
  the bindings are only evaluated once. Also takes an optional finally
  clause at the end, that is executed when the component is
  destroyed."
  [bindings & body]
  `(ra/with-let ~bindings ~@body))
