(ns re-frisk.ui.components.github)

(defn link []
  [:a {:href       "https://github.com/flexsurfer/re-frisk"
       :target     "_blank"
       :style {:font-size 12}
       :aria-label "Star re-frisk on GitHub"}
   "Github"])