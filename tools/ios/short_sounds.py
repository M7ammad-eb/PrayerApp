"""Trim decoded PCM CAF files to 25s using macOS afconvert output and Python's chunk parser.
No source recording changes. Full MP3 remains available for foreground AVFoundation playback.
"""
from pathlib import Path
import struct
for path in Path('iosApp/Generated/Resources').glob('*_decoded.caf'):
    raw=path.read_bytes(); pos=8; chunks=[]; rate=None; bytes_per_packet=None
    while pos+12<=len(raw):
        kind=raw[pos:pos+4]; size=struct.unpack('>q',raw[pos+4:pos+12])[0];pos+=12
        body=raw[pos:pos+size] if size>=0 else raw[pos:];pos+=len(body)
        if kind==b'desc':
            rate=struct.unpack('>d',body[:8])[0]
            bytes_per_packet=struct.unpack('>I',body[16:20])[0]
            frames_per_packet=struct.unpack('>I',body[20:24])[0]
            assert frames_per_packet==1 and bytes_per_packet>0
        if kind==b'data':
            assert rate and bytes_per_packet
            body=body[:4]+body[4:4+int(rate*25)*bytes_per_packet]
        # Packet tables/statistics refer to the original stream and are omitted for trimmed PCM.
        if kind not in [b'pakt',b'peak',b'ovvw']:
            chunks.append(kind+struct.pack('>q',len(body))+body)
    path.with_name(path.name.replace('_decoded.caf','_short.caf')).write_bytes(raw[:8]+b''.join(chunks))
    path.unlink()
