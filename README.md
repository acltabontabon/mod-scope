# ModScope

**Inspect game files before building mods.**

ModScope is a read-only discovery toolkit for PC game modding. It helps modders inspect installed games, inventory files, detect saves and configs, search for QoL-related hints, and generate reports — before building mods.

ModScope does **not** patch memory, bypass DRM, inject code, or modify game files during scans.

---

## What ModScope is

- A read-only file inspection tool for modders
- A Steam library scanner that locates game installations automatically
- A file inventory generator (executables, archives, configs, videos, localization)
- A QoL hint finder (motion blur, FOV, film grain, vignette, subtitles, etc.)
- A report generator (Markdown + JSON output)
- A native TUI application powered by TamboUI

## What ModScope is NOT

- Not a mod manager
- Not a trainer or cheat tool
- Not a memory patcher or DLL injector
- Not a DRM bypass tool

---

## Safety guarantee

ModScope will never:
- Delete, rename, patch, or overwrite any game files
- Inject code or modify executables
- Bypass DRM or anti-cheat
- Modify save files

Every scan prints `READ-ONLY SCAN` at startup and writes a safety note into every report.

---

## Reports

Reports are written to `.modscope/reports/` by default.

| File | Contents |
|------|----------|
| `scan-summary.md` | Scan overview, file counts, save candidates, top QoL leads |
| `file-inventory.json` | Full structured file list with category, size, hash |
| `candidates.md` | Likely modding surfaces by category |
| `text-hints.md` | QoL keyword matches with file/line/snippet |
| `save-locations.md` | Detected save paths and their status |

---

## Example output (scan-summary.md excerpt)

```markdown
# Scan Summary

- **Scan date:** 2026-05-28T10:00:00Z
- **Game:** 007 First Light
- **Install path:** `D:\SteamLibrary\steamapps\common\007 First Light`
- **Scan mode:** STANDARD
- **Total files:** 1234

## File counts
| Category | Count |
|----------|-------|
| ARCHIVE | 45 |
| CONFIG | 12 |
| VIDEO | 8 |
...

## QoL investigation leads
- **motionblur**: found in 3 match(es)
- **fov**: found in 2 match(es)
```

---

*ModScope is open-source and read-only by design.*
