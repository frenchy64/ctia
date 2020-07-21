(ns ctia.encryption.default-core
  (:require [clojure.string :as string]
            [ctia.properties :as p]
            [lock-key.core :refer [decrypt-from-base64
                                   encrypt-as-base64]]))

(defn encryption-key
  [{:keys [secret key]}]
  (when-not (or secret (:filepath key))
    (throw (ex-info "no secret or key filepath provided" {})))
  (or secret
      (some-> (:filepath key)
              slurp
              string/trim)))

(defn init [context]
  (let [props (get-in @p/properties [:ctia :encryption])
        secret (encryption-key props)]
    (assoc context
           :encrypt-fn #(encrypt-as-base64 % secret)
           :decrypt-fn #(decrypt-from-base64 % secret))))

(defn start [context]
  context)

(defn stop [context]
  context)

(defn decrypt [{:keys [decrypt-fn]} src]
  (decrypt-fn src))

(defn encrypt [{:keys [encrypt-fn]} src]
  (encrypt-fn src))
