(ns web.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]
            [web.rpc :as rpc]
            [clojure.core.match :refer [match]]
            [manifold.deferred :as d]))

(def get-fib (rpc/service "fibs.get"))
(def get-fact (rpc/service "fact.get"))

(defn resp->body [response]
  (match response
         [:ok n] n
         [:out-of-bounds _] "More than too much"))

(defn render-service-response [deferred]
  (-> deferred
      (d/chain resp->body)
      (d/timeout! 100 "Too slow!")
      (d/catch Exception (fn [e] e))
      deref))

(defn number-info [request]
  (let [n (Integer/parseInt (get-in request [:path-params :n]))
        fib (get-fib n)
        fact (get-fact n)]
    (ring-resp/response (str "fib: " (render-service-response fib)
                             "\nfact: " (render-service-response fact)))))

(defroutes routes
  [[["/number-info/:n"
     ^:constraints {:n #"[0-9]+"}
     {:get number-info}]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
