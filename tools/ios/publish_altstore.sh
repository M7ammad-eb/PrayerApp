#!/usr/bin/env bash
# Publishes the AltStore-ready IPA as a GitHub release and refreshes the AltStore source.
# Runs in CI after the IPA is built; needs GH_TOKEN and GITHUB_REPOSITORY/GITHUB_SHA.
#
# Source URL (add once in AltStore, Sources > +):
#   https://github.com/<repo>/releases/download/altstore-source/source.json
set -euo pipefail

INFO=build/unsigned-ipa/AltStore-build-info.json
VERSION=$(python3 -c "import json;print(json.load(open('$INFO'))['version'])")
BUILD=$(python3 -c "import json;print(json.load(open('$INFO'))['buildNumber'])")
NAME="Salati-AltStore-ready-$VERSION-build$BUILD"
TAG="build-$BUILD"
SOURCE_TAG="altstore-source"
WORK=$(mktemp -d)

cp build/unsigned-ipa/Salati-AltStore-ready.ipa "$WORK/$NAME.ipa"

if gh release view "$TAG" >/dev/null 2>&1; then
  gh release upload "$TAG" "$WORK/$NAME.ipa" --clobber
else
  gh release create "$TAG" "$WORK/$NAME.ipa" --target "$GITHUB_SHA" --latest=false \
    --title "Salati $VERSION (build $BUILD)" \
    --notes "AltStore-ready IPA, commit ${GITHUB_SHA:0:7}. Install it from the AltStore source rather than downloading it by hand."
fi
URL="https://github.com/$GITHUB_REPOSITORY/releases/download/$TAG/$NAME.ipa"

# Merge the new build into the existing source (kept as an asset of a fixed release tag).
if gh release download "$SOURCE_TAG" --pattern source.json --dir "$WORK" --clobber >/dev/null 2>&1; then
  mv "$WORK/source.json" "$WORK/previous.json"
else
  echo '{}' > "$WORK/previous.json"
fi
python3 tools/ios/altstore_source.py "$WORK/previous.json" "$INFO" "$URL" "$WORK/source.json"

if gh release view "$SOURCE_TAG" >/dev/null 2>&1; then
  gh release upload "$SOURCE_TAG" "$WORK/source.json" --clobber
else
  gh release create "$SOURCE_TAG" "$WORK/source.json" --target "$GITHUB_SHA" --latest=false \
    --title "AltStore source" \
    --notes "Add https://github.com/$GITHUB_REPOSITORY/releases/download/$SOURCE_TAG/source.json in AltStore under Sources."
fi
echo "Published $NAME and refreshed the AltStore source."
