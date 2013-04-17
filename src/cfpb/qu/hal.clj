(ns cfpb.qu.hal
  (:require [inflections.core :refer [plural]]
            [cheshire.core :as json]
            [clojure.data.xml :as xml]))

(defrecord Resource [href links embedded properties])

(defn new-resource [href]
  (->Resource href [] [] {}))

(defn add-link [resource & args]
  (let [link (apply hash-map args)]
    (update-in resource [:links] #((fnil conj []) % link))))

(defn add-resource [resource type embedded]
  (update-in resource [:embedded] #((fnil conj []) % [type embedded])))

(defn add-property [resource & args]
  (let [properties (apply hash-map args)]
    (update-in resource [:properties] #((fnil merge {}) % properties))))

(def add-properties add-property)

(defn- json-representation [resource]
  (let [links (-> [{:rel "self" :href (:href resource)}]
                  (concat (:links resource)))
        embedded (into {}
                       (map (fn [[k resources]]
                              [(plural k)
                               (map (comp json-representation second) resources)])
                            (group-by first (:embedded resource))))
        representation (merge (:properties resource)
                              {:_links links})
        representation (if (empty? embedded)
                         representation
                         (merge representation {:_embedded embedded}))]
    representation))

(defmulti Resource->representation (fn [_ representation-type]
                                     representation-type))

(defmethod Resource->representation :json [resource _]
  (let [representation (json-representation resource)]
    (json/generate-string representation)))
