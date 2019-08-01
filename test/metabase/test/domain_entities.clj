(ns metabase.test.domain-entities
  (:require [metabase.domain-entities
             [core :as de]
             [specs :as de.specs]]
            [metabase.models.table :as table]))

(def test-domain-entity-specs
  "A test domain specs written against our test DB."
  (->> [{:name                "Venues parent"
         :required_attributes [{:field "Longitude"}
                               {:field "Latitude"}]}
        {:name                "Venues"
         :required_attributes [{:field "Category"} ; Price
                               {:field "FK"} ; Category ID
                               ;; These are here just for uniquness
                               {:field "Longitude"}
                               {:field "Latitude"}]
         :refines             "Venues parent"
         :breakout_dimensions ["FK"]
         :metrics             {"Avg Price" {:aggregation [:avg [:dimension "Category"]]}}}
        {:name                "VenuesEnhanced"
         :refines             "Venues"
         :required_attributes [{:field "AvgPrice"}
                               {:field "MinPrice"}
                               {:field "MaxPrice"}]}]
       (map (fn [spec]
              [(:name spec) (-> spec
                                (#'de.specs/add-to-hiearchy!)
                                (#'de.specs/domain-entity-spec-parser))]))
       (into {})))

(defmacro with-test-domain-entity-specs
  "Evaluate `body` in a context where `domain-entities.specs/domain-entity-specs` have been swapped for `test-domain-entity-specs`"
  [& body]
  `(with-redefs [de.specs/domain-entity-specs (delay test-domain-entity-specs)
                 ;; This is somewhat hackish as it depends on the internals of how how we access `:domain_entity`,
                 table/with-domain-entity     (fn [tables#]
                                                (for [table# tables#]
                                                  (assoc table# :domain_entity (de/domain-entity-for-table table#))))]
     ~@body))