(ns re-frisk.db
  (:require [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]
            [re-frame.trace :as trace]))

(defonce
 tool-state
 (reagent/atom
  {;; INTERNAL TOOL

   ;External window opened
   :ext-win-opened?    false
   ;Latest opened position
   :latest-left        600
   ;Options provided by user {:ext_height :ext_width :events?}
   :opts               nil

   ;; EXTERNAL TOOL

   :app-db-delta-error false
   :subs-delta-error   false

   ;; COMMON

   ;Listening paused
   :paused?            false
   ;Selected event {:event  :app-db-diff  :indx  :queue}
   :selected-event     nil
   ;Auto-scroll events to bottom
   :scroll-bottom?     nil
   ;Events colors set by user
   :events-colors      nil
   ;re-frame trace enabled
   :trace?             (trace/is-trace-enabled?)
   ;js documant
   :doc                nil
   ;global subscriptions graph opened
   :graph-opened?      false
   ;event subs graph opened
   :subs-graph-opened? false
   ;timeline opened
   :timeline-opened?   false
   ;timeline zoom
   :timeline-zoom      0.1}))