(use 'figwheel-sidecar.repl-api)
(start-figwheel! {:figwheel-options {:css-dirs ["resources/public/css"] :server-port 3450} :build-ids ["reagent"] :all-builds (figwheel-sidecar.config/get-project-builds)})
(cljs-repl "reagent")