(ns ctia.flows.hooks-service-core
  "Handle hooks ([Cf. #159](https://github.com/threatgrid/ctia/issues/159))."
  (:require [clojure.tools.logging :as log]
            [ctia.flows.hooks.event-hooks :as event-hooks]
            [ctia.flows.hook-protocol :as prot]))

(defn- doc-list [& s]
  (with-meta [] {:doc (apply str s)}))

(def empty-hooks
  {:before-create (doc-list "`before-create` hooks are triggered on"
                            " create routes before the entity is saved in the DB.")
   :after-create (doc-list "`after-create` hooks are called after an entity was created.")
   :before-update (doc-list "`before-update` hooks are triggered on"
                            " update routes before the entity is saved in the DB.")
   :after-update (doc-list "`after-update` hooks are called after an entity was updated.")
   :before-delete (doc-list "`before-delete` hooks are called before an entity is deleted.")
   :after-delete (doc-list "`after-delete` hooks are called after an entity is deleted.")
   :event (doc-list "`event` hooks are called with an event during any CRUD activity.")})

(defn reset-hooks! [{:keys [hooks] :as _context_} get-in-config]
  (reset! hooks
          (-> empty-hooks
              (event-hooks/register-hooks get-in-config))))

(defn add-hook!
  "Add a `Hook` for the hook `hook-type`"
  [{:keys [hooks] :as _context_} hook-type hook]
  (swap! hooks update hook-type conj hook))

(defn add-hooks!
  "Add a list of `Hook` for the hook `hook-type`"
  [{:keys [hooks] :as _context_} hook-type hook-list]
  (swap! hooks update hook-type into hook-list))

(defn init-hooks!
  "Initialize all hooks"
  [{:keys [hooks] :as _context_}]
  (doseq [hook-list (vals @hooks)
          hook hook-list]
    (prot/init hook))
  @hooks)

(defn destroy-hooks!
  "Should call all destructor for each hook in reverse order."
  [{:keys [hooks] :as _context_}]
  (doseq [hook-list (vals @hooks)
          hook (reverse hook-list)]
    (prot/destroy hook)))
(defn apply-hooks
  "Apply the registered hooks for a given hook-type to the passed in data.
   Data may be an entity (or an event) and a previous entity.  Accepts
  read-only?, in which case the result of the hooks do not change the result.
  In any hook returns nil, the result is ignored and the input entity is kept."
  [{:keys [hooks] :as context}
   {:keys [hook-type
           entity
           prev-entity
           read-only?]}]
  (loop [[hook & more-hooks :as hooks] (get @hooks hook-type)
         result entity]
    (if (empty? hooks)
      result
      (let [handle-result (prot/handle hook result prev-entity)]
        (if (or read-only?
                (nil? handle-result))
          (recur more-hooks result)
          (recur more-hooks handle-result))))))

(defn apply-event-hooks [context event]
  (apply-hooks context
               {:hook-type :event
                :entity event
                :read-only? true}))

(defn shutdown!
  "Normally this should not be called directly since init! registers a
  shutdown hook"
  [context]
  (destroy-hooks! context))

(defn init [context]
  (assoc context :hooks (atom empty-hooks)))

(defn start
  "Initialize all hooks"
  [{:keys [hooks] :as context} get-in-config]
  (reset-hooks! context get-in-config)
  (init-hooks! context)
  (log/info "Hooks Initialized: " (pr-str @hooks))
  context)

(defn stop
  "Should call all destructor for each hook in reverse order."
  [context]
  (shutdown! context)
  (dissoc context :hooks))
