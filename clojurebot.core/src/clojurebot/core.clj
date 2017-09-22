;;;; Copyright © Paul Stadig.  All rights reserved.
;;;;
;;;; This Source Code Form is subject to the terms of the Mozilla Public
;;;; License, v. 2.0.  If a copy of the MPL was not distributed with this file,
;;;; You can obtain one at http://mozilla.org/MPL/2.0/.
;;;;
;;;; This Source Code Form is "Incompatible With Secondary Licenses", as defined
;;;; by the Mozilla Public License, v. 2.0.
(ns clojurebot.core
  (:refer-clojure
   :exclude [special-symbol?])
  (:require
   [clojure.stacktrace :refer [print-cause-trace]]
   [clojure.string :as string]
   [cemerick.pomegranate :refer [add-dependencies]]
   [cemerick.pomegranate.aether :refer [dependency-files]]
   [clojure.tools.nrepl.server :refer [start-server stop-server]]
   [outpace.config :refer [defconfig defconfig!]]
   [slack-rtm.core :as slack])
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
  [dependencies [dependency-name _ :as dependency]]
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

(defn create-environment
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
  (try
    (.invoke env "clojurebot.eval/eval" string)
    (catch Throwable t
      ["" (with-out-str (print-cause-trace t))])))

(defn raw-eval-in-environment
  [env string]
  (->> (.invoke env "clojure.core/read-string" string)
       (.invoke env "clojure.core/eval")))

(def default-dependencies
  '[[org.clojure/clojure "1.8.0"]])

(defconfig! token)

(defonce slack-connection (atom nil))

(defconfig log-channel "C76MJ835X")

(defn send-message*
  [message]
  (slack/send-event (:dispatcher @slack-connection) message))

(defn log
  [& message]
  (when log-channel
    (send-message* {:type "message"
                    :channel log-channel
                    :text (string/join " " message)
                    :mrkdwn false})))

(defn info
  [& message]
  (apply log [(str "INFO: " (string/join " " message))]))

(defn error
  [& message]
  (apply log [(str "ERR : " (string/join " " message))]))

(defn send-message
  [message channel]
  (let [msg {:type "message"
             :channel channel
             :text message}]
    (send-message* msg)
    (when log-channel
      (info (str "->" (pr-str msg))))))

(defn special-symbol?
  [sym]
  (contains? '#{create-environment destroy-environment} sym))

(defn special-form?
  [text]
  (let [form (binding [*read-eval* false] (read-string text))]
    (when (and (list? form) (special-symbol? (first form)))
      form)))

(defconfig bot-name
  "clojurebot")

(def mention-regex
  #"\A(<@U.*> )(?:.|\n)*\z")

(defn mention?
  [text]
  (re-matches mention-regex text))

(defn remove-mention
  [text]
  (let [[_ m] (re-matches mention-regex text)]
    (subs text (count m))))

(def wrapped-regex
  #"\A`+(.*)`+\z")

(defn wrapped?
  [text]
  (re-matches wrapped-regex text))

(defn unwrap
  [text]
  (let [[_ text] (re-matches wrapped-regex text)]
    text))

(defn clean-text
  [text]
  (cond-> text
    (wrapped? text) unwrap
    (mention? text) remove-mention))

(defonce environments
  (atom {}))

(defn register-environment
  [user dependencies]
  (let [env (create-environment dependencies)]
    (swap! environments assoc user [env (pr-str dependencies)])))

(defmulti handle-special (comp ffirst list))

(defmethod handle-special 'create-environment
  [[_ dependencies] channel user]
  (register-environment user dependencies)
  (send-message (str "Created environment: " (pr-str dependencies)) channel))

(defmethod handle-special 'destroy-environment
  [_ channel user]
  (let [[env dependencies] (get @environments user)]
    (.close env)
    (swap! environments dissoc user)
    (send-message (str "Destroyed environment: " dependencies) channel)))

(defmethod handle-special :default
  [form channel channel user]
  (send-message (str "You have sent an unknown special form: " (pr-str form))
                channel))

(defn handle-normal
  [text channel user]
  (let [user-env (get @environments user)
        [user-env? [env dependencies]] (if user-env
                                         [true user-env]
                                         [false (get @environments :default)])
        [out err] (eval-in-environment env text)]
    (send-message
     (str (when user-env?
            (str "```;; Using " dependencies "```"
                 (when (or (seq out) (seq err))
                   "\n")))
          (when (seq out)
            (str "```"
                 out
                 "```"
                 (when (seq err)
                   "\n\n")))
          (when (seq err)
            (str "*```" err "```*")))
     channel)))

(defn handle-message
  [{:keys [text channel user] :as message}]
  (when (not= log-channel channel)
    (info (str "<-" (pr-str message)))
    (if-let [form (special-form? (clean-text text))]
      (handle-special form channel user)
      (if (string/starts-with? text "```")
        (handle-normal (clean-text text) channel user)
        (info "Ignoring message:" text)))))

(defn main
  "I don't do a whole lot ... yet."
  []
  (reset! nrepl-server (start-server :port 1980))
  (reset! slack-connection (slack/connect token))
  (register-environment :default '[[org.clojure/clojure "1.8.0"]])
  (let [{:keys [events-publication]} @slack-connection]
    (slack/sub-to-event events-publication :message #(#'handle-message %))
    (slack/sub-to-event events-publication :error prn)
    (println "Hello, World!")))
