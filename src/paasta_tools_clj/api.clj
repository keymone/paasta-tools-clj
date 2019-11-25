(ns paasta-tools-clj.api
  (:require [martian.core :as martian]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(def swagger
  (let [swagger-spec (json/parse-string
                      (slurp (clojure.java.io/resource "swagger.json")))]
    (martian/bootstrap-swagger
     (swagger-spec "basePath")
     swagger-spec
     {:name ::perform-request
      :leave (fn [{:keys [request] :as ctx}]
               (assoc ctx :response (http/request request)))})))
