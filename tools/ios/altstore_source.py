"""Builds the AltStore source JSON for Salati.

usage: altstore_source.py <previous source.json or {}> <AltStore-build-info.json> <ipa download url> <output>

The newest build is placed first and the last ten builds are kept so AltStore can offer updates
and older versions. Both the current `versions` list and the older flat fields are written so
any AltStore release understands the file.
"""
import datetime
import json
import os
import sys
from pathlib import Path

previous_path, info_path, download_url, output_path = sys.argv[1:5]
repo = os.environ.get("GITHUB_REPOSITORY", "M7ammad-eb/PrayerApp")
icon = f"https://raw.githubusercontent.com/{repo}/ios/altstore/icon.png"
tint = "2E634F"
bundle_id = "com.salati.prayertimes.ios"

info = json.loads(Path(info_path).read_text(encoding="utf-8"))
try:
    previous = json.loads(Path(previous_path).read_text(encoding="utf-8"))
except (OSError, ValueError):
    previous = {}
old_versions = []
for app in previous.get("apps", []):
    if app.get("bundleIdentifier") == bundle_id:
        old_versions = app.get("versions", [])

now = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
new_version = {
    "version": info["version"],
    "buildVersion": str(info["buildNumber"]),
    "date": now,
    "localizedDescription": f"Build {info['buildNumber']} (commit {info['sourceCommit'][:7]}).",
    "downloadURL": download_url,
    "size": info["bytes"],
    "sha256": info["sha256"],
    "minOSVersion": info.get("minimumIOS", "26.0"),
}
versions = [new_version] + [v for v in old_versions if v.get("buildVersion") != new_version["buildVersion"]]
versions = versions[:10]
latest = versions[0]

app = {
    "name": "Salati",
    "bundleIdentifier": bundle_id,
    "developerName": "Mohammad Eb",
    "subtitle": "Prayer times, Qibla and Hijri calendar",
    "localizedDescription": "Accurate prayer times calculated on your iPhone, a Qibla compass, the Hijri calendar, "
                            "widgets, a countdown Live Activity and adhan alarms. Works offline.",
    "iconURL": icon,
    "tintColor": tint,
    "category": "lifestyle",
    "screenshots": [],
    "versions": versions,
    "appPermissions": {
        "entitlements": ["com.apple.security.application-groups"],
        "privacy": {
            "NSLocationWhenInUseUsageDescription": "Your location is used to find local prayer times and Qibla direction.",
            "NSAlarmKitUsageDescription": "Allow Salati to schedule prominent system alarms for prayers you choose.",
        },
    },
    # Flat fields for older AltStore versions.
    "version": latest["version"],
    "versionDate": latest["date"],
    "versionDescription": latest["localizedDescription"],
    "downloadURL": latest["downloadURL"],
    "size": latest["size"],
}

source = {
    "name": "Salati",
    "identifier": "com.salati.prayertimes.source",
    "subtitle": "Salati builds",
    "description": "Prayer times, Qibla and the Hijri calendar for iPhone.",
    "iconURL": icon,
    "website": f"https://github.com/{repo}",
    "tintColor": tint,
    "apps": [app],
    "news": [],
}
Path(output_path).write_text(json.dumps(source, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"AltStore source written: {len(versions)} build(s), newest {latest['version']} ({latest['buildVersion']})")
