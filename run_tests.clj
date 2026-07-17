(require '[clojure.test :as t])

(doseq [ns-sym '[tsukuroi.methods.test-charter-gates
                  tsukuroi.methods.test-manifest-invariants
                  tsukuroi.repository-contract-test]]
  (require ns-sym))

(let [result (apply t/run-tests
                    '[tsukuroi.methods.test-charter-gates
                      tsukuroi.methods.test-manifest-invariants
                      tsukuroi.repository-contract-test])]
  (System/exit (if (zero? (+ (:fail result) (:error result))) 0 1)))
