"""Remove obsolete generated excerpts without modifying any original recording.

The notification role intentionally uses the system sound until dedicated short
assets/cut points are approved. AlarmKit names the unmodified original MP3.
"""
from pathlib import Path
import struct

resources = Path(__file__).resolve().parents[2] / "iosApp/Generated/Resources"
for pattern in ("*_short.caf", "*_decoded.caf"):
    for path in resources.glob(pattern):
        path.unlink()

# ActivityKit's scheduled-start AlertConfiguration requires a nonoptional sound.
# A single 0.1-second silent system alert honors the user's silent reminder choice.
# This never starts AVFoundation and is not a background-audio loop.
description = struct.pack(">d4sIIIII", 8000.0, b"lpcm", 12, 2, 1, 1, 16)
samples = b"\x00" * 1600
def chunk(kind, data):
    return kind + struct.pack(">q", len(data)) + data
resources.mkdir(parents=True, exist_ok=True)
(resources / "salati_silent.caf").write_bytes(
    b"caff\x00\x01\x00\x00" + chunk(b"desc", description)
    + chunk(b"data", b"\x00\x00\x00\x00" + samples)
)
