;;;; Copyright © Paul Stadig.  All rights reserved.
;;;;
;;;; This Source Code Form is subject to the terms of the Mozilla Public
;;;; License, v. 2.0.  If a copy of the MPL was not distributed with this file,
;;;; You can obtain one at http://mozilla.org/MPL/2.0/.
;;;;
;;;; This Source Code Form is "Incompatible With Secondary Licenses", as defined
;;;; by the Mozilla Public License, v. 2.0.
(ns clojurebot.core.evaluator
  (:require
   [clojure.stacktrace :refer [print-cause-trace]]
   [cemerick.pomegranate :refer [add-dependencies]]
   [cemerick.pomegranate.aether :refer [dependency-files maven-central]])
  (:import
   (clojure.lang DynamicClassLoader)
   (java.net URL)
   (java.util.concurrent ExecutionException)
   (org.projectodd.shimdandy ClojureRuntimeShim)))

(defn missing-dep?
  [dependencies dep]
  (not (some (comp #{dep} first) dependencies)))

(defn conj-if-missing
  [dependencies [dependency-name _ :as dependency]]
  (cond-> dependencies
    (missing-dep? dependencies dependency-name) (conj dependency)))

(def latest-clojure
  '[org.clojure/clojure "1.8.0"])

(def shimdandy-impl
  '[org.projectodd.shimdandy/shimdandy-impl "1.2.0"])

(def repositories
  (merge maven-central
         {"clojars" "https://clojars.org/repo"}))

(def autorequires
  ["clojurebot.eval"])

(def clojurebot-eval-jar-path
  (System/getenv "CLOJUREBOT_EVAL_PATH"))

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

(defn exception->response
  [e]
  ["" (with-out-str (print-cause-trace e))])

(defn eval-in-environment
  [env string]
  (try
    (.invoke env "clojurebot.eval/eval" string)
    (catch ExecutionException e
      (exception->response (.getCause e)))
    (catch Throwable t
      (exception->response t))))

(defn raw-eval-in-environment
  [env string]
  (->> (.invoke env "clojure.core/read-string" string)
       (.invoke env "clojure.core/eval")))
