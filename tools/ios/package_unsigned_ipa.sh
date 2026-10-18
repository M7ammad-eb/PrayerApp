#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
# build.sh must first generate the IosArm64 device framework, not a simulator framework.
OUTPUT="$PWD/build/unsigned-ipa"
mkdir -p "$OUTPUT"
xcodebuild -project iosApp/Salati.xcodeproj -scheme Salati -configuration Release \
  -sdk iphoneos -destination 'generic/platform=iOS' -derivedDataPath "$OUTPUT/DerivedData" \
  ARCHS=arm64 ONLY_ACTIVE_ARCH=YES ENABLE_DEBUG_DYLIB=NO CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY= DEVELOPMENT_TEAM= build
APP="$OUTPUT/DerivedData/Build/Products/Release-iphoneos/Salati.app"
# ARM64 alone is insufficient: simulators also use ARM64. Verify the Mach-O platform.
for binary in "$APP/Salati" "$APP/PlugIns/SalatiWidgets.appex/SalatiWidgets"; do
  file "$binary"
  xcrun vtool -show-build "$binary" | tee "$OUTPUT/$(basename "$binary")-platform.txt"
  grep -Eq '^[[:space:]]*platform IOS[[:space:]]*$' "$OUTPUT/$(basename "$binary")-platform.txt"
done
if codesign --display "$APP" >/dev/null 2>&1; then
  echo "Expected an unsigned device application" >&2
  exit 1
fi
test ! -e "$APP/embedded.mobileprovision"
# A fresh staging directory avoids mixing simulator or previously signed products.
STAGING=$(mktemp -d "$OUTPUT/staging.XXXXXX")
mkdir -p "$STAGING/Payload"
ditto "$APP" "$STAGING/Payload/Salati.app"
(cd "$STAGING" && ditto -c -k --sequesterRsrc --keepParent Payload "$OUTPUT/Salati-unsigned.ipa")
python3 - <<'PY'
import hashlib,json,os,plistlib,subprocess,zipfile
from pathlib import Path
out=Path('build/unsigned-ipa');ipa=out/'Salati-unsigned.ipa'
with zipfile.ZipFile(ipa) as archive:
    assert archive.testzip() is None
    files=archive.namelist()
    for bundle in ['Payload/Salati.app','Payload/Salati.app/PlugIns/SalatiWidgets.appex']:
        info=plistlib.loads(archive.read(bundle+'/Info.plist'))
        assert info['CFBundleSupportedPlatforms']==['iPhoneOS'],info
        assert bundle+'/'+info['CFBundleExecutable'] in files
    assert 'Payload/Salati.app/places.db' in files
    assert len([f for f in files if f.startswith('Payload/Salati.app/') and f.endswith('.mp3')])==11
    assert not any(f.endswith('embedded.mobileprovision') for f in files)
    assert not any(f.endswith('.debug.dylib') or f.endswith('/__preview.dylib') for f in files), 'Preview/debug dylibs must not ship in the sideload IPA'
digest=hashlib.sha256(ipa.read_bytes()).hexdigest()
(out/'Salati-unsigned.ipa.sha256').write_text(digest+'  Salati-unsigned.ipa\n')
(out/'build-info.json').write_text(json.dumps({
    'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),
    'workflowRun':'https://github.com/'+os.environ.get('GITHUB_REPOSITORY','M7ammad-eb/PrayerApp')+'/actions/runs/'+os.environ.get('GITHUB_RUN_ID','local'),
    'platform':'iPhoneOS','architecture':'arm64','minimumIOS':'26.0',
    'configuration':'Release','signing':'unsigned; signing and provisioning required before installation',
    'sha256':digest,'bytes':ipa.stat().st_size},indent=2)+'\n')
print('Validated unsigned ARM64 iPhone IPA:',ipa,ipa.stat().st_size,'bytes',digest)
PY
