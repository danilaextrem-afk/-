#!/usr/bin/env python3
"""Strict RGB24 uncompressed TGA normal-map audit. No external dependencies."""
from __future__ import annotations
import argparse
from pathlib import Path
import struct

def audit_normal_tga(path: Path) -> dict:
    data = path.read_bytes()
    if len(data) < 18:
        raise ValueError("truncated TGA header")
    id_len, cmap, kind, cmap_start, cmap_len, cmap_depth, x, y, w, h, depth, descriptor = struct.unpack("<BBBHHBHHHHBB", data[:18])
    if id_len or cmap or kind != 2 or depth != 24 or (descriptor & 0x30) != 0x20:
        raise ValueError("expected no-ID, uncompressed, top-left RGB24 TGA")
    if not w or not h or len(data) != 18 + w*h*3:
        raise ValueError("invalid dimensions or payload length")
    invalid = 0
    worst_z = 255
    for i in range(18, len(data), 3):
        b, g, r = data[i:i+3]
        worst_z = min(worst_z, b)
        if b < 128 or (r == 4 and g == 4 and b == 4):
            invalid += 1
    return {"file": str(path), "width": w, "height": h,
            "invalid_normal_pixels": invalid, "minimum_blue": worst_z,
            "pass": invalid == 0}

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("files", nargs="+", type=Path)
    args = parser.parse_args()
    for path in args.files:
        result = audit_normal_tga(path)
        print(result)
        if not result["pass"]:
            return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
