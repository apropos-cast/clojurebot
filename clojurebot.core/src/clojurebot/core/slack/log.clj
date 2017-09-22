;;;; Copyright © Paul Stadig.  All rights reserved.
;;;;
;;;; This Source Code Form is subject to the terms of the Mozilla Public
;;;; License, v. 2.0.  If a copy of the MPL was not distributed with this file,
;;;; You can obtain one at http://mozilla.org/MPL/2.0/.
;;;;
;;;; This Source Code Form is "Incompatible With Secondary Licenses", as defined
;;;; by the Mozilla Public License, v. 2.0.
(ns clojurebot.core.slack.log
  (:require
   [clojure.string :as string]
   [clojurebot.core.slack :as slack]
   [outpace.config :refer [defconfig]]))

(defconfig log-channel)

(defn log
  [conn & message]
  (when log-channel
    (slack/send-message* conn
                         {:type "message"
                          :channel log-channel
                          :text (string/join " " message)
                          :mrkdwn false})))

(defn info
  [conn & message]
  (apply log [conn (str "INFO: " (string/join " " message))]))

(defn error
  [conn & message]
  (apply log [conn (str "ERR : " (string/join " " message))]))
