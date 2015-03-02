(ns fact.core
  (:gen-class)
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.queue :as lq]
            [democracyworks.kehaar :as kehaar]))

(def fact-queue-name "fact.get")

(defn fact [n]
  (try
    [:ok (reduce * 1 (range 1 (inc n)))]
    (catch java.lang.ArithmeticException _
      [:out-of-bounds n])))

(defn -main [& _]
  (let [connection (rmq/connect)
        channel (lch/open connection)]
    (lq/declare channel
                fact-queue-name
                {:exclusive false :auto-delete true})
    (lc/subscribe channel
                  fact-queue-name
                  (kehaar/simple-responder fact)
                  {:auto-ack true})))
