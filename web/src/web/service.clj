(ns web.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]
            [web.rpc :refer [defservice]]
            [web.time :refer [timer]]
            [clojure.core.match :refer [match]]))

(defservice fib-service "fibs.get")
(defservice fact-service "fact.get")

(defn render-service-response [response]
  (match response
    [:ok n] n
    [:out-of-bounds _] "More than too much"
    [:timeout msg] msg
    _ "Unknown error"))

(defn render-whole-service-response [response]
  (match response
    [:ok n] [n 200]
    [:out-of-bounds _] ["More than too much" 400]
    [:timeout msg] [msg 500]
    _ ["Unknown error" 500]))

(defn number-info [request]
  (let [n (Integer/parseInt (get-in request [:path-params :n]))
        fib-promise (fib-service n)
        fact-promise (fact-service n)
        fib (deref fib-promise 100 [:timeout "No fib response available"])
        fact (deref fact-promise 100 [:timeout "No fact response available"])]
    (ring-resp/response (str "fib: " (render-service-response fib)
                             "\nfact: " (render-service-response fact)))))

(defn time-service-call [service-fn arg]
  (timer
   (-> (service-fn arg)
       (deref 100 [:timeout (str "No response available")]))))

(defn make-api-interceptor [service-fn request-param render-fn]
  (fn [request]
    (let [num (Integer/parseInt (get-in request request-param))
          [an-answer time] (time-service-call service-fn num)
          [final-response status] (render-fn an-answer)]
      (-> (ring-resp/response (str "answer: " final-response ", time: " time))
          (ring-resp/status status)))))

(def fibs-interceptor (make-api-interceptor fib-service [:path-params :n] render-whole-service-response))
(def fact-interceptor (make-api-interceptor fact-service [:path-params :n] render-whole-service-response))

(defroutes routes
  [[["/fibs/:n" {:get fibs-interceptor}]
    ["/fact/:n" {:get fact-interceptor}]
    ["/number-info/:n"
     ^:constraints {:n #"[0-9]+"}
     {:get number-info}]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
