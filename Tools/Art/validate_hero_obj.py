#!/usr/bin/env python3
"""Static original-OBJ geometry gate for RAZLOM artist deliveries.

Only geometry/MTL properties; NOT UE5 import, visual quality, PBR, FPS, collision
or a replacement for engine validation. No external Python packages required.
Usage:
    python Tools/Art/validate_hero_obj.py Content/Art/HeroProps/SM_GateWardenKey_1348_v1.obj
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

MIN_AREA2 = 2e-12
MAX_ABS_COORD = 1e4


def inspect(obj_path: Path) -> dict:
    vertices: list[tuple[float, float, float]] = []
    declared_mtls: set[str] = set()
    referenced_mtls: set[str] = set()
    errors: list[str] = []
    warnings: list[str] = []
    count = 0
    degenerate = 0
    vts = 0
    vns = 0
    face_missing_uv = 0
    area = 0.0
    used_vertex_ids: set[int] = set()
    material_name = ""
    mtllibs: list[str] = []
    objects: set[str] = set()

    for line_no, raw in enumerate(obj_path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split()
        kind = parts[0]

        try:
            if kind == "v":
                if len(parts) < 4:
                    raise ValueError("vertex requires xyz")
                v = tuple(float(t) for t in parts[1:4])
                if not all(math.isfinite(t) and abs(t) < MAX_ABS_COORD for t in v):
                    raise ValueError("invalid coordinate")
                vertices.append(v)
            elif kind == "vt":
                vts += 1
            elif kind == "vn":
                vns += 1
            elif kind == "o":
                objects.add(" ".join(parts[1:]))
            elif kind == "mtllib":
                mtllibs.extend(parts[1:])
            elif kind == "usemtl":
                material_name = " ".join(parts[1:])
                referenced_mtls.add(material_name)
            elif kind == "f":
                if len(parts) != 4:
                    raise ValueError("triangle-only asset; non-tri face")
                if not vertices:
                    raise ValueError("face before any vertices")
                ids = [int(ref.split("/")[0]) for ref in parts[1:]]
                if not all(1 <= index <= len(vertices) for index in ids):
                    raise ValueError("invalid/forward/negative face vertex index")
                if len(set(ids)) != 3:
                    raise ValueError("repeated vertex in face")
                used_vertex_ids.update(ids)
                count += 1
                face_missing_uv += int(any("/" not in ref for ref in parts[1:]))
                a, b, c = (vertices[index-1] for index in ids)
                u = tuple(b[i]-a[i] for i in range(3))
                v = tuple(c[i]-a[i] for i in range(3))
                cx = u[1]*v[2]-u[2]*v[1]
                cy = u[2]*v[0]-u[0]*v[2]
                cz = u[0]*v[1]-u[1]*v[0]
                length = math.sqrt(cx*cx+cy*cy+cz*cz)
                if length <= MIN_AREA2:
                    degenerate += 1
                area += length / 2.0
        except (ValueError, IndexError, OverflowError) as exc:
            errors.append(f"line {line_no}: {exc}")

    if not vertices or not count:
        errors.append("missing vertices or triangles")
    if degenerate:
        errors.append(f"{degenerate} degenerate triangles")
    if not mtllibs:
        warnings.append("no material-library reference")
    for lib in mtllibs:
        local_path = obj_path.parent / lib
        if not local_path.is_file():
            errors.append(f"material library not found: {local_path}")
            continue
        for raw in local_path.read_text(encoding="utf-8").splitlines():
            if raw.strip().startswith("newmtl "):
                declared_mtls.add(raw.strip()[7:])
    missing = referenced_mtls - declared_mtls
    if missing:
        errors.append(f"unknown material slots: {sorted(missing)}")
    if not vts or face_missing_uv:
        warnings.append("UV unwrap missing or face UV indices incomplete")
    if not vns:
        warnings.append("OBJ contains no explicit normals; review generated UE5 shading")
    if vertices and len(used_vertex_ids) < len(vertices):
        warnings.append(f"{len(vertices) - len(used_vertex_ids)} unused vertices")

    bounds = {}
    if vertices:
        for i, axis in enumerate("xyz"):
            vals = [p[i] for p in vertices]
            bounds[axis] = [round(min(vals), 6), round(max(vals), 6)]

    return {
        "asset": str(obj_path),
        "vertices": len(vertices),
        "triangles": count,
        "objects": len(objects),
        "mtl_slots": sorted(referenced_mtls),
        "mtl_defined": sorted(declared_mtls),
        "texture_vertices": vts,
        "normals": vns,
        "surface_area_m2": round(area, 6),
        "bounds_m": bounds,
        "errors": errors,
        "warnings": warnings,
        "static_pass": not errors,
        "verified_ue5": False,
        "verified_4k_fps": False,
        "art_approved": False,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("obj", nargs="+", type=Path)
    args = parser.parse_args()
    results = [inspect(path) for path in args.obj]
    for result in results:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    raise SystemExit(1 if any(result["errors"] for result in results) else 0)


if __name__ == "__main__":
    main()
