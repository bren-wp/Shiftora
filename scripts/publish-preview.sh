#!/usr/bin/env bash
set -euo pipefail
run_id="${1:?GitHub Actions run ID is required}"
run_sha="${2:?The tested commit SHA is required}"
version="$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)".*/\1/p' app/build.gradle.kts | head -n1)"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid Android versionName: $version" >&2
  exit 1
fi
tag="v${version}"
if gh release view "$tag" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
  echo "$tag already exists — no overwrite"
  exit 0
fi
# Do not publish an APK unless the same commit also passed instrumented UI tests
# on both supported Android emulator profiles.
ui_checked="false"
for attempt in $(seq 1 90); do
  ui_conclusion="$(gh api "repos/$GITHUB_REPOSITORY/actions/workflows/android-ui.yml/runs?per_page=40" \
    --jq '[.workflow_runs[] | select(.head_sha == "'$run_sha'")] | .[0].conclusion // "pending"')"
  case "$ui_conclusion" in
    success) ui_checked="true"; break ;;
    failure|cancelled|timed_out|action_required)
      echo "UI tests not successful for $run_sha: $ui_conclusion" >&2
      exit 1 ;;
    *) sleep 20 ;;
  esac
done
if [[ "$ui_checked" != "true" ]]; then
  echo "UI tests have not completed successfully for $run_sha" >&2
  exit 1
fi
mkdir -p dist/apk dist/aab
gh run download "$run_id" --repo "$GITHUB_REPOSITORY" -n shiftora-debug-apk -D dist/apk
gh run download "$run_id" --repo "$GITHUB_REPOSITORY" -n shiftora-unsigned-aab -D dist/aab
mv dist/apk/app-debug.apk "dist/shiftora-${version}-debug.apk"
mv dist/aab/app-release.aab "dist/shiftora-${version}-unsigned.aab"
gh release create "$tag" --repo "$GITHUB_REPOSITORY" --target "$run_sha" \
  --title "Shiftora ${version} – Development Preview" \
  --notes "RAZVOJNO IZDANJE — nije odobreno za Google Play. Android CI i emulator testovi na API 30 i 36 uspješno su završeni za ovaj commit. Upozorenje: debug APK može imati drukčiji potpis od prethodne verzije, pa nadogradnja možda neće biti moguća bez produkcijskog ključa. Prije promjene instalacije izvezi JSON sigurnosnu kopiju (Više → Sigurnosna kopija) i spremi je izvan uređaja. Nemoj deinstalirati aplikaciju niti brisati njezine podatke bez sigurne kopije. AAB je NEPOTPISAN i nije spreman za Play Console. Procjena bruto plaće je informativna, a potpuna vizualna 1:1 podudarnost još nije certificirana. Promjene su opisane u CHANGELOG.md." \
  --prerelease \
  "dist/shiftora-${version}-debug.apk" "dist/shiftora-${version}-unsigned.aab"
