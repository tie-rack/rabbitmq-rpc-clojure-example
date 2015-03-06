(ns web.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :refer [defbefore]]
            [ring.util.response :as ring-resp]
            [web.rpc :as rpc]
            [clojure.core.async :as async]
            [clojure.core.match :refer [match]]))

(defn render-service-response [response]
  (match response
    [:ok n] n
    [:out-of-bounds _] "More than too much"
    [:timeout msg] msg))

(defn get-or-timeout [ch timeout message]
  (async/go
    (or (first (async/alts! [ch (async/timeout timeout)]))
        [:timeout message])))

(defn number-info [request]
  (let [fib-channel (:fib-channel request)
        fact-channel (:fact-channel request)
        fib (get-or-timeout fib-channel 1000 "No fibs available!")
        fact (get-or-timeout fact-channel 1000 "No fact available!")]
    (ring-resp/response (str "fib: " (render-service-response (async/<!! fib))
                             "\nfact: " (render-service-response (async/<!! fact))))))

(defbefore parse-n
  [ctx]
  (let [n (Integer/parseInt (get-in ctx [:request :path-params :n]))]
    (assoc-in ctx [:request :n] n)))

(defbefore request-fib
  [ctx]
  (let [n (get-in ctx [:request :n])
        fib-response-channel (rpc/fibs-request n)]
    (assoc-in ctx [:request :fib-channel] fib-response-channel)))

(defbefore request-fact
  [ctx]
  (let [n (get-in ctx [:request :n])
        fact-response-channel (rpc/fact-request n)]
    (assoc-in ctx [:request :fact-channel] fact-response-channel)))

(defroutes routes
  [[["/number-info/:n"
     ^:constraints {:n #"[0-9]+"}
     ^:interceptors [parse-n
                     request-fib
                     request-fact]
     {:get number-info}]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
