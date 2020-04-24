(ns re-frisk-remote.core
  (:require [re-frisk-remote.server.main :as main]))

;ENTRY POINT
(defn -main [& [port]]
  (main/main port))

(defn start [& [port]]
  (-main port))