(defproject pjstadig/clojurebot.core "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Mozilla Public License, v. 2.0"
            :url "http://mozilla.org/MPL/2.0/"}
  :dependencies [[cider/cider-nrepl "0.15.0"]
                 [com.cemerick/pomegranate "0.4.0"]
                 [com.outpace/config "0.10.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.projectodd.shimdandy/shimdandy-impl "1.2.0"]
                 [slack-rtm "0.1.6"]]
  :main ^:skip-aot clojurebot.core
  :target-path "target/%s"
  :profiles {:provided {:dependencies [[org.projectodd.shimdandy/shimdandy-api "1.2.0"]]}
             :uberjar {:aot :all}})
