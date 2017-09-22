(defproject pjstadig/clojurebot.sandbox "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Mozilla Public License, v. 2.0"
            :url "http://mozilla.org/MPL/2.0/"}
  :jvm-opts ["-Djava.security.policy=example.policy"]
  :target-path "target/%s"
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :uberjar {:aot :all}})
