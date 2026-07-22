#!/usr/bin/env bash
# Compare two server-platform version modules, normalizing away package-name
# and version-string noise so only real API / 写法 differences surface.
#
# Usage:  compare.sh <moduleA> <moduleB>
# Example: compare.sh forge-1.21.5 forge-1.21.8
#
# Convention assumed: directory "<platform>-<version>" maps to java package
# token "<platform>_<version-with-dots-and-dashes-as-underscores>"
#   forge-1.21.5  -> forge_1_21_5
#   spigot-1.16.1 -> spigot_1_16_1
set -euo pipefail

A="${1:?usage: compare.sh <moduleA> <moduleB>}"
B="${2:?usage: compare.sh <moduleA> <moduleB>}"
A="${A%/}"; B="${B%/}"

[ -d "$A" ] || { echo "no such directory: $A" >&2; exit 1; }
[ -d "$B" ] || { echo "no such directory: $B" >&2; exit 1; }

nameA="$(basename "$A")"; nameB="$(basename "$B")"   # forge-1.21.5
pkgA="${nameA//[-.]/_}";  pkgB="${nameB//[-.]/_}"     # forge_1_21_5
verA="${nameA#*-}";       verB="${nameB#*-}"          # 1.21.5

# sed program that blanks out the per-module identifiers into shared tokens.
norm() {
  sed -e "s/$1/__PKG__/g" -e "s/$2/__NAME__/g" -e "s/$3/__VER__/g" \
      -e "s/${3//./_}/__VERU__/g"
}

esc() { printf '%s' "$1" | sed 's/[.[\*^$()+?{|]/\\&/g'; }
pkgAe="$(esc "$pkgA")"; pkgBe="$(esc "$pkgB")"
nameAe="$(esc "$nameA")"; nameBe="$(esc "$nameB")"
verAe="$(esc "$verA")"; verBe="$(esc "$verB")"

diffcount=0
onlyA=0; onlyB=0

# Enumerate source files in A (skip build artifacts), pair them with B.
while IFS= read -r fa; do
  rel="${fa#"$A"/}"
  relB="${rel//$pkgA/$pkgB}"      # swap package token in path
  fb="$B/$relB"
  if [ ! -f "$fb" ]; then
    echo "===== ONLY IN $nameA: $rel ====="
    onlyA=$((onlyA+1)); continue
  fi
  out="$(diff <(norm "$pkgAe" "$nameAe" "$verAe" < "$fa") \
              <(norm "$pkgBe" "$nameBe" "$verBe" < "$fb") || true)"
  if [ -n "$out" ]; then
    echo "===== $rel ====="
    echo "$out"
    diffcount=$((diffcount+1))
  fi
done < <(find "$A" -type f \
            -not -path "*/build/*" -not -path "*/.gradle/*" \
            -not -path "*/target/*" -not -path "*/bin/*" | sort)

# Report files that exist only in B.
while IFS= read -r fb; do
  rel="${fb#"$B"/}"
  relA="${rel//$pkgB/$pkgA}"
  [ -f "$A/$relA" ] || { echo "===== ONLY IN $nameB: $rel ====="; onlyB=$((onlyB+1)); }
done < <(find "$B" -type f \
            -not -path "*/build/*" -not -path "*/.gradle/*" \
            -not -path "*/target/*" -not -path "*/bin/*" | sort)

echo
echo "---- summary: $diffcount file(s) differ; $onlyA only in $nameA; $onlyB only in $nameB ----"
