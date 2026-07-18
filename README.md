# tsukuroi — Authorized Remediation Actor

`tsukuroi` is the defensive patch-proposal sibling of `akuma`. It consumes an
owner-attested vulnerability finding, synthesizes a defensive patch, validates it
in an egress-restricted sandbox, and proposes it to the authorized target. It
never probes, merges, deploys, releases, or holds platform master credentials.

## Migration Boundary

`kotoba-lang/kotodama-cells/tsukuroi_*` is legacy source
during migration. The domain actor implementation belongs here as pure `.cljc`
plans under `src/tsukuroi/murakumo.cljc`: the seven remediation cells map to
`remediationMandate`, `patchProposal`, `patchValidationResult`,
`closureAttestation`, and `silenTsukuroiReview` MST records. The boundary is
fail-closed: missing Council, owner/authority mandate, akuma finding,
no-probing, propose-only, defensive-only, scoped-write, sandbox, no-platform-key,
Murakumo-only, and closure attestations produce no write effects. Host placement
remains in `kotoba-lang/murakumo`; any AT Protocol/PDS surface remains in
`gftdcojp/app-aozora`.

## Cells

| Cell | Node | Output |
|---|---|---|
| `tsukuroi_finding_intake` | levi | `remediationMandate` |
| `tsukuroi_patch_synthesis` | levi | `patchProposal` |
| `tsukuroi_charter_rider_scan` | levi | `patchProposal` |
| `tsukuroi_patch_validation` | levi | `patchValidationResult` |
| `tsukuroi_pr_submission` | levi | `patchProposal` |
| `tsukuroi_closure_verification` | levi | `closureAttestation` |
| `tsukuroi_silen_tsukuroi_review` | levi | `silenTsukuroiReview` |

## Boundary Invariants

- No probing: vulnerability input comes from `akuma` or an owner-signed report.
- Propose only: no autonomous merge, self-approval, force-push, deploy, or release.
- Defensive only: no PoC, exploit, or offensive payload.
- Scoped write: touched paths must remain within `mandate.allowedPaths`.
- No platform-held key: submission uses owner-delegated, expiring fork-PR credentials.
- Closure requires owner human merge and akuma re-probe pass.
