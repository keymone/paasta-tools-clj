(ns paasta-tools-clj.metastatus
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [martian.core :as martian]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [paasta-tools-clj.defaults :as defaults]
            [paasta-tools-clj.configs :refer [soa-get sys-get]]
            [paasta-tools-clj.api :refer [swagger] :reload true]))

(def opts
  [["-h" "--help" "Print this message"]
   ["-v" "--verbose"
    (str "Print out more output regarding the state of the cluster. "
         "Multiple v options increase verbosity. Maximum is 3.")
    :default 0 :update-fn inc]
   ["-c" "--cluster CLUSTER[,CLUSTER2,...]"
    (str "A comma separated list of clusters to view. Defaults to view"
         "all clusters. Try: --clusters norcal-prod,nova-prod")
    :parse-fn #(clojure.string/split % #",")]
   ["-d" "--soa-dir DIR" "define a different soa config directory"
    :default-fn (fn [& _] (or (System/getenv "SOA_DIR") defaults/SOA_DIR))]
   ["-C" "--sys-dir DIR" "define a different soa config directory"
    :default-fn (fn [& _] (or (System/getenv "SYS_DIR") defaults/SYS_DIR))]
   ["-a" "--autoscaling-info" "Show cluster autoscaling info, implies -vv"
    :default false]
   [nil "--use-mesos-cache" "Use Mesos cache for state.json and frameworks"
    :default false]
   ["-g" "--groupings GROUP[,GOUP2,...]"
    (str "Group resource information of slaves grouped by attribute."
         "Note: This is only effective with -vv")
    :default-fn (constantly ["region"])
    :parse-fn #(clojure.string/split % #",")]
   ["-s" "--service SERVICE"
    (str "Show how many of a given service instance can be run on a cluster "
         "slave. Note: This is only effective with -vvv and --instance must "
         "also be specified")]
   ["-i" "--instance INSTANCE"
    (str "Show how many of a given service instance can be run on a "
         "cluster slave. Note: This is only effective with -vvv and "
         "--service must also be specified")]])

(defn usage [options-summary errors-summary]
  (if errors-summary
    (println errors-summary)
    (print
     (str
      "Display the status for an entire PaaSTA cluster\n\n"
      "'paasta metastatus' is used to get the vital statistics about a PaaSTA "
      "cluster as a whole. This tool is helpful when answering the question: "
      "'Is it just my service or the whole cluster that is broken?'\n\n"
      "metastatus operates by querying Paasta API for selected cluster.\n\n")))
  (println options-summary)
  (println
   "\nThe metastatus command may time out during heavy load. When that happens "
   "users may execute the ssh command directly, in order to bypass the timeout.")
  (System/exit (if errors-summary 1 0)))

(defn metastatus-cmd-args
  [{:keys [autoscaling-info verbose groupings use-mesos-cache]}]
  (let [v-times (max verbose (if autoscaling-info 2 0))
        timeout (if (= 0 v-times) 20 120)]
    [(cond-> []
       autoscaling-info (conj "-a")
       (> 0 v-times) (conj (apply str "-" (take v-times (cycle "v"))))
       groupings (conj "-g" (clojure.string/join "," groupings))
       use-mesos-cache (conj "--use-mesos-cache"))
     timeout]))

(defn metastatus [options arguments]
  (let [api-endpoints (sys-get "api_endpoints" :dir (:sys-dir options))
        clusters (or (:cluster options)
                     (sys-get "clusters" :dir (:sys-dir options)))
        [cmd-args timeout] (metastatus-cmd-args options)
        request (martian/request-for swagger :metastatus {:cmd-args cmd-args})
        in-flight (pmap #(http/request
                          (assoc request :url (str (api-endpoints %)
                                                   (:url request))))
                        clusters)]
    (System/exit
     (reduce (fn [final-code {body :body}]
               (let [{out "output" code "exit_code"} (json/parse-string body)]
                 (println out)
                 (if-not (= 0 code) code final-code)))
             0
             in-flight))))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args opts)]
    (cond (:help options) (usage summary nil)
          errors (usage summary errors)
          :else (metastatus options arguments))))
