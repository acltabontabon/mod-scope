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

## Supported games

| Profile ID | Game |
|------------|------|
| `007-first-light` | 007 First Light (Steam App ID: 3768760) |

More profiles can be added by implementing `GameProfile` and registering in `GameProfileRegistry`.

---

## Running on JVM

**Requirements:** Java 25+

```bash
# Build
mvn package

# Run
java -jar target/modscope-0.1.0-SNAPSHOT.jar
```

---

## Building a native executable

**Requirements:** GraalVM JDK 25 with Native Image support

```bash
# Install native-image component (if needed)
gu install native-image

# Build native executable
mvn -Pnative package

# Run the native binary
./target/modscope
```

The native binary starts in milliseconds and requires no JVM.

### Known native-image limitations

- TamboUI's JLine backend may require `--enable-native-access` on some platforms
- Jackson's `ObjectMapper` is used with `Map<String, Object>` to avoid reflection — no additional config needed
- If native build fails, check that you are using GraalVM JDK 25 (not a standard OpenJDK)
- Run with `-H:+ReportExceptionStackTraces` to diagnose native image build errors

### Troubleshooting native image

```
# Verbose build output
mvn -Pnative package -Dnative.maven.plugin.verbose=true

# Add missing reflection config to:
src/main/resources/META-INF/native-image/reflect-config.json
```

---

## Usage

ModScope launches an interactive TUI. Navigate with arrow keys, select with Enter.

### Home screen

- **Scan detected Steam games** — scans the first detected Steam game matching a known profile
- **Scan 007 First Light** — scans specifically for 007 First Light via Steam
- **Choose game folder manually** — enter a custom directory path in setup
- **Exit** — quit

### Scan setup screen

Select profile, directory, deep scan mode, and output folder. The screen shows a clear warning:

```
⚠  READ-ONLY SCAN — ModScope will not modify game files.
```

### Scan progress screen

Live display of current phase and counters:
- Files scanned
- Config-like files
- Archives
- Videos
- Hints found

### Scan results screen

Summary of detected game, file counts by category, save candidates, QoL hints, and path to generated reports.

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

## Roadmap

- [x] Steam install detection (Windows, Mac, Linux)
- [x] File inventory with categorization and hashing
- [x] QoL hint scanner
- [x] Markdown + JSON report generation
- [x] TamboUI interactive TUI
- [x] GraalVM native image build profile
- [ ] Manual game folder entry via TUI text input
- [ ] Additional game profiles (Cyberpunk 2077, Hitman, Stellar Blade, …)
- [ ] Recent reports viewer in TUI
- [ ] Diff comparison between two scans
- [ ] Mod candidate scoring

---

*ModScope is open-source and read-only by design.*
