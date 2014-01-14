(ns dojo.core
  (:require
    [ring.util.response :refer [response]]
    [chord.http-kit :refer [with-channel]]
    [clojure.core.async :refer [<! >! put! close! go-loop]]
    [compojure.core :refer [defroutes GET routes]]
    [compojure.handler :refer [api]]
    [compojure.route :refer [resources]]
    [hiccup.page :refer [html5 include-js]]
    [hiccup.element :refer [javascript-tag]])
  (:import (org.python.util PythonInterpreter)))

(defonce python-interp (PythonInterpreter.))

#_ (defn do-python [message]
  (try
    (if-let [[_ name] (re-find #"^def ([A-Za-z]+)" message)]
      (do
        (.exec python-interp message)
        (format  "Declared: %s -> %s" message name))
      (format "Evaluated: %s -> %s" message (str (.eval python-interp message))))
    (catch Exception e (format "Error in %s: %s" message (.toString e)))))

(defn do-python [message]
  (try
    (if-let [[_ _ fname vname] (re-find #"^\s*(def\s+(\w+)|(\w+)\s*=)" message)]
      (do
        (.exec python-interp message)
        (format  "Declared: %s -> %s" message (or fname vname)))
      (format "Evaluated: %s -> %s" message (str (.eval python-interp message))))
    (catch Exception e (format "Error in %s: %s" message (.toString e)))))

(defn index-page []
  (html5
   [:head
    [:title "London Clojure Dojo January 2014: Python Interpreter"]]
    [:body
      [:div#app]
      (include-js "//fb.me/react-0.8.0.js") ; only required in dev build
      (include-js "/out/goog/base.js") ; only required in dev build
      (include-js "/js/dojo.js")
      (javascript-tag "goog.require('dojo.client');") ; only required in dev build
      ]))

(defn ws-handler [req]
  (with-channel req ws
    (println "Opened connection from" (:remote-addr req))
    (go-loop []
      (when-let [{:keys [message]} (<! ws)]
        (println "Message received:" message)
        (>! ws (do-python message))
        (recur)))))

(defn app-routes []
  (routes
    (GET "/" [] (response (index-page)))
    (GET "/ws" [] ws-handler)
    (resources "/js" {:root "js"})
    (resources "/out" {:root "out"}) ; only required in dev build
    ))

(defn webapp []
  (-> (app-routes)
      api))


(re-find #"^\s*(def (\w+)|(\w+)\s*=)" "A")
