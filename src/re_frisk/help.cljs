(ns re-frisk.help)

(def fx {:db "reset! app-db with a new value. Expects a map. / re-frame's internal"
         :dispatch "`dispatch` one event. Expects a single vector. / re-frame's internal"
         :dispatch-n  "`dispatch` more than one event. Expects a list or vector of events. Something for which sequential? returns true. / re-frame's internal"
         :deregister-event-handler  "Removes a previously registered event handler. Expects either a single id (typically a keyword), or a seq of ids. / re-frame's internal"
         :dispatch-later "`dispatch` one or more events after given delays. Expects a collection of maps with two keys:  :`ms` and `:dispatch`. / re-frame's internal"})

(def cofx {:db "Adds to coeffects the value in `app-db`, under the key `:db`. / re-frame's internal"})

(def intrcp {:coeffects "An interceptor which injects re-frame :db coeffect. / re-frame's internal"
             :do-fx "An interceptor which actions a `context's` (side) `:effects`. For each key in the `:effects` map, call the `effects handler` previously registered using `reg-fx`. / re-frame's internal"
             :db-handler "An interceptor which wraps the kind of event handler given to `reg-event-db`. These handlers take two arguments;  `db` and `event`, and they return `db`. / re-frame's internal"
             :fx-handler "An interceptor which wraps the kind of event handler given to `reg-event-fx`. These handlers take two arguments;  `coeffects` and `event`, and they return `effects`. / re-frame's internal"})

(def intrcp-fn {:coeffects "Function which adds to coeffects the value in `app-db`, under the key `:db`. / re-frame's internal"
                :do-fx "Function which calls the `effects handler` previously registered using `reg-fx` for each key in the `:effects` map. / re-frame's internal"
                :db-handler "Function which calls the handler given to `reg-event-db`. This handler take two arguments;  `db` and `event`, and returns `db`. / re-frame's internal"
                :fx-handler "Function which calls the handler given to `reg-event-fx`. This handler take two arguments;  `coeffects` and `event`, and returns `effects`. / re-frame's internal"})
