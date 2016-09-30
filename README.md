# re-frisk

Visualize [re-frame](https://github.com/Day8/re-frame) pattern data in your re-frame apps or any ratom data in [regent](https://reagent-project.github.io) apps as a tree structure.

<img src="re-frisk-show.gif">

## Overview

This lightweight library helps you to visualize re-frame data and easily debug re-frame apps in the real time.

[![Clojars](https://img.shields.io/clojars/v/re-frisk.svg)](https://clojars.org/re-frisk)

## Setup

Add `[re-frisk "0.1.4"]` to the dev `:dependencies` in your project.clj


## Usage

If you want to watch re-frame app-db, run re-frisk before any rendering calls, using `enable-re-frisk!` function

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

If you are not using re-frame in your app, you can run re-frisk without re-frame by `enable-frisk!` function

```clojure
(enable-frisk!)
```

If you want to watch ratom, you can add it using `add-data` function

```clojure
(add-data :data-key your-data-ratom)
```

You can provide starting position for the re-frisk panel

```clojure
(enable-re-frisk! {:x 100 :y 500})

(enable-frisk! {:x 100 :y 500})
```

also, it will be helpful for the IE, because it doesn't support resize property, you can provide width and height

```clojure
(enable-re-frisk! {:width 400 :height 400})

(enable-frisk! {:width 400 :height 400})
```


## Advanced thing

You can watch all re-frame views which are rendering now

Add `:external-config {:re-frisk {:enabled true}}}}` to the dev `:compiler` in your project.clj if you are working with the re-frame app

<img src="re-frisk-project.png">

this config needs to do not generate any code in production.


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

If you are using `reagent.core/create-class` function for creating views, data for these views will be still showing in the re-frisk after this components will be unmounted.

## License

Copyright © 2016 Shovkoplyas Andrey [motor4ik]

Distributed under the MIT License (MIT)
