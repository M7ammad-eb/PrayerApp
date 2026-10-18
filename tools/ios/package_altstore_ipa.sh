#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUTPUT="$PWD/build/unsigned-ipa"
SOURCE="$OUTPUT/DerivedData/Build/Products/Release-iphoneos/Salati.app"
# AltStore discovers requested capabilities from Mach-O entitlements, not a sidecar.
# Embed only ad-hoc signing metadata; AltStore must replace this with Apple-ID signing.
STAGING=$(mktemp -d "$OUTPUT/altstore.XXXXXX")
mkdir -p "$STAGING/Payload"
APP="$STAGING/Payload/Salati.app"
ditto "$SOURCE" "$APP"
codesign --force --sign - --timestamp=none --entitlements iosApp/Config/Salati.entitlements "$APP/PlugIns/SalatiWidgets.appex"
codesign --force --sign - --timestamp=none --entitlements iosApp/Config/Salati.entitlements "$APP"
codesign --verify --deep --strict "$APP"
python3 - "$APP" <<'PY'
import plistlib,subprocess,sys
from pathlib import Path
app=Path(sys.argv[1])
for bundle in [app,app/'PlugIns/SalatiWidgets.appex']:
    entitlements=plistlib.loads(subprocess.check_output(['codesign','--display','--entitlements','-',str(bundle)],stderr=subprocess.DEVNULL))
    assert entitlements['com.apple.security.application-groups']==['group.com.salati.prayertimes']
    signature=subprocess.run(['codesign','--display','--verbose=2',str(bundle)],capture_output=True,text=True,check=True)
    assert 'Signature=adhoc' in signature.stderr+signature.stdout
    assert not (bundle/'embedded.mobileprovision').exists()
print('Verified app/widget ad-hoc metadata requests the same App Group for AltStore provisioning')
PY
(cd "$STAGING" && ditto -c -k --sequesterRsrc --keepParent Payload "$OUTPUT/Salati-AltStore-ready.ipa")
python3 - <<'PY'
import hashlib,json,zipfile
from pathlib import Path
out=Path('build/unsigned-ipa');ipa=out/'Salati-AltStore-ready.ipa'
with zipfile.ZipFile(ipa) as archive:
    assert archive.testzip() is None
    assert not any(f.endswith('embedded.mobileprovision') or f.endswith('.debug.dylib') or f.endswith('/__preview.dylib') for f in archive.namelist())
digest=hashlib.sha256(ipa.read_bytes()).hexdigest()
info=json.loads((out/'build-info.json').read_text())
info.update({'signing':'ad-hoc capability metadata only; AltStore must re-sign with the user Apple ID before installation',
             'requestedAppGroups':['group.com.salati.prayertimes'],'sha256':digest,'bytes':ipa.stat().st_size})
(out/'AltStore-build-info.json').write_text(json.dumps(info,indent=2)+'\n')
(out/'Salati-AltStore-ready.ipa.sha256').write_text(digest+'  Salati-AltStore-ready.ipa\n')
(out/'AltStore-README.txt').write_text('Salati build 2 for AltStore Classic. Keep the SalatiWidgets extension when importing. This package carries ad-hoc App Group capability metadata, not Apple development/distribution signing or a provisioning profile. AltStore must sign it with your Apple ID. Open Salati once after installation to refresh the widget schedule.\n')
print('Validated AltStore-ready ARM64 iPhone IPA:',ipa,ipa.stat().st_size,'bytes',digest)
PY
