(ns web.time
  "Timer related functions since the built in time is more of a REPL
  tool."
  (:import (clojure.lang IObj)))

(defmacro timer
  "Evaluate the expr and returns a vector of the value and the elapsed
  time."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     [ret# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]))
