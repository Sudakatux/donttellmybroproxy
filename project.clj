(defproject donttellmybroproxy "0.1.0-SNAPSHOT"
  :description "Dynamic route proxy"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.10.0"]
                 [ring "1.7.1"]
                 [nrepl "0.6.0"]
                 [metosin/reitit "0.3.1"]
                 [metosin/ring-http-response "0.9.1"]
                 [ring/ring-defaults "0.3.2"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 [com.google.javascript/closure-compiler-unshaded "v20190325"]
                 [org.clojure/google-closure-library "0.0-20190213-2033d5d9"]
                 [cljs-ajax "0.7.3"]
                 [reagent "0.10.0"]
                 [re-frame "0.12.0"]
                 [thheller/shadow-cljs "2.8.52"]
                 [day8.re-frame/re-frame-10x "0.6.2"]
                 [reagent-utils "0.3.3"]
                 [lilactown/hx "0.5.3"]
                 [cljs-bean "1.5.0"]
                 [funcool/struct "1.4.0"]
                 [venantius/accountant "0.2.5"]]

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  ;:repl-options {:init-ns donttellmybroproxy2.core}
  :main ^:skip-aot donttellmybroproxy.core
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[midje "1.9.9"]]
                   :plugins [[lein-midje "3.2.1"]]}
             :uberjar {:omit-source true
                       :aot :all
                       :uberjar-name "donttellmybro-proxy.jar"
                       :prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]}})
