(ns web.rpc
  (:require [democracyworks.kehaar :as k]
            [clojure.core.async :as async]
            [langohr.core :as rmq]
            [langohr.channel :as lch]))

(def rabbit-connection (rmq/connect))
(def rabbit-channel (lch/open rabbit-connection))

(def fibs-channel (async/chan))
(def fact-channel (async/chan))

(def fibs-request (k/ch->response-fn fibs-channel))
(def fact-request (k/ch->response-fn fact-channel))

(defn initialize []
  (k/wire-up-service rabbit-channel "fibs.get" fibs-channel)
  (k/wire-up-service rabbit-channel "fact.get" fact-channel))
