(ns fact.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.queue :as lq]
            [langohr.basic :as lb]))

(def fact-queue-name "fact.get")

(defn fact [n]
  (try
    [:ok (reduce * 1 (range 1 (inc n)))]
    (catch java.lang.ArithmeticException _
      [:out-of-bounds n])
    (catch Exception _
      [:error "Unhandled exception"])))

(defn -main [& _]
  (let [connection (rmq/connect)
        channel (lch/open connection)]
    (lq/declare channel
                fact-queue-name
                {:exclusive false :auto-delete true})
    (lc/subscribe channel
                  fact-queue-name
                  (fn [ch {:keys [reply-to correlation-id]} ^bytes payload]
                    (let [request (String. payload "UTF-8")
                          n (edn/read-string request)]
                      (lb/publish ch "" reply-to (pr-str (fact n)) {:correlation-id correlation-id})))
                  {:auto-ack true})))
