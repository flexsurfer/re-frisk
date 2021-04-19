(ns ^{:mranderson/inlined true} re-frisk.inlined-deps.reagent.v1v0v0.reagent.impl.protocols)

(defprotocol Compiler
  (get-id [this])
  (as-element [this x])
  (make-element [this argv component jsprops first-child]))

