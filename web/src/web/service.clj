(ns web.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]
            [web.rpc :refer [defservice]]
            [clojure.core.match :refer [match]]))

(defservice fib-service "fibs.get")
(defservice fact-service "fact.get")

(defn render-service-response [response]
  (match response
    [:ok n] n
    [:out-of-bounds _] "More than too much"
    [:timeout msg] msg
    _ "Unknown error"))

(defn number-info [request]
  (let [n (Integer/parseInt (get-in request [:path-params :n]))
        fib-promise (fib-service n)
        fact-promise (fact-service n)
        fib (deref fib-promise 100 [:timeout "No fib response available"])
        fact (deref fact-promise 100 [:timeout "No fact response available"])]
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
