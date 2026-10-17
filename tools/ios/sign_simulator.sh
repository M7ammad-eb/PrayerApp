#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
APP="${1:-iosApp/.derivedData/Build/Products/Debug-iphonesimulator/Salati.app}"
PLATFORM=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleSupportedPlatforms:0' "$APP/Info.plist")
if [[ "$PLATFORM" != "iPhoneSimulator" ]]; then
  echo "This helper signs only local simulator builds, never iPhone device builds." >&2
  exit 1
fi
codesign --force --sign - --generate-entitlement-der --entitlements iosApp/Config/Simulator.entitlements "$APP/PlugIns/SalatiWidgets.appex"
codesign --force --sign - --generate-entitlement-der --entitlements iosApp/Config/Simulator.entitlements "$APP"
codesign --display --entitlements - "$APP"
