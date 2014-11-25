(ns web.rpc
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.queue :as lq]
            [langohr.basic :as lb]
            [manifold.deferred :as d]
            [clojure.core.match :refer [match]]))

(def rabbit-connection (rmq/connect))
(def rabbit-channel (lch/open rabbit-connection))

(defn service [queue-name]
  (let [pending-rpc-calls (atom {})
        response-queue-name (str queue-name "." (java.util.UUID/randomUUID))]
    (lq/declare rabbit-channel
                queue-name
                {:exclusive false :auto-delete true})
    (lq/declare rabbit-channel
                response-queue-name
                {:exclusive true :auto-delete true})
    (lc/subscribe rabbit-channel
                  response-queue-name
                  (fn [ch {:keys [correlation-id]} ^bytes payload]
                    (when-let [response-promise (@pending-rpc-calls correlation-id)]
                      (let [resp (clojure.edn/read-string (String. payload))]
                        (match resp
                               [:ok r] (d/success! response-promise r)
                               [:error e] (d/error! response-promise (ex-info (str e) {:type e}))))
                      (swap! pending-rpc-calls dissoc correlation-id)))
                  {:auto-ack true})
    (fn [arg]
      (let [response (d/deferred)
            correlation-id (str (java.util.UUID/randomUUID))]
        (swap! pending-rpc-calls assoc correlation-id response)
        (lb/publish rabbit-channel "" queue-name (pr-str arg)
                    {:reply-to response-queue-name
                     :correlation-id correlation-id})
        response))))
