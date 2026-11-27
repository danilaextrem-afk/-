#!/usr/bin/env bash
set -eu
mkdir -p captures reports
model_test=ru.vladis.realtorai.data.DeviceInferenceTest
(cd realtor && gradle -PemulatorAbi "-Pandroid.testInstrumentationRunnerArguments.notClass=$model_test" :app:connectedDebugAndroidTest)
cp -r realtor/app/build/reports/androidTests reports/integration
adb install -r -g realtor/app/build/outputs/apk/debug/app-debug.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell settings put global anr_show_background 0
adb shell cmd uimode night yes
adb shell am start -W -n ru.vladis.realtorai/.MainActivity
capture_result=0
python3 realtor/ci/capture.py || capture_result=$?
if (( capture_result )); then
    adb exec-out screencap -p > captures/failed-screen.png
    adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' || true
fi
model_result=0
(cd realtor && gradle -PemulatorAbi "-Pandroid.testInstrumentationRunnerArguments.class=$model_test" :app:connectedDebugAndroidTest) || model_result=$?
cp -r realtor/app/build/reports/androidTests reports/model
adb logcat -d -s DeviceInferenceTest:I > captures/inference.txt
preview_result=0
(
    (cd realtor && gradle -PemulatorAbi :app:assemblePreview) &&
    adb install -r realtor/app/build/outputs/apk/preview/app-preview.apk &&
    adb shell am start -W -n ru.vladis.realtorai.preview5/ru.vladis.realtorai.MainActivity &&
    python3 realtor/ci/capture.py --preview
) > reports/release-candidate.txt 2>&1 || preview_result=$?
cat reports/release-candidate.txt
exit $((capture_result || model_result || preview_result))
