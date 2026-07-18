(ns tsukuroi.murakumo-test
  (:require [clojure.test :refer [deftest is testing]]
            [tsukuroi.murakumo :as tsukuroi]))

(def full-attestations
  (into {}
        (map (fn [gate] [gate (str "attested-" (name gate))]))
        (distinct (mapcat :required-gates (vals tsukuroi/cell-specs)))))

(deftest maps-all-legacy-tsukuroi-cells
  (is (= #{"tsukuroi_charter_rider_scan"
           "tsukuroi_closure_verification"
           "tsukuroi_finding_intake"
           "tsukuroi_patch_synthesis"
           "tsukuroi_patch_validation"
           "tsukuroi_pr_submission"
           "tsukuroi_silen_tsukuroi_review"}
         (set (map :legacy-cell (vals tsukuroi/cell-specs))))))

(deftest r0-gates-block-effects
  (let [plan (tsukuroi/cell-plan :finding-intake
                                 {:mandate-id "mandate-001"
                                  :finding-cid "bafkreifinding"
                                  :target-repo "owner/repo"
                                  :computed-at "2026-06-29T00:00:00Z"})]
    (is (= :blocked (:status plan)))
    (is (= [:council-charter-attestation
            :silen-tsukuroi-baseline-review
            :active-remediation-mandate-baseline
            :owner-authority-dual-signature-baseline
            :akuma-finding-cid-baseline
            :no-probing-baseline
            :propose-only-no-merge-baseline
            :defensive-only-no-exploit-baseline
            :scoped-write-allowed-paths-baseline
            :no-platform-held-key-baseline
            :murakumo-only-inference-baseline
            :same-owner-target-finding-baseline
            :mandate-valid-window-baseline
            :mandate-not-revoked-baseline]
           (:missing-gates plan)))
    (is (empty? (:effects plan)))))

(deftest attested-pr-submission-emits-propose-only-patch-proposal
  (let [plan (tsukuroi/cell-plan :pr-submission
                                 {:attestations full-attestations
                                  :mandate-id "mandate-001"
                                  :finding-cid "bafkreifinding"
                                  :target-repo "owner/repo"
                                  :proposal-id "proposal-001"
                                  :submission-mode "fork-pr"
                                  :computed-at "2026-06-29T00:00:00Z"
                                  :record {:tid "proposal-001"
                                           :forkUrl "https://github.com/owner/repo/pull/1"}})
        effect (first (:effects plan))]
    (is (= :ready (:status plan)))
    (is (= :mst/put-record (:op effect)))
    (is (= tsukuroi/actor-did (:actor effect)))
    (is (= "com.etzhayyim.tsukuroi.patchProposal" (:collection effect)))
    (is (= "proposal-001" (:rkey effect)))
    (is (= false (get-in effect [:record :mergeAuthorityHeld])))
    (is (= false (get-in effect [:record :autonomousMerge])))
    (is (= true (get-in effect [:record :defensiveOnly])))
    (is (= 0 (get-in effect [:record :platformHeldKeyCount])))))

(deftest validation-never-targets-live-runtime
  (let [plan (tsukuroi/cell-plan :patch-validation
                                 {:attestations full-attestations
                                  :validation-id "validation-001"
                                  :proposal-id "proposal-001"
                                  :computed-at "2026-06-29T00:00:00Z"})
        effect (first (:effects plan))]
    (is (= :ready (:status plan)))
    (is (= "com.etzhayyim.tsukuroi.patchValidationResult" (:collection effect)))
    (is (= false (get-in effect [:record :ranAgainstLiveTarget])))))

(deftest silen-review-keeps-zero-counters
  (let [plan (tsukuroi/cell-plan :silen-tsukuroi-review
                                 {:attestations full-attestations
                                  :review-id "review-2026q2"
                                  :computed-at "2026-06-29T00:00:00Z"})
        record (:record (first (:effects plan)))]
    (is (= :ready (:status plan)))
    (is (= 0 (:autonomousMergeCount record)))
    (is (= 0 (:exploitArtifactCount record)))
    (is (= 0 (:outOfScopeWriteCount record)))
    (is (= 0 (:platformHeldKeyCount record)))))

(deftest cell-specific-gates-remain-specific
  (testing "patch synthesis keeps scoped write"
    (let [attestations (dissoc full-attestations :paths-touched-subset-allowed-paths-baseline)
          plan (tsukuroi/cell-plan :patch-synthesis {:attestations attestations})]
      (is (= [:paths-touched-subset-allowed-paths-baseline] (:missing-gates plan)))))
  (testing "closure keeps akuma re-probe"
    (let [attestations (dissoc full-attestations :akuma-reprobe-pass-attestation)
          plan (tsukuroi/cell-plan :closure-verification {:attestations attestations})]
      (is (= [:akuma-reprobe-pass-attestation] (:missing-gates plan)))))
  (testing "PR submission keeps no platform-held key"
    (let [attestations (dissoc full-attestations :owner-delegated-expiring-fork-pr-credential-baseline)
          plan (tsukuroi/cell-plan :pr-submission {:attestations attestations})]
      (is (= [:owner-delegated-expiring-fork-pr-credential-baseline] (:missing-gates plan))))))

(deftest all-cell-plans-ready-when-attested
  (let [plans (tsukuroi/all-cell-plans {:attestations full-attestations
                                        :mandate-id "mandate-001"
                                        :finding-cid "bafkreifinding"
                                        :target-repo "owner/repo"
                                        :proposal-id "proposal-001"
                                        :validation-id "validation-001"
                                        :closure-id "closure-001"
                                        :review-id "review-2026q2"
                                        :owner-did "did:example:owner"
                                        :authority-did "did:example:authority"
                                        :submission-mode "fork-pr"
                                        :computed-at "2026-06-29T00:00:00Z"})]
    (is (= (set (keys tsukuroi/cell-specs)) (set (keys plans))))
    (is (every? #(= :ready (:status %)) (vals plans)))
    (is (= 7 (count (mapcat :effects (vals plans)))))))
