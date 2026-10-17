#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
# Xcode embeds simulator entitlements in the proper Mach-O section and signs all nested
# products. Applying iPhone entitlements directly with codesign creates an invalid host
# signature on current macOS. The pseudo-identity '-' requires no certificate or account.
xcodebuild -project iosApp/Salati.xcodeproj -scheme Salati \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/.derivedData \
  ARCHS="$(uname -m)" ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=YES \
  CODE_SIGN_IDENTITY=- CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM= \
  build-for-testing
APP=iosApp/.derivedData/Build/Products/Debug-iphonesimulator/Salati.app
codesign --verify --deep --strict "$APP"
codesign --display --entitlements - "$APP"
