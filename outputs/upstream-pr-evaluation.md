# Upstream PR evaluation for Y700 Gen 3

Evaluated: `2026-08-30`

Baseline: `origin/master` at `c76d3ddc6dec902cb55c4d2d720718f4db03e492`, merged into `y700/stable` by `e99e4105`.

## Decision

Integrate two focused changes: [#1599](https://github.com/utkarshdalal/GameNative/pull/1599), adapted rather than merged because its branch is stale/conflicting and would regress newer Epic resume and external-storage behavior; and [#1719](https://github.com/utkarshdalal/GameNative/pull/1719), cherry-picked because its current two-commit delta is focused, mergeable, and green upstream. Keep nine potentially relevant PRs deferred behind device, maturity, or conflict gates. Reject the remaining candidates for this Y700 delivery lane.

The screen is intentionally strict. “Could be useful on Android” is not enough; the change must justify its regression and maintenance surface on the Y700/Joy-Con branch.

## Integrated

- [#1599](https://github.com/utkarshdalal/GameNative/pull/1599) — **integrate, P1** — reduces Epic downloader hot-path logging, progress-update frequency, and repeated chunk-to-file scans. The author reported tablet CPU dropping from roughly 180–225% to 96–129%, threads from ~127 to ~70–73, and CPU0 from ~63°C to ~43–45°C during a large real download. Head `633113d1271dafba62fa9531ec45c61412788f0c`; upstream CI passed. Adapted in local commit `a5491ddf` while preserving current internal chunk cache, resumable-file verification, exFAT allocation behavior, and storage-space checks. Y700 thermal benefit remains a hardware-validation claim, not yet proven locally.
- [#1719](https://github.com/utkarshdalal/GameNative/pull/1719) — **integrate, P1** — reduces Steam PICS refresh memory pressure by selecting only `packageId` and `access_token` from Room instead of materializing complete license records, and replaces hard-coded 500-item chunks with the existing `MAX_PICS_BUFFER`. The PR is mergeable, changes three Kotlin files by 22 additions/3 deletions, and its upstream build/review checks passed. Cherry-picked with original authorship as `edc88cc5` and `d3c8092b`; the latter also removes an unused import introduced by the first commit.

## Deferred candidates

- [#1834](https://github.com/utkarshdalal/GameNative/pull/1834) — **defer, P2** — libredirect performance is relevant, but this PR conflicts with the upstream libredirect update already merged, carries an opaque native binary, and its CI was cancelled. Require a clean delta and Y700 launch/compatibility measurements.
- [#1833](https://github.com/utkarshdalal/GameNative/pull/1833) — **defer, P2** — multi-action bindings are valuable but the 40-file, 2,256-line controller/input rewrite conflicts and overlaps the Joy-Con ownership surface. It needs a clean rebase plus Joy-Con and touch regression evidence.
- [#1811](https://github.com/utkarshdalal/GameNative/pull/1811) — **defer, P2** — viewport positioning could help the Y700 display, but it is draft, UI/runtime-wide, overlaps `XServerScreen`, and lacks Y700 evidence.
- [#1806](https://github.com/utkarshdalal/GameNative/pull/1806) — **defer, P2** — lower-latency DirectAudio may matter on Y700, but the PR is draft, Proton-11/arm64ec-specific, adds binary assets, and lacks device recordings.
- [#1776](https://github.com/utkarshdalal/GameNative/pull/1776) — **defer, P2** — temperature and battery warnings fit a thermally constrained tablet, but a 26-file HUD/UI change is disproportionate without Y700 validation.
- [#1759](https://github.com/utkarshdalal/GameNative/pull/1759) — **defer, P2** — adaptive resolution could improve Y700 scaling, but the broad 24-file change includes generated artifacts and needs visual/performance evidence on the actual panel.
- [#1612](https://github.com/utkarshdalal/GameNative/pull/1612) — **defer, P2** — frame-rate control is device-relevant, but the draft ships a native `.so`; require source/binary provenance, clean rebase, and frame-pacing evidence.
- [#1584](https://github.com/utkarshdalal/GameNative/pull/1584) — **defer, P2** — controller/vibration work directly overlaps `evshim`, controller config, and `XServerScreen`; draft status and native binary replacement make it unsafe for the proven Joy-Con path.
- [#1564](https://github.com/utkarshdalal/GameNative/pull/1564) — **defer, P2** — per-game vibration is attractive for Joy-Con, but the 587-line `evshim`/WinHandler/controller rewrite must prove paired-half rumble semantics and preserve app-private shared memory first.

## Rejected for this branch

### Current group

- [#1867](https://github.com/utkarshdalal/GameNative/pull/1867) — **reject** — repairs upstream’s three-AAB ad-hoc extraction workflow, not the isolated single-APK Y700 workflow; no reproduced failure here.
- [#1866](https://github.com/utkarshdalal/GameNative/pull/1866) — **reject** — HLTB spoiler UI has no Y700/Joy-Con value.
- [#1859](https://github.com/utkarshdalal/GameNative/pull/1859) — **reject** — large draft EA launcher feature, conflicting and unrelated to device/controller stability.
- [#1858](https://github.com/utkarshdalal/GameNative/pull/1858) — **reject** — opaque Steam-emulator binary refresh without a Y700-specific compatibility receipt.
- [#1853](https://github.com/utkarshdalal/GameNative/pull/1853) — **reject** — OpenComposite/XR scope is unrelated and very large.
- [#1849](https://github.com/utkarshdalal/GameNative/pull/1849) — **reject** — broad progress UI/QoL work has no device-specific benefit.
- [#1821](https://github.com/utkarshdalal/GameNative/pull/1821) — **reject** — adopted-primary storage is a sound fix, but the Y700 Gen 3 has no adopted microSD target; no payoff for this lane.
- [#1818](https://github.com/utkarshdalal/GameNative/pull/1818) — **reject as duplicate** — host `EVSHIM_BASE_PATH` problem is already solved more robustly here by app-private JNI initialization in `WinHandler`.
- [#1815](https://github.com/utkarshdalal/GameNative/pull/1815) — **reject** — library-detail UI toggle is unrelated.
- [#1810](https://github.com/utkarshdalal/GameNative/pull/1810) — **reject as duplicate** — dynamic package paths and app-private gamepad memory are already present; applying it would duplicate/weaken the current explicit JNI bridge.
- [#1804](https://github.com/utkarshdalal/GameNative/pull/1804) — **reject** — draft microphone routing feature has no demonstrated Y700 need.

### Library, store, and general application features

- [#1799](https://github.com/utkarshdalal/GameNative/pull/1799) — **reject** — library-tab customization only.
- [#1796](https://github.com/utkarshdalal/GameNative/pull/1796) — **reject** — large hidden-game/library DB feature, no Y700 value.
- [#1795](https://github.com/utkarshdalal/GameNative/pull/1795) — **reject** — store-page links only.
- [#1790](https://github.com/utkarshdalal/GameNative/pull/1790) — **reject** — Epic multi-save fix is useful generally but not tied to the current Y700 objective and changes service lifecycle/state broadly.
- [#1786](https://github.com/utkarshdalal/GameNative/pull/1786) — **reject** — curated 4:3 library filtering is irrelevant to the 16:10 Y700 lane.
- [#1781](https://github.com/utkarshdalal/GameNative/pull/1781) — **reject** — URI/deep-link surface adds attack and maintenance surface without device benefit.
- [#1765](https://github.com/utkarshdalal/GameNative/pull/1765) — **reject** — global offline mode is unrelated and broad.
- [#1760](https://github.com/utkarshdalal/GameNative/pull/1760) — **reject** — cross-store library source switching is unrelated.
- [#1758](https://github.com/utkarshdalal/GameNative/pull/1758) — **reject** — list-layout/external-storage crash fix has no reproduced Y700 path and carries a broad UI/storage diff.
- [#1757](https://github.com/utkarshdalal/GameNative/pull/1757) — **reject** — shared containers materially change isolation semantics.
- [#1742](https://github.com/utkarshdalal/GameNative/pull/1742) — **reject** — native Android game builds/Steam Frame are a different product surface.
- [#1721](https://github.com/utkarshdalal/GameNative/pull/1721) — **reject** — large library/PICS optimization superseded in part by newer focused work; no controller/runtime benefit.

- [#1707](https://github.com/utkarshdalal/GameNative/pull/1707) — **reject** — family-library selection is large, conflicting, and unrelated.
- [#1695](https://github.com/utkarshdalal/GameNative/pull/1695) — **reject** — achievements viewer only.
- [#1693](https://github.com/utkarshdalal/GameNative/pull/1693) — **reject** — library ViewModel optimization lacks a Y700-relevant bottleneck and current integration evidence.
- [#1651](https://github.com/utkarshdalal/GameNative/pull/1651) — **reject** — draft centralized download queue changes six services; too broad for this lane.
- [#1627](https://github.com/utkarshdalal/GameNative/pull/1627) — **reject** — 46-file screenshot feature is unrelated.
- [#1570](https://github.com/utkarshdalal/GameNative/pull/1570) — **reject** — 510-file draft HTML5 runtime is effectively another product.
- [#1489](https://github.com/utkarshdalal/GameNative/pull/1489) — **reject** — search-bar options shortcut only.
- [#1459](https://github.com/utkarshdalal/GameNative/pull/1459) — **reject** — executable-path UI/URI support is stale, conflicting, and unrelated.
- [#1419](https://github.com/utkarshdalal/GameNative/pull/1419) — **reject** — launcher-warning metadata/UI only.
- [#1383](https://github.com/utkarshdalal/GameNative/pull/1383) — **reject** — broad Steam cloud-status feature, no Y700 value.
- [#1273](https://github.com/utkarshdalal/GameNative/pull/1273) — **reject** — draft GOG cloud conflict UX is unrelated.
- [#1146](https://github.com/utkarshdalal/GameNative/pull/1146) — **reject** — broad cloud/local-save policy work is unrelated.
- [#965](https://github.com/utkarshdalal/GameNative/pull/965) — **reject** — navigation/offline UI fix is stale and unrelated.
- [#788](https://github.com/utkarshdalal/GameNative/pull/788) — **reject** — family-sharing account fix is unrelated.
- [#392](https://github.com/utkarshdalal/GameNative/pull/392) — **reject** — 101-file theme engine is obsolete and disproportionate.
- [#297](https://github.com/utkarshdalal/GameNative/pull/297) — **reject** — stale custom-media rewrite.
- [#229](https://github.com/utkarshdalal/GameNative/pull/229) — **reject** — older overlapping custom-media implementation.

### Runtime/controller changes rejected rather than deferred

- [#1741](https://github.com/utkarshdalal/GameNative/pull/1741) — **reject** — accessibility barrel zoom rewrites rendering/input behavior without a current Y700 requirement.
- [#1706](https://github.com/utkarshdalal/GameNative/pull/1706) — **reject** — 29k-line BLE support targets the unreleased Steam Controller “Triton”, not Joy-Con; untenable maintenance surface.
- [#1631](https://github.com/utkarshdalal/GameNative/pull/1631) — **reject** — draft frame-pacing logger is diagnostic instrumentation, not a delivered improvement.
- [#1499](https://github.com/utkarshdalal/GameNative/pull/1499) — **reject as superseded** — current upstream already contains newer external-storage enumeration/performance work; no Y700 storage failure is reproduced.
- [#1464](https://github.com/utkarshdalal/GameNative/pull/1464) — **reject as stale/partly superseded** — current tree already carries install-script data/setup paths; this 1,084-line divergent branch overlaps launch code.
- [#1427](https://github.com/utkarshdalal/GameNative/pull/1427) — **reject as superseded** — current imagefs installer/migrator already contains the relevant migration hooks.
- [#1347](https://github.com/utkarshdalal/GameNative/pull/1347) — **reject** — 34-file Real Steam overhaul is divergent and overlaps `XServerScreen`, `Container`, and `WinHandler`; far beyond the Y700 contract.
- [#1005](https://github.com/utkarshdalal/GameNative/pull/1005) — **reject** — 2,233-line controller preset rewrite overlaps physical/on-screen bindings and would multiply Joy-Con regression surface.
- [#887](https://github.com/utkarshdalal/GameNative/pull/887) — **reject as superseded** — current imagefs symlink/migration implementation already contains the relevant capability.

## Compatibility and risk conclusion

- Paired Joy-Con ownership, reconnect, slot persistence, touch coexistence, package suffix, stable signing, and app-private `evshim` remain untouched by the selected product patch.
- The selected patch changes one Kotlin downloader file. It does not alter controller, renderer, packaging, signing, workflow secret handling, or application identity.
- The upstream libredirect binary merged through `c76d3ddc` has SHA-256 `557aacc4a8b9e0dccb5e6d43aecca7b1723c0095f9bcb24fc5efe0db5390ff41`; it matches the exact `origin/master` Git blob `530da42f0cbf1e3b336159f3ebb21c1f10d06534`. Builder Assurance therefore reports `QUALITY_INCOMPLETE`, not a code finding, because its source adapter cannot inspect that upstream ARM64 ELF.
- No upstream PR was merged, closed, commented on, reviewed, or otherwise mutated.
