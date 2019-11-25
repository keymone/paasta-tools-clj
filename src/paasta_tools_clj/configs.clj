(ns paasta-tools-clj.configs
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [paasta-tools-clj.defaults :refer [SOA_DIR SYS_DIR]]))

(defn load-path! [store path]
  (swap! store merge (json/parse-stream-strict (io/reader path))))

(defn load-all! [store dir]
  (dorun (map #(load-path! store %) (.listFiles (io/file dir)))))

(defn load! [store domain dir]
  (let [path (str dir "/" domain ".json")]
    (if (.exists (io/file path))
      (load-path! store path)
      (do
        (.println *err* (str "WARNING: loading all configs, consider adding "
                             "domain hints for " domain " in " dir))
        (load-all! store dir)))))

(defn- make-store [store default-dir hints]
  (fn [key & {:keys [dir] :or {dir default-dir}}]
    (if-not (contains? @store key)
      (load! store (or (hints key) key) dir))
    (@store key)))

(def soa-get (make-store (atom {}) SOA_DIR {}))
(def sys-get (make-store (atom {}) SYS_DIR {}))
