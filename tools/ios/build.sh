#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
# iosArm64 for a signed device build; simulator architecture follows this Mac.
TARGET="${KOTLIN_TARGET:-IosSimulatorArm64}"
if [[ "$(uname -m)" == "x86_64" && -z "${KOTLIN_TARGET:-}" ]]; then TARGET=IosX64; fi
bash ./gradlew ":shared:linkDebugFramework${TARGET}" --console=plain
mkdir -p iosApp/Generated
BINARY_TARGET="$(echo "${TARGET:0:1}" | tr '[:upper:]' '[:lower:]')${TARGET:1}"
# ditto replaces the generated framework, never repository sources.
ditto "shared/build/bin/${BINARY_TARGET}/debugFramework/SalatiCore.framework" iosApp/Generated/SalatiCore.framework
python3 tools/ios/prepare_resources.py
python3 tools/ios/prepare_localizations.py
for recording in iosApp/Generated/Resources/*.mp3; do
  afconvert -f caff -d LEI16 -c 1 "$recording" "${recording%.mp3}_decoded.caf"
done
python3 tools/ios/short_sounds.py
sips -z 1024 1024 iosApp/Generated/Resources/Assets.xcassets/AppIcon.appiconset/Icon.png >/dev/null
xcodegen generate --spec iosApp/project.yml
