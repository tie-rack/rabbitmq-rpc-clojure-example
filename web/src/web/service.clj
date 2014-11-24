(ns web.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]
            [web.rpc :as rpc]))

(def get-fib (rpc/service "fibs.get"))
(def get-fact (rpc/service "fact.get"))

(defn number-info [request]
  (let [n (Integer/parseInt (get-in request [:path-params :n]))
        fib (-> n get-fib (deref 100 :fib-timeout))
        fact (-> n get-fact (deref 100 :fact-timeout))]
    (ring-resp/response (str "fib: " fib "\nfact: " fact))))

(defroutes routes
  [[["/number-info/:n"
     ^:constraints {:n #"[0-9]+"}
     {:get number-info}]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
