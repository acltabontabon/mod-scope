# ModScope

**Installed-game modding triage assistant.**

ModScope is a read-only discovery toolkit that helps PC modders decide *which of their installed games are worth investigating*, and what the realistic next step is for each one. It auto-detects every installed Steam game, runs fast triage across them, ranks them by modding surface, and explains in plain English why a game is a good first target or a hard one.

ModScope never patches, injects, or modifies game files.

---

## What ModScope does

- Auto-detects every installed Steam game on launch
- Ranks installed games by modding surface (loose files, configs, archives, saves)
- Runs a fast **quick triage** across all games — no hashing, no binary string scan
- Identifies the engine family (Unreal, Unity, Glacier/IOI, REDengine, Bethesda Creation, Source) from file layout
- Recommends a concrete next action per game (inspect configs, list archives, build a save backup, run a deep scan, set up external tooling, manual review, low-value target, …)
- Produces per-game markdown reports and a library-wide summary
- Persists scan history and settings under `~/.modscope/`

## What ModScope is not

- Not a mod manager
- Not a trainer, cheat, or memory patcher
- Not a DLL injector
- Not a DRM or anti-cheat bypass
- Not a tool that modifies, renames, deletes, or rebuilds any game file

---

## Triage-first workflow

```
Dashboard (all installed games, with badges + recommended action)
    │
    ├── t            quick triage on the selected game
    ├── T            scan-all-quick: triage every detected game in turn
    ├── Enter        open Game Details
    │                    │
    │                    ├── t  triage    ├── d  deep scan
    │                    └── r  open report
    │
    ├── d            jump straight to deep-scan setup for the selected game
    ├── r            open the library summary
    ├── s            settings
    └── q            quit
```

Failures during scan-all-quick are recorded per game and never stop the loop.

---

## Storage layout

Everything ModScope writes lives under `~/.modscope/` (override with `MODSCOPE_HOME`):

```
~/.modscope/
  state/
    scan-history.json     last-scan record per game
    settings.json         user preferences
  reports/
    library-summary.md    aggregated view across the whole library
    games/<safe-id>/
      scan-summary.md     plain-English summary of what was found
      candidates.md       likely modding surfaces by category
      file-inventory.json structured file list
      text-hints.md       QoL keyword matches
      save-locations.md   detected save paths
      save-inventory.md   files inside each save folder
      binary-string-hints.md (when present)
      package-definition-analysis.md (when present)
      recommendations.md  ranked next-action list with reasons
```

ModScope never writes anything outside `~/.modscope/`.

---

## Scan modes

| Mode | Hashing | Text hints | Binary strings | When to use |
|------|---------|------------|----------------|-------------|
| `QUICK` | off | off | off | Dashboard triage. Fast classification across many games. |
| `STANDARD` | up to 100 MB per file | on | conservative policy | Default for a single game. |
| `DEEP` | unbounded | on | full | Thorough investigation of one game. |

---

## Recommendation types

| Type | Meaning |
|------|---------|
| `GOOD_FIRST_MOD_TARGET` | Loose configs, readable text, known engine — start here. |
| `INSPECT_CONFIGS` | Loose `*.ini` / `*.cfg` / `*.json` / `*.xml` files worth opening. |
| `INSPECT_ARCHIVES` | Content packed into large archives. List read-only first. |
| `SET_UP_EXTERNAL_TOOL` | Engine-specific tooling needed for real asset modding. |
| `BUILD_SAVE_BACKUP` | Save data found — a save backup is a realistic first QoL feature. |
| `RUN_DEEP_SCAN` | Quick triage was low-confidence; a deep scan will say more. |
| `IGNORE_VENDOR_DLL_NOISE` | A large share of files are vendor/runtime libraries. |
| `MANUAL_REVIEW` | Content looks promising but engine could not be identified. |
| `LOW_VALUE_TARGET` | No engine, no loose surface, no archives — skip unless you have a specific reason. |

Every recommendation that mentions external tooling carries the same caveat: **use read-only listing first, do not extract or rebuild archives without backups and a clear modding workflow.**

---

## Example: 007 First Light triage

```
Likely Glacier/IOI package layout detected from .rpkg files.
This game is archive-heavy. Most content appears to be stored in large package files.
Loose-file QoL mods may be limited.
No loose config files were found. Simple ini/json tweaks are unlikely from the install folder.
Save data was detected. A save-backup workflow is a realistic first QoL feature.
Recommended:
  1. Archive listing recommended (INSPECT_ARCHIVES)
  2. External tool likely required: Glacier/RPKG tooling (SET_UP_EXTERNAL_TOOL)
  3. Save backup tooling candidate (BUILD_SAVE_BACKUP)
```

For an unknown game, ModScope still produces generic conclusions based on file layout (archive ratio, loose-file count, vendor noise) so you can decide whether it's worth a deeper look.

---

## Safety guarantee

Every report and every TUI screen states it explicitly:

> READ-ONLY SCAN — ModScope did not modify, rename, delete, patch, overwrite, inject, or execute game files.

External-tooling recommendations always include:

> Use read-only listing first. Do not extract, rebuild, or replace archives without backups and a clear modding workflow.

---

*ModScope is open-source and read-only by design. Java 25, TamboUI-first, GraalVM native-image ready.*
