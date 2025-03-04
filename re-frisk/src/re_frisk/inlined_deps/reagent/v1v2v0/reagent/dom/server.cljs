(ns ^{:mranderson/inlined true} re-frisk.inlined-deps.reagent.v1v2v0.reagent.dom.server
  (:require ["react-dom/server" :as dom-server]
            [re-frisk.inlined-deps.reagent.v1v2v0.reagent.impl.util :as util]
            [re-frisk.inlined-deps.reagent.v1v2v0.reagent.impl.template :as tmpl]
            [re-frisk.inlined-deps.reagent.v1v2v0.reagent.impl.protocols :as p]
            [re-frisk.inlined-deps.reagent.v1v2v0.reagent.ratom :as ratom]))

(defn render-to-string
  "Turns a component into an HTML string."
  ([component]
   (render-to-string component tmpl/*current-default-compiler*))
  ([component compiler]
   (ratom/flush!)
   (binding [util/*non-reactive* true]
     (dom-server/renderToString (p/as-element compiler component)))))

(defn render-to-static-markup
  "Turns a component into an HTML string, without data-react-id attributes, etc."
  ([component]
   (render-to-static-markup component tmpl/*current-default-compiler*))
  ([component compiler]
   (ratom/flush!)
   (binding [util/*non-reactive* true]
     (dom-server/renderToStaticMarkup (p/as-element compiler component)))))
