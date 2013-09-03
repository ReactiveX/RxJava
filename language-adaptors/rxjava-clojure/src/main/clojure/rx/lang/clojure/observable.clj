(in-ns 'rx.lang.clojure.observable)
(clojure.core/require 'clojure.core
                      '[rx.lang.clojure.interop :as interop])

(interop/wrap-class rx.Observable)
