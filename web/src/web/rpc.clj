(ns web.rpc
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.queue :as lq]
            [langohr.basic :as lb]))

(def rabbit-connection (rmq/connect))
(def rabbit-channel (lch/open rabbit-connection))

(defmacro defservice [name queue-name]
  (let [queue-name-sym (symbol (str name "-queue-name"))
        pending-rpc-calls-sym (symbol (str name "-pending-rpc-calls"))
        response-queue-name-sym (symbol (str name "-response-queue-name"))
        response-queue-name (str queue-name "." (java.util.UUID/randomUUID))]
    `(do
       (def ~queue-name-sym ~queue-name)
       (def ~pending-rpc-calls-sym (atom {}))
       (def ~response-queue-name-sym ~response-queue-name)
       (lq/declare rabbit-channel
                   ~queue-name-sym
                   {:exclusive false :auto-delete true})
       (lq/declare rabbit-channel
                   ~response-queue-name-sym
                   {:exclusive true :auto-delete true})
       (lc/subscribe rabbit-channel
                     ~response-queue-name-sym
                     (fn [ch# {:keys [~'correlation-id]} ^"bytes" payload#]
                       (when-let [response-promise# (@~pending-rpc-calls-sym ~'correlation-id)]
                         (deliver response-promise# (clojure.edn/read-string (String. payload#)))
                         (swap! ~pending-rpc-calls-sym dissoc ~'correlation-id)))
                     {:auto-ack true})
       (defn ~name [arg#]
         (let [response# (promise)
               correlation-id# (str (java.util.UUID/randomUUID))]
           (swap! ~pending-rpc-calls-sym assoc correlation-id# response#)
           (lb/publish rabbit-channel "" ~queue-name-sym (pr-str arg#)
                       {:reply-to ~response-queue-name-sym
                        :correlation-id correlation-id#})
           response#)))))
