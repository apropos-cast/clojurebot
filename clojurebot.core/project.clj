(defproject pjstadig/clojurebot.core "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Mozilla Public License, v. 2.0"
            :url "http://mozilla.org/MPL/2.0/"}
  :dependencies [[com.cemerick/pomegranate "0.4.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.projectodd.shimdandy/shimdandy-impl "1.2.0"]]
  :main ^:skip-aot clojurebot.core
  :target-path "target/%s"
  :profiles {:provided {:dependencies [[org.projectodd.shimdandy/shimdandy-api "1.2.0"]]}
             :uberjar {:aot :all}})
