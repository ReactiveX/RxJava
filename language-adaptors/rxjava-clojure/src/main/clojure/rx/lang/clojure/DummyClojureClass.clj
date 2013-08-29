(ns rx.lang.clojure.DummyClojureClass)

(defn hello-world [username]
  (println (format "Hello, %s" username)))

(hello-world "world")
