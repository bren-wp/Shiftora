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
mkdir -p dist/apk dist/aab
gh run download "$run_id" --repo "$GITHUB_REPOSITORY" -n shiftora-debug-apk -D dist/apk
gh run download "$run_id" --repo "$GITHUB_REPOSITORY" -n shiftora-unsigned-aab -D dist/aab
mv dist/apk/app-debug.apk "dist/shiftora-${version}-debug.apk"
mv dist/aab/app-release.aab "dist/shiftora-${version}-unsigned.aab"
gh release create "$tag" --repo "$GITHUB_REPOSITORY" --target "$run_sha" \
  --title "Shiftora ${version} – Development Preview" \
  --notes "Development preview. Debug-signed APK for testing and UNSIGNED release AAB. Google Play requires production signing. Pixel-exact 1:1 UI parity and payroll regulatory compliance are not certified." \
  --prerelease \
  "dist/shiftora-${version}-debug.apk" "dist/shiftora-${version}-unsigned.aab"
