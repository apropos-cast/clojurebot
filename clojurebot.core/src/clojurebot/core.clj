;;;; Copyright © Paul Stadig.  All rights reserved.
;;;;
;;;; This Source Code Form is subject to the terms of the Mozilla Public
;;;; License, v. 2.0.  If a copy of the MPL was not distributed with this file,
;;;; You can obtain one at http://mozilla.org/MPL/2.0/.
;;;;
;;;; This Source Code Form is "Incompatible With Secondary Licenses", as defined
;;;; by the Mozilla Public License, v. 2.0.
(ns clojurebot.core
  (:require
   [clojure.tools.nrepl.server :refer [start-server stop-server]]
   [cemerick.pomegranate :refer [add-dependencies]]
   [cemerick.pomegranate.aether :refer [dependency-files]])
  (:import
   (clojure.lang DynamicClassLoader)
   (java.net URL)
   (org.projectodd.shimdandy ClojureRuntimeShim)))

(def clojurebot-eval-jar-path
  (System/getenv "CLOJUREBOT_EVAL_PATH"))

(defonce nrepl-server
  (atom nil))

(defn missing-dep?
  [dependencies dep]
  (not (some (comp #{dep} first) dependencies)))

(defn conj-if-missing
  [dependencies [dependency-name _]]
  (cond-> dependencies
    (missing-dep? dependencies dependency-name) (conj dependency)))

(def latest-clojure
  '[org.clojure/clojure "1.8.0"])

(def shimdandy-impl
  '[org.projectodd.shimdandy/shimdandy-impl "1.2.0"])

(def repositories
  (merge cemerick.pomegranate.aether/maven-central
         {"clojars" "https://clojars.org/repo"}))

(def autorequires
  ["clojurebot.eval"])

(defn make-environment
  [dependencies]
  (let [loader (doto (DynamicClassLoader. (ClassLoader/getSystemClassLoader))
                 (.addURL (URL. (str "file:" clojurebot-eval-jar-path))))
        dependencies (-> dependencies
                         (conj-if-missing latest-clojure)
                         (conj-if-missing shimdandy-impl))
        dependencies (add-dependencies :classloader loader
                                       :coordinates dependencies
                                       :repositories repositories)
        ;; these must be passed along so permissions can be configured so the
        ;; source can be read from them
        jar-file-paths (for [file (dependency-files dependencies)]
                         (.getCanonicalPath file))]
    (doto (ClojureRuntimeShim/newRuntime loader)
      (.require (into-array String autorequires))
      (.invoke "clojurebot.eval/init" jar-file-paths))))

(defn eval-in-environment
  [env string]
  (.invoke env "clojurebot.eval/eval" string))

(defn raw-eval-in-environment
  [env string]
  (->> (.invoke env "clojure.core/read-string" string)
       (.invoke env "clojure.core/eval")))

(def default-dependencies
  '[[org.clojure/clojure "1.8.0"]])

(def environments
  (atom {}))

(defn main
  "I don't do a whole lot ... yet."
  []
  (reset! nrepl-server (start-server :port 1980))
  (println "Hello, World!"))
