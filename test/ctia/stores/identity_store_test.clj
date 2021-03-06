(ns ctia.stores.identity-store-test
  (:require [clj-momo.test-helpers.core :as mth]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [ctia.store :as store]
            [ctia.store-service :as store-svc]
            [ctia.test-helpers
             [core :as helpers]
             [store :refer [test-for-each-store-with-app]]]))

(use-fixtures :once mth/fixture-schema-validation)

(deftest test-read-identity
  (test-for-each-store-with-app
   (fn [app]
    (let [{:keys [read-store write-store]} (-> (helpers/get-service-map app :StoreService)
                                               store-svc/lift-store-service-fns)]
     (testing "Reading not-found identity returns nil"

       (write-store :identity
                          (fn [store]
                            (store/create-identity store {:login "bar"
                                                          :groups ["foogroup"]
                                                          :capabilities #{:read-actor}
                                                          :role "admin"})))
       (is (nil? (read-store :identity
                                   (fn [store]
                                     (store/read-identity store "foo"))))))))))
