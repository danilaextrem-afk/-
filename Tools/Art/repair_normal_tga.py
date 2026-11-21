#!/usr/bin/env python3
"""Repair only RGB(4,4,4) invalid pixels in uncompressed RGB24 TGA normals."""
from pathlib import Path
import argparse
import struct

BAD = bytes((4, 4, 4))
NEUTRAL = bytes((255, 128, 128))  # TGA B,G,R, tangent-space +Z

def repair_bytes(raw: bytes) -> tuple[bytes, int]:
    if len(raw) < 18:
        raise ValueError("truncated header")
    id_len, cmap, kind, _, _, _, _, _, w, h, depth, desc = struct.unpack("<BBBHHBHHHHBB", raw[:18])
    if id_len != 0 or cmap != 0 or kind != 2 or depth != 24 or (desc & 0x30) != 0x20 or w == 0 or h == 0:
        raise ValueError("unsupported TGA format")
    if len(raw) != 18 + w * h * 3:
        raise ValueError("payload size mismatch")
    out = bytearray(raw)
    count = 0
    for pos in range(18, len(out), 3):
        if out[pos:pos+3] == BAD:
            out[pos:pos+3] = NEUTRAL
            count += 1
    return bytes(out), count

def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("files", nargs="+", type=Path)
    p.add_argument("--in-place", action="store_true", help="overwrite inputs; otherwise write .repaired.tga")
    p.add_argument("--expect", type=int, help="expected replacement count; mismatch aborts all writes")
    args = p.parse_args()
    results = [(path, *repair_bytes(path.read_bytes())) for path in args.files]
    total = sum(n for _, _, n in results)
    if args.expect is not None and total != args.expect:
        raise SystemExit(f"Expected {args.expect} replacements, got {total}; no files written")
    for path, content, count in results:
        dest = path if args.in_place else path.with_name(path.stem + ".repaired.tga")
        dest.write_bytes(content)
        print(f"{dest}: repaired={count}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
