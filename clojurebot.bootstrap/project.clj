(defproject pjstadig/clojurebot.bootstrap "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Mozilla Public License, v. 2.0"
            :url "http://mozilla.org/MPL/2.0/"}
  :dependencies [[org.projectodd.shimdandy/shimdandy-api "1.2.0"]]
  :java-source-paths ["src"]
  :main ^:skip-aot clojurebot.main
  :target-path "target/%s")
