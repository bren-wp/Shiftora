#!/usr/bin/env bash
set -euo pipefail
device="${1:-pixel_4}"
package="com.brendigo.shiftora"
apk="app/build/outputs/apk/debug/app-debug.apk"
test -s "$apk"
adb install -r "$apk"
adb shell am force-stop "$package" || true
adb shell am start -W -n "$package/.MainActivity"
sleep 4
adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | grep "$package"
size="$(adb shell wm size | grep -m1 'Physical size' | awk '{print $3}' | tr -d '\r')"
width="${size%x*}"
height="${size#*x}"
test "$width" -gt 300
test "$height" -gt 700
nav_y=$((height - 180))
adb exec-out screencap -p > "shiftora-${device}-onboarding.png"
# Use the actual UI element bounds instead of hard-coded pixel taps. This also
# tolerates status-bar and font scaling changes between API 30 and API 36.
tap_label() {
  local label="$1" coords
  adb shell uiautomator dump /sdcard/shiftora-uia.xml >/dev/null 2>&1 || return 1
  adb exec-out cat /sdcard/shiftora-uia.xml > /tmp/shiftora-uia.xml || return 1
  coords="$(python3 - "$label" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse("/tmp/shiftora-uia.xml").getroot()
    for node in root.iter("node"):
        text = node.get("text", "")
        desc = node.get("content-desc", "")
        if text == sys.argv[1] or desc == sys.argv[1]:
            bounds = re.findall(r"\d+", node.get("bounds", ""))
            if len(bounds) == 4:
                x1,y1,x2,y2 = map(int,bounds)
                print((x1+x2)//2, (y1+y2)//2)
                break
except (ET.ParseError, FileNotFoundError):
    pass
PY
)"
  test -n "$coords" || return 1
  read -r x y <<< "$coords"
  adb shell input tap "$x" "$y"
}
# Validate the app stays foreground. In an earlier API 36 run tapping a
# clipped bottom item hit Android's Home button; distinct wallpaper frames
# falsely appeared to be successful navigation screenshots.
assert_app_foreground() {
  if ! adb shell dumpsys activity activities |
      grep -E 'mResumedActivity|topResumedActivity' | grep -q "$package"; then
    echo "Screenshot capture left Shiftora: Android launcher/another app foreground" >&2
    exit 1
  fi
}
assert_screen_text() {
  local expected="$1"
  adb shell uiautomator dump /sdcard/shiftora-uia.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/shiftora-uia.xml > /tmp/shiftora-uia.xml
  if ! python3 - "$expected" <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse("/tmp/shiftora-uia.xml").getroot()
expected = sys.argv[1]
if not any(expected in n.get("text","") or expected in n.get("content-desc","")
           for n in root.iter("node")):
    print("Expected screen label not visible:", expected, file=sys.stderr)
    sys.exit(1)
PY
  then
    echo "Screenshot navigation failed on: $expected" >&2
    exit 1
  fi
}
if tap_label "Preskoči"; then
  sleep 3
fi
# The Gradle connectedAndroidTest task uninstalls its test runner APK on
# completion. Re-install *only* the instrumentation APK before invoking the
# screenshot-only fixture; the regular production APK is already installed.
test_apk="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
test -s "$test_apk"
adb install -r "$test_apk"
# CI-only fixture: use a real test runner, never debug menu hooks or data
# included in production. Without it the screenshots contain only empty
# calendar cells and zero-value salary/statistics, masking real UI overflows.
fixture_log="$(adb shell am instrument -w \
  -e class "$package.ScreenshotFixtureTest#seedScreenshotMonth" \
  -e shiftoraScreenshotFixture true \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" | tr -d '\r')"
printf '%s\n' "$fixture_log"
if ! printf '%s\n' "$fixture_log" | grep -q 'OK (1 test)'; then
  echo "Failed to seed representative screenshot data" >&2
  exit 1
fi
adb shell am force-stop "$package" || true
adb shell am start -W -n "$package/.MainActivity"
sleep 3
assert_app_foreground
assert_screen_text "Danas je"
adb exec-out screencap -p > "shiftora-${device}-home.png"
tap_label "Kalendar" || adb shell input tap $((width * 3 / 10)) "$nav_y"
sleep 2
assert_app_foreground
assert_screen_text "Mjesec"
adb exec-out screencap -p > "shiftora-${device}-calendar.png"
tap_label "Statistika" || adb shell input tap $((width / 2)) "$nav_y"
sleep 2
assert_app_foreground
assert_screen_text "Statistika i plaća"
adb exec-out screencap -p > "shiftora-${device}-statistics.png"
tap_label "Plaća" || adb shell input tap $((width * 7 / 10)) "$nav_y"
sleep 2
assert_app_foreground
assert_screen_text "Pregled radnih sati i dodataka."
adb exec-out screencap -p > "shiftora-${device}-salary.png"
tap_label "Više" || adb shell input tap $((width * 9 / 10)) "$nav_y"
sleep 2
assert_app_foreground
assert_screen_text "Alati"
adb exec-out screencap -p > "shiftora-${device}-more.png"
for kind in onboarding home calendar statistics salary more; do
  test -s "shiftora-${device}-${kind}.png"
done
# Previous screenshot jobs silently saved onboarding in *every* destination.
# A screenshot matrix cannot certify navigation unless the frames are distinct.
distinct="$(sha256sum shiftora-"${device}"-{onboarding,home,calendar,statistics,salary,more}.png |
  awk '{print $1}' | sort -u | wc -l)"
if test "$distinct" -ne 6; then
  echo "Invalid UI screenshot capture: expected six distinct screens, found $distinct" >&2
  exit 1
fi
