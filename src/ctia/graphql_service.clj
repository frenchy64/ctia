(ns ctia.graphql-service
  (:require [ctia.graphql-service-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.services :refer [service-context]]))

(defprotocol GraphQLService
  (get-graphql [this] "Returns an instance of graphql.GraphQL")
  ;; Type registry to avoid any duplicates when using new-object
  ;; or new-enum. Contains a map with types indexed by name
  (get-or-update-type-registry
    [this name f]
    "If name exists in registry, return existing mapping. Otherwise conj
    {name (f)} to registry, and return (f)."))

(defn- GraphQLService-map [this]
  {:get-graphql (partial get-graphql this)
   :get-or-update-type-registry (partial get-or-update-type-registry this)})

(tk/defservice graphql-service
  GraphQLService
  [StoreService]
  (start [this context] (core/start context {:StoreService StoreService
                                             :GraphQLService (GraphQLService-map this)}))
  (get-graphql [this] (core/get-graphql (service-context this)))
  (get-or-update-type-registry [this name f] (core/get-or-update-type-registry (service-context this)
                                                                               name
                                                                               f)))
