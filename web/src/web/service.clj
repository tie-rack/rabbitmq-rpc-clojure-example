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

(defn render-service-response [deferred]
  (-> deferred
      (d/timeout! 100 "Too slow!")
      (d/catch clojure.lang.ExceptionInfo (fn [e] [:error (-> e ex-data :type)]))
      deref))

(defn number-info [request]
  (let [n (Integer/parseInt (get-in request [:path-params :n]))
        fib (get-fib n)
        fact (get-fact n)
        sum (-> (d/zip fib fact)
                (d/chain #(apply + %)))]
    (ring-resp/response (str "fib: " (render-service-response fib)
                             "\nfact: " (render-service-response fact)
                             "\nsum: " (render-service-response sum)))))

(defroutes routes
  [[["/number-info/:n"
     ^:constraints {:n #"[0-9]+"}
     {:get number-info}]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
