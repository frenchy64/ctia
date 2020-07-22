(ns ctia.properties.routes
  (:require [compojure.api.sweet :refer :all]
            [ctia.properties :refer [get-global-properties]]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(defroutes properties-routes
  (context "/properties" []
           :tags ["Properties"]
           :summary "Configured properties"
           :capabilities :developer
           (GET "/" []
                (ok @(get-global-properties)))))
