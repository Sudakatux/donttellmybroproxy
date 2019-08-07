(defproject donttellmybro-proxy "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  
  
  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [http-kit "2.3.0"]
                 [compojure "1.6.1"]
                 [metosin/muuntaja "0.6.4"]
                 [org.clojure/clojurescript "1.10.516"]
                 [org.clojure/core.async  "0.4.474"]
                 [ring "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [reagent "0.8.0"]
                 [figwheel "0.5.18"]
                 [cljs-ajax "0.8.0"]
                 [cljsjs/material-ui "3.9.3-0"]
                 [cljsjs/material-ui-icons "3.0.1-0"]]

  :plugins [[lein-figwheel "0.5.18"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

 

  :target-path "target/%s/"
  :main ^:skip-aot donttellmybro-proxy.server

  :source-paths ["src/clj"]

  :cljsbuild
  {:builds
   {:client
    {:source-paths ["src/cljs"]
     :figwheel true
     :compiler {:parallel-build true
                :source-map true
                :optimizations :none
                :main "donttellmybro-proxy.core"
                :output-dir "target/cljsbuild/client/public/js/out"
                :output-to "target/cljsbuild/client/public/js/main.js"
                :asset-path "js/out"
                :npm-deps false}}

    ;; FIXME: Doesn't work due to Closure bug with scoped npm packages
    :client-npm
    {:source-paths ["src/cljs"]
     :figwheel true
     :compiler {:parallel-build true
                :source-map true
                :optimizations :none
                :main "donttellmybro-proxy.core"
                :output-dir "target/cljsbuild/client-npm/public/js/out"
                :output-to "target/cljsbuild/client-npm/public/js/main.js"
                :asset-path "js/out"
                :install-deps true
                :npm-deps {react "16.8.1"
                           react-dom "16.8.1"
                           "@material-ui/core" "3.1.1"
                           "@material-ui/icons" "3.0.1"}
                :process-shim true}}}}

  ;; :cljsbuild {:builds
  ;;             [{:id "dev"
  ;;               :source-paths ["src/cljs"]

  ;;               ;; The presence of a :figwheel configuration here
  ;;               ;;
  ;;               ;; will cause figwheel to inject the figwheel client
  ;;               ;; into your build
  ;;               :figwheel {:on-jsload "donttellmybro-proxy.core/on-js-reload"
  ;;                          ;; :open-urls will pop open your application
  ;;                          ;; in the default browser once Figwheel has
  ;;                          ;; started and compiled your application.
  ;;                          ;; Comment this out once it no longer serves you.
  ;;                          :open-urls ["http://localhost:3449/index.html"]}

  ;;               :compiler {:main donttellmybro-proxy.core
  ;;                          :asset-path "js/compiled/out"
  ;;                          :output-to "resources/public/js/compiled/donttellmybro_proxy.js"
  ;;                          :output-dir "resources/public/js/compiled/out"
  ;;                          :source-map-timestamp true
  ;;                          ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
  ;;                          ;; https://github.com/binaryage/cljs-devtools
  ;;                          :preloads [devtools.preload]}}
  ;;              ;; This next build is a compressed minified build for
  ;;              ;; production. You can build this with:
  ;;              ;; lein cljsbuild once min
  ;;              {:id "min"
  ;;               :source-paths ["src/cljs"]
  ;;               :compiler {:output-to "resources/public/js/compiled/donttellmybro_proxy.js"
  ;;                          :main donttellmybro-proxy.core
  ;;                          :optimizations :advanced
  ;;                          :pretty-print false}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"
             :http-server-root "public"

             ;:css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this


             ;; doesn't work for you just run your own server :) (see lein-ring)

             :ring-handler donttellmybro-proxy.interactive-server/dev-app

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you are using emacsclient you can just use
             ;; :open-file-command "emacsclient"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"

             ;; to pipe all the output to the repl
             ;; :server-logfile false
             }


  ;; Setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]
                                  [figwheel-sidecar "0.5.18"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src/clj" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   ; :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                   :resource-paths ["target/cljsbuild/client" "target/cljsbuild/client-npm"]
                   ;; need to add the compliled assets to the :clean-targets
                   ;:clean-targets ^{:protect false} ["resources/public/js/compiled"
                    ;                                 :target-path]
                   }
             :uberjar {
                                        ;this runs the first entry in the cljsbuils :builds map below
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                       :env {:environment "production"}
                       :main donttellmybro-proxy.server
                       :aot :all
                       :source-paths ["src/clj"]
                       :omit-source true
                       }})
