(ns ctia.test-helpers.http
  (:require
   [clojure.string :as string]
   [clj-momo.test-helpers.http-assert-1 :as mthh]
   [ctia.test-helpers.core :as th]))

(def api-key "45c1f5e3f05d0")

(defn doc-id->rel-url
  "given a doc id (url) make a relative url for test queries"
  [doc-id]
  (when doc-id
    (string/replace doc-id #".*(?=ctia)" "")))

;; TODO thread app arg through these functions
(def test-post
  (mthh/with-port-fn-and-api-key th/get-http-port api-key mthh/test-post))

(def assert-post
  (mthh/with-port-fn-and-api-key th/get-http-port api-key mthh/assert-post))

(def test-put
  (mthh/with-port-fn-and-api-key th/get-http-port api-key mthh/test-put))

(def test-get
  (mthh/with-port-fn-and-api-key th/get-http-port api-key mthh/test-put))

(def test-get-list
  (mthh/with-port-fn-and-api-key th/get-http-port api-key mthh/test-get-list))

(def test-delete
  (mthh/with-port-fn-and-api-key th/get-http-port api-key mthh/test-delete))
