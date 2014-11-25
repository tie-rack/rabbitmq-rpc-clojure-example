(ns fibs.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.queue :as lq]
            [langohr.basic :as lb]))

(def fibs-queue-name "fibs.get")

(defn fib [n]
  (let [fib-n (loop [a 0 b 1 n n]
                (if (zero? n)
                  a
                  (recur b (+ a b) (dec n))))]
    fib-n))

(defn ->resp [n]
  (try [:ok (fib n)]
       (catch java.lang.ArithmeticException _
         [:error :out-of-bound])
       (catch Exception _
         [:error :unknown])))

(defn -main [& _]
  (let [connection (rmq/connect)
        channel (lch/open connection)]
    (lq/declare channel
                  fibs-queue-name
                  {:exclusive false :auto-delete true})
    (lc/subscribe channel
                  fibs-queue-name
                  (fn [ch {:keys [reply-to correlation-id]} ^bytes payload]
                    (let [request (String. payload "UTF-8")
                          n (edn/read-string request)]
                      (lb/publish ch "" reply-to (pr-str (->resp n)) {:correlation-id correlation-id})))
                  {:auto-ack true})))
