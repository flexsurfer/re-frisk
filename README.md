# re-frisk

Take the full control on your [re-frame](https://github.com/Day8/re-frame) application.

<img src="re-frisk.png">

## DEMO

https://flexsurfer.github.io/conduit-re-frisk-demo/

## Usage

**Important**: Please note the following compatibility table:

re-frisk Version     | React Version     | Reagent Versions
-------------------- | ----------------- | ----------------
`1.1.0`              | React 16.13.0     | 0.10.x |
`1.0.0`              | React 16.9.0      | 0.9.x |
`0.5.3`              | React 16 - 16.8.6 | 0.8.x | 

### Web application

[![Clojars](https://img.shields.io/clojars/v/re-frisk.svg)](https://clojars.org/re-frisk)

re-frisk will be embedded in the DOM of your application. So my suggestion is to use re-frisk-remote, it doesn't affect your application and has more features such as traces for rendering etc
 
1. Add re-frisk as a dev dependency  `[re-frisk "1.1.0"]` 

2. Enable re-frisk

    `:preloads  [re-frisk.preload]`

    OR
    
    `(:require [re-frisk.core :as re-frisk])`
    
    `(re-frisk/enable)`
      

### React Native, Electron or Web applications

[![Clojars](https://img.shields.io/clojars/v/re-frisk-remote.svg)](https://clojars.org/re-frisk-remote)

1. Add re-frisk as a dev dependency `[re-frisk-remote "1.1.0"]` 

2. Enable re-frisk on default port (4567):

    `:preloads [re-frisk-remote.preload]`

    OR
    
    `(:require [re-frisk-remote.core :as re-frisk-remote])`
    
    `(re-frisk-remote/enable)`
    
3. Start re-frisk on default port (4567):

    `shadow-cljs run re-frisk-remote.core/start`

    OR
    
    add in `deps.edn`
    
    `:aliases {:dev {:extra-deps {re-frisk-remote {:mvn/version "1.0.0"}}}}}`
    
    create `re_frisk.clj`
    
    ```clojure
   (ns re-frisk
     (:require [re-frisk-remote.core :as re-frisk-remote]))
   
   (re-frisk-remote/start)
    ```
    
    `clj -R:dev re_frisk.clj`

Open re-frisk in a browser at http://localhost:4567

When remote debugging on an Android device you might need to enable reverse socket connections on port 4567:

```bash
adb reverse tcp:4567 tcp:4567
```
### Enable traces

shadow-cljs
```clojure
:compiler-options {:closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}}
```

OR

```clojure
:compiler         {:closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}}
```

### Settings

External window dimensions

```clojure
(re-frisk/enable {:ext_height 1000 :ext_width 1200})
```

If you don't need to watch events you can disable them

```clojure
(re-frisk/enable {:events? false})
```

Using custom IP or port

```clojure
(re-frisk-remote/enable {:host "192.168.0.2:7890"})

(re-frisk-remote/start {"7890"})
```

Normalize app-db before send to re-frisk

```clojure
(re-frisk-remote/enable {:normalize-db-fn (fn [app-db] (reduce ...))})
```


### bonus re-frame 6-domino cascade

[<img src="https://docs.google.com/drawings/d/1ptKAIPfb_gtwwSqYmt-JGTkwPVm_6LeWjjm-FcWznBs/pub?w=1786&amp;h=916">](
https://docs.google.com/drawings/d/1ptKAIPfb_gtwwSqYmt-JGTkwPVm_6LeWjjm-FcWznBs/edit?usp=sharing)
