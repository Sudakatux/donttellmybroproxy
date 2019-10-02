(defproject donttellmybroproxy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.10.0"]
                 [ring "1.7.1"]
                 [nrepl "0.6.0"]
                 [metosin/muuntaja "0.6.4"]
                 [metosin/reitit "0.3.1"]
                 [metosin/ring-http-response "0.9.1"]
                 [ring/ring-defaults "0.3.2"]]
  :source-paths ["src/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  ;:repl-options {:init-ns donttellmybroproxy2.core}
  :main donttellmybroproxy.core)
