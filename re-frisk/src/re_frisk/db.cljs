(ns re-frisk.db
  (:require [reagent.core :as reagent]
            [re-frame.trace :as trace]))

(defonce
 tool-state
 (reagent/atom
  {;; Internal tool
   ;External window opened
   :ext-win-opened?    false
   ;Latest opened position
   :latest-left        600
   ;Options provided by user {:ext_height :ext_width :events?}
   :opts               nil

   ;; External tool
   :app-db-delta-error false
   :subs-delta-error   false

   ;; Common
   ;Selected event {:event  :app-db-diff  :indx  :queue}
   :selected-event     nil
   ;Auto-scroll events to bottom
   :scroll-bottom?     nil
   ;Events colors set by user
   :events-colors      nil
   ;re-frame trace enabled
   :trace?             (trace/is-trace-enabled?)}))