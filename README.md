# re-frisk

Visualize [re-frame](https://github.com/Day8/re-frame) pattern data or [reagent](https://reagent-project.github.io) ratom data as a tree structure, watch re-frame events and export state.

<img src="img/re-frisk-debugger.gif">

## Changes

### 0.4.5
- Implemented app-db-sorted

### 0.4.4

- Remote debugger for react native and electron
- app-db cljs live filter
- сompatibility with previous versions of re-frame


## Usage

### Web applications with re-frame

In-app re-frisk debugger. The debugger will be embedded into the interface of your application.
 
[![Clojars](https://img.shields.io/clojars/v/re-frisk.svg)](https://clojars.org/re-frisk)
 
1. Add `[re-frisk "0.4.5"]` to the dev `:dependencies` in your project.clj

2. Run re-frisk using `enable-re-frisk!` function

```clojure
(:require [re-frisk.core :refer [enable-re-frisk!]])

(defn ^:export run
 []
 (dispatch-sync [:initialize])
 (enable-re-frisk!)
 (reagent/render [simple-example]
                 (js/document.getElementById "app")))
```

This is just an example, it's better to enable re-frisk in the dev environment

ENJOY!

### React native, Electron and Web applications with re-frame using remote server 

Run remote re-frisk debugger server using leiningen re-frisk [plugin](https://github.com/flexsurfer/lein-re-frisk) by following next steps:

1. Add `[lein-re-frisk "0.4.7"]` into your global Leiningen config (`~/.lein/profiles.clj`) like so:

```clojure
{:user {:plugins [[lein-re-frisk "0.4.7"]]}}
```

or into the `:plugins` vector of your project.clj

```clojure
(defproject your-project "0.1.1"
  {:plugins [[lein-re-frisk "0.4.7"]]})
```

2. Start a web server in the current directory on the default port (4567):

    $ lein re-frisk

Or select a different port by supplying the port number on the command line:

    $ lein re-frisk 8095


3. Add `[re-frisk-remote "0.4.2"]` to the dev `:dependencies` in your project.clj
                                
run re-frisk using `enable-re-frisk-remote!` function on the localhost and default port (4567)

```clojure
(:require [re-frisk-remote.core :refer [enable-re-frisk-remote!]])

(defn ^:export run
 []
 (dispatch-sync [:initialize])
 (enable-re-frisk-remote!)
 (reagent/render [simple-example]
                 (js/document.getElementById "app")))
```

Or select a different host and port by supplying the host and port number:

```clojure
(enable-re-frisk-remote! {:host "192.168.1.1:8095"})
```

This is just an example, it's better to enable re-frisk in the dev environment

Run an application

ENJOY!

### re-frame handlers

You can also watch all re-frame subscriptions and events

```clojure
(enable-re-frisk! {:kind->id->handler? true})
```

### Events

If you don't want to watch events you can turn it off providing settings `{:events? false}`

```clojure
(enable-re-frisk! {:events? false})
```

Also you can watch interceptor's context providing `re-frisk.core/watch-context` in the reg-event interceptors list

```clojure
(reg-event-db
 :timer-db
 [re-frisk.core/watch-context]
 (fn
  [db [_ value]]
  (assoc db :timer value)))
```

### Export and Import state of your re-frame application

Export works only for the cljs [data structures](https://github.com/cognitect/transit-cljs#default-type-mapping).

<img src="img/debugger.png">

### re-frame 6-domino cascade and re-frisk

[<img src="https://docs.google.com/drawings/d/1ptKAIPfb_gtwwSqYmt-JGTkwPVm_6LeWjjm-FcWznBs/pub?w=1786&amp;h=916">](
https://docs.google.com/drawings/d/1ptKAIPfb_gtwwSqYmt-JGTkwPVm_6LeWjjm-FcWznBs/edit?usp=sharing)

### Settings

You can provide starting position for the re-frisk panel

```clojure
(enable-re-frisk! {:x 100 :y 500})

(enable-frisk! {:x 100 :y 500})
```

also, it will be helpful for the IE, because it doesn't support resize property, you can provide width and height

```clojure
(enable-re-frisk! {:width "400px" :height "400px"})

(enable-frisk! {:width "400px" :height "400px"})
```

### reagent
If you are not using re-frame in your app, you can run re-frisk without re-frame by `enable-frisk!` function

```clojure
(enable-frisk!)
```

If you want to watch ratom or log any data, you can add it using `add-data` or `add-in-data` functions

```clojure
(add-data :data-key your-data)

(add-in-data [:data-key1  :data-key2] your-data)
```

## Advanced thing

For me, it's very handy to name events with the appropriate suffix, for example for reg-event-db :event-key-db, and for reg-event-fx :event-key-fx, in that case re-frisk automatically highlight these events

Also you can watch all re-frame views which are rendering now

Add `:external-config {:re-frisk {:enabled true}}}}` to the dev `:compiler` in your project.clj if you are working with the re-frame app

<img src="img/re-frisk-project.png">

this config needed to do not generate any code in production.


Require macro
```clojure
(:require [re-frisk.core :refer-macros [def-view]])
```

Define your views (components) with the `def-view` macro

```clojure
(def-view greeting
 [message]
 [:h1 message])
```


### For more

re-frame [dev/re_frisk/demo.cljs](https://github.com/flexsurfer/re-frisk/blob/master/dev/re_frisk/demo.cljs).
reagent [dev/re_frisk/reagent_demo.cljs](https://github.com/flexsurfer/re-frisk/blob/master/dev/re_frisk/reagent_demo.cljs).

### Known issues

Works weird in the Internet Explorer which doesn't support css resize property.
Debugger doesn't work in IE.

If you are using `reagent.core/create-class` function for creating views, data for these views will be still showing in the re-frisk after this components will be unmounted.

## License

Copyright © 2016-2017 Shovkoplyas Andrey [motor4ik]

Distributed under the MIT License (MIT)
