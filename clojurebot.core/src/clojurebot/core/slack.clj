;;;; Copyright © Paul Stadig.  All rights reserved.
;;;;
;;;; This Source Code Form is subject to the terms of the Mozilla Public
;;;; License, v. 2.0.  If a copy of the MPL was not distributed with this file,
;;;; You can obtain one at http://mozilla.org/MPL/2.0/.
;;;;
;;;; This Source Code Form is "Incompatible With Secondary Licenses", as defined
;;;; by the Mozilla Public License, v. 2.0.
(ns clojurebot.core.slack
  (:require
   [clojure.string :as string]
   [slack-rtm.core :as slack]))

(def mention-regex
  #"\A(<@U.*> )(?:.|\n)*\z")

(defn mention?
  [text]
  (re-matches mention-regex text))

(defn remove-mention
  [text]
  (let [[_ m] (re-matches mention-regex text)]
    (subs text (count m))))

(defn wrapped?
  [text]
  (and (string/starts-with? text "```")
       (string/ends-with? text "```")))

(defn unwrap
  [text]
  (subs text 3 (- (count text) 3)))

(defn clean-text
  [text]
  (cond-> text
    (wrapped? text) unwrap
    (mention? text) remove-mention))

(defn send-message*
  [{:keys [dispatcher]} message]
  (slack/send-event dispatcher message))

(defn start
  [token handler-constructor]
  (let [{:keys [events-publication] :as conn} (slack/connect token)]
    (slack/sub-to-event events-publication :message (handler-constructor conn))
    conn))
