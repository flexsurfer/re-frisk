(ns re-frisk.clipboard)

(defn copy-to-clip [text]
  (let [el (.createElement js/document "textarea")]
    (set! (.-value el) text)
    (.appendChild js/document.body el)
    (.select el)
    (.execCommand js/document "copy")
    (.removeChild js/document.body el)))