;;;; Copyright © Paul Stadig.  All rights reserved.
;;;;
;;;; This Source Code Form is subject to the terms of the Mozilla Public
;;;; License, v. 2.0.  If a copy of the MPL was not distributed with this file,
;;;; You can obtain one at http://mozilla.org/MPL/2.0/.
;;;;
;;;; This Source Code Form is "Incompatible With Secondary Licenses", as defined
;;;; by the Mozilla Public License, v. 2.0.
(ns clojurebot.eval
  (:refer-clojure :exclude [eval])
  (:require
   [clojurebot.clojail.core :as clojail]
   [clojurebot.clojail.jvm :as clojail.jvm])
  (:use
   [clojurebot.clojail.testers :only [secure-tester-without-def]])
  (:import
   (java.io PrintWriter StringWriter)))

(declare eval)

(defn init
  [jar-paths]
  ;; we must have permissions to read the jars to load the source code from them
  (let [permissions (for [jar-path jar-paths]
                      (java.io.FilePermission. jar-path "read"))
        context (-> (apply clojail.jvm/permissions permissions)
                    clojail.jvm/domain
                    clojail.jvm/context)
        sandbox (clojail/sandbox secure-tester-without-def :context context)]
    ;; this is kind of ugly, but is meant to hide `sandbox` from the code being
    ;; evaluated.  this is not strictly necessary since we already blacklist all
    ;; clojurebot.* namespaces, but still
    (defn eval
      "Evaluate a string of clojure code in a sandbox."
      [^String in]
      (let [form (binding [*read-eval* false] (read-string in))]
        (sandbox `(let [out# (java.io.StringWriter.)
                        err# (java.io.StringWriter.)]
                    (binding [*out* (java.io.PrintWriter. out#)
                              *err* (java.io.PrintWriter. err#)
                              *print-length* 100
                              *print-level* 10]
                      (println ~form)
                      [(str out#) (str err#)])))))))

(defn -main
  "This is just here for testing."
  [in]
  (let [[out err] (eval in)]
    (when (seq out)
      (print out))
    (when (seq err)
      (binding [*out* *err*]
        (print err)))))
