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
   [cider.nrepl :refer [cider-nrepl-handler]]
   [clojure.string :as string]
   [clojure.tools.nrepl.server :refer [start-server]]
   [clojurebot.core.evaluator :as eval]
   [clojurebot.core.slack :as slack]
   [clojurebot.core.slack.log :as log]
   [outpace.config :refer [defconfig defconfig!]]))

(defn send-message
  [conn message channel]
  (let [msg {:type "message"
             :channel channel
             :text message}]
    (slack/send-message* conn msg)
    (log/info conn (str "->" (pr-str msg)))))

(defn special-symbol?
  [sym]
  (contains? '#{create-environment destroy-environment} sym))

(defn special-form?
  [text]
  (let [form (binding [*read-eval* false] (read-string text))]
    (when (and (list? form) (special-symbol? (first form)))
      form)))

(defonce environments
  (atom {}))

(defn create-environment
  [user dependencies]
  (let [env (eval/make-environment dependencies)]
    (swap! environments assoc user [env (pr-str dependencies)])))

(defmulti handle-special (comp ffirst list))

(defmethod handle-special 'create-environment
  [[_ dependencies] conn channel user]
  (create-environment user dependencies)
  (send-message conn
                (str "```;; Created environment: " (pr-str dependencies) "```")
                channel))

(defmethod handle-special 'destroy-environment
  [_ conn channel user]
  (let [[env dependencies] (get @environments user)]
    (.close env)
    (swap! environments dissoc user)
    (send-message conn
                  (str "```;; Destroyed environment: " dependencies "```")
                  channel)))

(defmethod handle-special :default
  [form conn channel user]
  (send-message conn
                (str "```;; You have sent an unknown special form: "
                     (pr-str form)
                     "```")
                channel))

(defn handle-normal
  [text conn channel user]
  (let [user-env (get @environments user)
        [user-env? [env dependencies]] (if user-env
                                         [true user-env]
                                         [false (get @environments :default)])
        [out err] (eval/eval-in-environment env text)
        [out err] (if user-env?
                    (cond
                      (or (seq out) (empty? err))
                      [(str ";; Using " dependencies "\n" out) err]
                      :else
                      [out (str ";; Using " dependencies "\n" err)])
                    [out err])]
    (send-message conn
                  (str (when (seq out)
                         (str "```" out "```"
                              (when (seq err)
                                "\n\n")))
                       (when (seq err)
                         (str "*```" err "```*")))
                  channel)))

(defn handler-constructor
  [conn]
  (fn handle-message [{:keys [text channel user] :as message}]
    (when (string/starts-with? text "```")
      (log/info conn (str "<-" (pr-str message)))
      (let [text (slack/clean-text text)]
        (if-let [form (special-form? text)]
          (handle-special form conn channel user)
          (handle-normal text conn channel user))))))

(defconfig default-dependencies
  '[[org.clojure/clojure "1.8.0"]])

(defconfig! token)

(defn main
  "I don't do a whole lot ... yet."
  []
  (start-server :port 1980 :handler cider-nrepl-handler)
  (create-environment :default default-dependencies)
  (slack/start token handler-constructor))
