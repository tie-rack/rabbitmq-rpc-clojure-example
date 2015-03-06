(ns fibs.core
  (:gen-class)
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.queue :as lq]
            [democracyworks.kehaar :as kehaar]))

(defn fib [n]
  (println "Calculating fib" n)
  (try
    (let [fib-n (loop [a 0 b 1 n n]
                  (if (zero? n)
                    a
                    (recur b (+ a b) (dec n))))]
      [:ok fib-n])
    (catch java.lang.ArithmeticException _
      [:out-of-bounds n])))

(defn -main [& _]
  (let [connection (rmq/connect)
        channel (lch/open connection)
        fibs-queue-name "fibs.get"]
    (lq/declare channel
                  fibs-queue-name
                  {:exclusive false :auto-delete true})
    (lc/subscribe channel
                  fibs-queue-name
                  (kehaar/simple-responder fib)
                  {:auto-ack true})))
