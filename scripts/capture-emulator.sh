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
adb shell uiautomator dump /sdcard/shiftora-uia.xml >/dev/null 2>&1 || true
if adb shell cat /sdcard/shiftora-uia.xml 2>/dev/null | grep -q 'Preskoči'; then
  adb shell input tap $((width - 160)) 240
  sleep 3
fi
adb exec-out screencap -p > "shiftora-${device}-home.png"
adb shell input tap $((width * 3 / 10)) "$nav_y"
sleep 2
adb exec-out screencap -p > "shiftora-${device}-calendar.png"
adb shell input tap $((width / 2)) "$nav_y"
sleep 2
adb exec-out screencap -p > "shiftora-${device}-statistics.png"
adb shell input tap $((width * 7 / 10)) "$nav_y"
sleep 2
adb exec-out screencap -p > "shiftora-${device}-salary.png"
adb shell input tap $((width * 9 / 10)) "$nav_y"
sleep 2
adb exec-out screencap -p > "shiftora-${device}-more.png"
for kind in onboarding home calendar statistics salary more; do
  test -s "shiftora-${device}-${kind}.png"
done
