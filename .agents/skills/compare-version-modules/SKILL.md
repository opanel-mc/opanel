---
name: compare-version-modules
description: Compare two server-platform adaptation modules of the same platform but different Minecraft/game versions (e.g. forge-1.21.5 vs forge-1.21.8), surfacing only the real API / 写法 differences by normalizing away package-name and version-string noise. Use when the user asks to 对比/比较/diff two version modules, find what changed between two `<platform>-<version>` directories (forge/neoforge/fabric/spigot/folia), or understand how to port an adaptation from one game version to the next.
---

# Compare version modules

Adaptation modules for different game versions of the same platform do the same
job with slightly different code. This skill isolates the *meaningful* deltas
(API renames, signature changes, new imports, build-config bumps) from the
mechanical noise (package names and version numbers that differ in every file).

## Quick start

Run the bundled script with the two module directory paths. Modules now live
under their platform folder (`fabric/`, `forge/`, `neoforge/`, `bukkit/`), so
pass the platform-prefixed path:

```bash
bash <skill-dir>/scripts/compare.sh forge/forge-1.21.5 forge/forge-1.21.8
```

(The script derives the package token from the directory's basename, so the
platform-folder prefix does not affect normalization.)

It pairs files across the two modules, normalizes each module's package token,
directory name, and version string into shared placeholders, then prints a
per-file diff plus a summary line. Files that are identical after normalization
are omitted; files present in only one module are flagged `ONLY IN ...`.

## Convention it relies on

Directory `<platform>/<platform>-<version>` ↔ java package `net.opanel.<platform>_<ver>`,
where the package token is the module directory's basename with `-` and `.` turned into `_`:

- `forge-1.21.5`  → `forge_1_21_5`
- `spigot-1.16.1` → `spigot_1_16_1`
- `neoforge-1.21.3` → `neoforge_1_21_3`

This holds for forge / neoforge / fabric / spigot / folia in this repo.

## Workflow

1. Run `scripts/compare.sh <platform>/<older> <platform>/<newer>` from the repo root.
2. Read the diff. Each `===== <path> =====` block is one changed file.
3. Group the findings for the user, e.g.:
   - **Build config** — `gradle.properties` (version range, loader version), `build.gradle` (new repos/deps), `pack.mcmeta` (pack_format).
   - **API changes** — renamed/moved imports, changed method signatures, replaced types (e.g. `GameProfile` → `NameAndId`).
   - **New behavior** — added listeners, fields, null guards, newly-implemented interfaces.
4. Note knock-on changes: a constructor signature change in one file usually
   forces edits at every call site — call those out together.
5. Skip files reported only as version-string/package differences; the script
   already removed those, so anything left is a genuine difference worth reporting.

## Notes

- The right bound of `minecraft_version_range` (the *next* version) is a real
  difference, not noise, so it is intentionally shown.
- If a module pair lives outside the repo root, pass full/relative paths:
  `compare.sh path/to/forge-1.21.8 path/to/forge-1.21.9` (any path works; only the basename matters).
- The script reads files only; it never edits. Safe to run anytime.
