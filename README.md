# re-frisk

Visualize [re-frame](https://github.com/Day8/re-frame) pattern data or [reagent](https://reagent-project.github.io) ratom data as a tree structure, watch re-frame events and export state.

<img src="img/re-frisk-debugger.gif">

## Changes

### 0.4.4

- Remote debugger for react native and electron
- app-db cljs live filter
- сompatibility with previous versions of re-frame


## Usage

### web, react native, electron applications with re-frame 

Run remote re-frisk debugger server using leiningen re-frisk [plugin](https://github.com/flexsurfer/lein-re-frisk)

Add `[lein-re-frisk "0.4.5"]` into your global Leiningen config (`~/.lein/profiles.clj`) like so:

```clojure
{:user {:plugins [[lein-re-frisk "0.4.5"]]}}
```

or into the :plugins vector of your project.clj

```clojure
(defproject your-project "0.1.1"
  {:plugins [[lein-re-frisk "0.4.4"]]})
```

Start a web server in the current directory on the default port (4567):

    $ lein re-frisk

Select a different port by supplying the port number on the command line:

    $ lein re-frisk 8095


Add `[re-frisk-remote "0.4.1"]` to the dev `:dependencies` in your project.clj
                                
run re-frisk after document will be loaded and before any rendering calls, using `enable-re-frisk-remote!` function on the localhost and default port (4567)

```clojure
(:require [re-frisk-remote.core :refer [enable-re-frisk-remote!]])

(defn ^:export run
 []
 (dispatch-sync [:initialize])
 (enable-re-frisk-remote!)
 (reagent/render [simple-example]
                 (js/document.getElementById "app")))
```

Select a different host and port by supplying the host and port number:

```clojure
(enable-re-frisk-remote! {:host "192.168.1.1:8095"})
```

Run an application

ENJOY!


### web applications with re-frame

In-app re-frisk debugger 

You don't need leiningen plugin for using this version.
 
[![Clojars](https://img.shields.io/clojars/v/re-frisk.svg)](https://clojars.org/re-frisk)
 
Add `[re-frisk "0.4.4"]` to the dev `:dependencies` in your project.clj

Run re-frisk after document will be loaded and before any rendering calls, using `enable-re-frisk!` function

```clojure
(:require [re-frisk.core :refer [enable-re-frisk!]])

(defn ^:export run
 []
 (dispatch-sync [:initialize])
 (enable-re-frisk!)
 (reagent/render [simple-example]
                 (js/document.getElementById "app")))
```

ENJOY!

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

### re-frame handlers

```clojure
(enable-re-frisk! {:kind->id->handler? true})
```

### Debugger

You can export and import app state, and watch events in the separate debugger window.
Unfortunately debugger doesn't work in IE.

Export works only for the cljs [data structures](https://github.com/cognitect/transit-cljs#default-type-mapping).

<img src="img/debugger.png">

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
