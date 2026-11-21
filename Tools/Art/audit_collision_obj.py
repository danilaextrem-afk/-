"""Audit OBJ collision proxy bounds against render mesh (not a UE5 cook test)."""
import argparse
import json
import math
from pathlib import Path

def load_obj(path):
    vertices, faces = [], []
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        p = line.split()
        if not p or p[0].startswith("#"):
            continue
        if p[0] == "v":
            if len(p) != 4:
                raise ValueError("vertex must have three coordinates")
            v = tuple(map(float, p[1:]))
            if not all(map(math.isfinite, v)):
                raise ValueError("non-finite vertex")
            vertices.append(v)
        elif p[0] == "f":
            if len(p) < 4:
                raise ValueError("face needs >=3 corners")
            indices = [int(x.split("/")[0]) for x in p[1:]]
            if any(i <= 0 or i > len(vertices) for i in indices):
                raise ValueError("unsupported or out-of-range face index")
            if len(set(indices)) < 3:
                raise ValueError("degenerate face")
            faces.append(indices)
    if not vertices or not faces:
        raise ValueError("OBJ must contain vertices and faces")
    return vertices, faces

def bounds(vertices):
    return [[min(v[i] for v in vertices), max(v[i] for v in vertices)] for i in range(3)]

def audit(render_path, proxy_path, tolerance=0.01):
    if tolerance < 0 or not math.isfinite(tolerance):
        raise ValueError("tolerance must be finite and non-negative")
    rv, rf = load_obj(render_path)
    pv, pf = load_obj(proxy_path)
    rb, pb = bounds(rv), bounds(pv)
    excess = [max(0, rb[i][0]-pb[i][0], pb[i][1]-rb[i][1]) for i in range(3)]
    return {
        "render_vertices": len(rv), "render_faces": len(rf),
        "proxy_vertices": len(pv), "proxy_faces": len(pf),
        "render_bounds": rb, "proxy_bounds": pb,
        "proxy_excess_m": excess,
        "proxy_outside_tolerance": any(x > tolerance for x in excess),
        "note": "AABB only; not convexity, watertightness, or UE5 collision cooking"
    }

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("render_obj")
    p.add_argument("collision_obj")
    p.add_argument("--tolerance", type=float, default=0.01)
    p.add_argument("--strict", action="store_true")
    args = p.parse_args()
    result = audit(args.render_obj, args.collision_obj, args.tolerance)
    print(json.dumps(result, indent=2))
    if args.strict and result["proxy_outside_tolerance"]:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
