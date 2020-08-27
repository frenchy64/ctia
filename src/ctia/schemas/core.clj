(ns ctia.schemas.core
  (:require [ctia.schemas.utils :as csutils]
            [ctim.schemas
             [bundle :as bundle]
             [common :as cos]
             [verdict :as vs]
             [vocabularies :as vocs]]
            [flanders
             [schema :as f-schema]
             [spec :as f-spec]]
            [schema-tools.core :as st]
            [schema.core :as s :refer [Bool Str]]))

(s/defschema APIHandlerServices
  "Maps of services available to routes"
  {:ConfigService {:get-config (s/=> s/Any s/Any)
                   :get-in-config (s/=>* s/Any
                                         [s/Any]
                                         [s/Any s/Any])}
   :HooksService {:apply-hooks (s/pred ifn?) ;;keyword varargs
                  :apply-event-hooks (s/=> s/Any s/Any)}
   :StoreService {:read-store (s/pred ifn?) ;;varags
                  :write-store (s/pred ifn?)} ;;varags
   :IAuth {:identity-for-token (s/=> s/Any s/Any)}
   :GraphQLService {:get-graphql (s/=> s/Any)}
   :IEncryption {:encrypt (s/=> s/Any s/Any)
                 :decrypt (s/=> s/Any s/Any)}})

(s/defschema DelayedRoutes
  "Function taking a map of services and returning routes
  (eg., return value of `entity-crud-routes`)"
  (s/=> s/Any APIHandlerServices))

(def base-stored-entity-entries
  {:id s/Str
   :owner s/Str
   :groups [s/Str]
   :created java.util.Date
   (s/optional-key :modified) java.util.Date})

;; TODO merge this with ctia.schemas.graphql.helpers
#_
(s/defschema RealizeFnServices
  "Maps of service functions available for realize-fns"
  {:ConfigService {:get-in-config (s/=>* s/Any
                                         [s/Any]
                                         [s/Any s/Any])}
   :StoreService {:read-store (s/pred ifn?)} ;;varags
   :GraphQLService {:get-or-update-type-registry (s/=> s/Any
                                                       (s/named s/Str
                                                                'name)
                                                       (s/named (s/=> s/Any s/Any)
                                                                'f))}})

;; TODO merge this with ctia.schemas.graphql.helpers
#_
(s/def GraphQLRuntimeOptions
  "A map of options to resolve a DelayedGraphQLValue"
  {:services RealizeFnServices})

(defn MaybeDelayedRealizeFnResult
  [a]
  (s/if fn?
    ;; delayed
    (s/=> a {s/Keyword {s/Keyword (s/pred fn?)}})
    ;; eager
    a))

(defn RealizeFnReturning [return]
  (s/=>* return
         [s/Any  ;; new-object
          s/Any  ;; id
          s/Any  ;; tempids
          s/Any  ;; owner
          s/Any] ;; groups
         [s/Any  ;; new-object
          s/Any  ;; id
          s/Any  ;; tempids
          s/Any  ;; owner
          s/Any  ;; groups
          s/Any])) ;; prev-object

(s/defschema MaybeDelayedRealizeFn
  (RealizeFnReturning (MaybeDelayedRealizeFnResult (s/pred map?))))

(s/defschema RealizeFn
  (RealizeFnReturning (s/pred map?)))

(s/defn MaybeDelayedRealizeFn->RealizeFn
  :- RealizeFn
  [realize-fn :- MaybeDelayedRealizeFn
   rt-opt #_:- #_GraphQLRuntimeOptions]
  (fn [& args]
    (let [maybe-delayed (apply realize-fn args)]
      (assert (or (fn? maybe-delayed)
                  (instance? graphql.schema.GraphQLType maybe-delayed))
              maybe-delayed)
      ;; TODO if the above assertion succeeds, we can probably use ifn? here.
      ;; TODO merge with ctia.schemas.graphql.helpers/resolve-with-rt-opt
      (if (fn? maybe-delayed)
        (maybe-delayed rt-opt)
        maybe-delayed))))

(s/defschema Entity
  (st/merge
   {:entity s/Keyword
    :plural s/Keyword
    :new-spec (s/either s/Keyword s/Any)
    :schema (s/protocol s/Schema)
    :partial-schema (s/protocol s/Schema)
    :partial-list-schema (s/protocol s/Schema)
    :stored-schema (s/protocol s/Schema)
    :partial-stored-schema (s/protocol s/Schema)
    :es-store s/Any
    :es-mapping {s/Any s/Any}}
   (st/optional-keys
    {:new-schema (s/protocol s/Schema)
     :route-context s/Str
     ;; at most one of :routes and :routes-from-services allowed.
     :routes DelayedRoutes
     :routes-from-services DelayedRoutes ;; deprecated, can be removed
     :tags [s/Str]
     :capabilities #{s/Keyword}
     :no-bulk? s/Bool
     :no-api? s/Bool
     :realize-fn MaybeDelayedRealizeFn})))

(s/defschema OpenCTIMSchemaVersion
  {(s/optional-key :schema_version) s/Str})

(s/defschema CTIAEntity
  (st/merge
   OpenCTIMSchemaVersion
   (st/optional-keys
    {:authorized_users [Str]
     :authorized_groups [Str]
     :owner s/Str
     :groups [s/Str]})))

(s/defschema CTIAStoredEntity
  (st/merge CTIAEntity
            base-stored-entity-entries))

(defmacro defschema [name-sym ddl spec-kw-ns]
  `(do
     (s/defschema ~name-sym
       (f-schema/->schema ~ddl))
     (f-spec/->spec ~ddl ~spec-kw-ns)))

(defmacro def-stored-schema
  [name-sym sch]
  `(do
     (s/defschema ~name-sym
       (csutils/recursive-open-schema-version
        (st/merge
         ~sch
         CTIAStoredEntity)))))

(defmacro def-advanced-acl-schema [{:keys [name-sym
                                           ddl
                                           spec-kw-ns
                                           open?]}]
  `(do
     (s/defschema ~name-sym
       (cond-> (csutils/recursive-open-schema-version
                (st/merge
                 (f-schema/->schema ~ddl)
                 CTIAEntity))
         ~open? st/open-schema))
     (f-spec/->spec ~ddl ~spec-kw-ns)))

(defmacro def-acl-schema [name-sym ddl spec-kw-ns]
  `(def-advanced-acl-schema {:name-sym ~name-sym
                             :ddl ~ddl
                             :spec-kw-ns ~spec-kw-ns}))

;; verdict

(def-acl-schema Verdict
  vs/Verdict
  "verdict")

;; Bundle

(def-acl-schema CTIMNewBundle
  bundle/NewBundle
  "new-bundle")

(def-acl-schema CTIMBundle
  bundle/Bundle
  "bundle")


;; Casebooks should be considered fully separate from CTIM
(s/defschema NewBundle
  (st/dissoc
   (csutils/recursive-open-schema-version CTIMNewBundle) :casebooks))

(s/defschema Bundle
  (st/dissoc
   (csutils/recursive-open-schema-version CTIMBundle) :casebooks))

;; common

(s/defschema TLP
  (f-schema/->schema
   cos/TLP))

(defschema Observable
  cos/Observable
  "common.observable")

(defschema Reference
  cos/Reference
  "common.reference")

(defschema ID
  cos/ID
  "common.id")

(def TransientID s/Str)

(s/defschema TempIDs
  "Mapping table between transient and permanent IDs"
  {TransientID ID})

(s/defschema StatusInfo
  "Status information for a specific instance of CTIA"
  {:status (s/enum :ok :error :unknow)})

(s/defschema VersionInfo
  "Version information for a specific instance of CTIA"
  {:base Str
   :ctim-version Str
   :ctia-build Str
   :beta Bool
   :ctia-config Str
   :ctia-supported_features [Str]})

;; vocabularies

(defschema ObservableTypeIdentifier
  vocs/ObservableTypeIdentifier
  "vocab.observable-type-id")
