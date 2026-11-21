#!/usr/bin/env python3
"""RAZLOM original prop UV/PBR work-in-progress builder.

Consumes existing authored OBJ and emits a separate UV+normal OBJ, shared PBR
atlas in TGA, material-library bindings, and JSON. No Blender or numpy required.
Material atlas is a first texel/UV pass, NOT a sculpt-baked normal/4K proof.
"""
from __future__ import annotations
import argparse
import json
import math
import struct
from collections import defaultdict
from pathlib import Path

BASE = {
    "iron": ((41, 46, 47), 198, 235, 0.29),
    "steel": ((102, 107, 107), 140, 245, 0.31),
    "bronze": ((127, 91, 49), 143, 235, 0.27),
    "oxide": ((34, 41, 40), 223, 205, 0.22),
    "oak": ((116, 70, 40), 199, 0, 0.47),
    "oak_dark": ((73, 41, 24), 214, 0, 0.47),
    "grain": ((61, 38, 23), 228, 0, 0.50),
    "recess": ((18, 22, 22), 242, 0, 0.15),
}

def clamp(x: float, lo: int = 0, hi: int = 255) -> int:
    return max(lo, min(hi, int(round(x))))

def read_obj(path: Path):
    vertices = []
    faces = []
    material = ""
    obj_name = ""
    for line_no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        words = raw.strip().split()
        if not words or words[0].startswith("#"):
            continue
        if words[0] == "v":
            coords = tuple(float(w) for w in words[1:4])
            if len(coords) != 3 or not all(math.isfinite(x) for x in coords):
                raise ValueError(f"Invalid vertex at line {line_no}")
            vertices.append(coords)
        elif words[0] == "usemtl":
            material = " ".join(words[1:])
            if material not in BASE:
                raise ValueError(f"Unrecognized art material {material!r}")
        elif words[0] == "o":
            obj_name = " ".join(words[1:])
        elif words[0] == "f":
            ids = tuple(int(w.split("/")[0]) for w in words[1:])
            if len(ids) != 3 or not all(1 <= k <= len(vertices) for k in ids):
                raise ValueError(f"Not a validated triangle at line {line_no}")
            if not material:
                raise ValueError(f"Material missing at line {line_no}")
            faces.append((ids, obj_name, material))
    if not vertices or not faces:
        raise ValueError("Expected nonempty vertices and faces")
    return vertices, faces

def sub(b, a):
    return tuple(b[i]-a[i] for i in range(3))

def cross(a, b):
    return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])

def normal(v):
    length = max(math.sqrt(sum(x*x for x in v)), 1e-12)
    return tuple(x/length for x in v)

def axis_for(face_normal):
    dominant = max(range(3), key=lambda k: abs(face_normal[k]))
    return ((1, 2), (0, 2), (0, 1))[dominant]

def layout(materials):
    cols = math.ceil(math.sqrt(len(materials)))
    rows = math.ceil(len(materials)/cols)
    return cols, rows, {m: (i % cols, i // cols) for i, m in enumerate(materials)}

def write_tga(path: Path, width: int, rgb_pixels: bytes):
    if len(rgb_pixels) != width*width*3:
        raise ValueError("Invalid RGB buffer length")
    header = struct.pack("<BBBHHBHHHHBB", 0, 0, 2, 0, 0, 0, 0, 0, width, width, 24, 0x20)
    out = bytearray(header)
    for i in range(0, len(rgb_pixels), 3):
        out.extend((rgb_pixels[i+2], rgb_pixels[i+1], rgb_pixels[i]))
    path.write_bytes(out)

def noise(u, v):
    return (math.sin(u*137.2+v*41.3)*0.45 + math.sin(u*479.4-v*271.3)*0.18
            + math.cos(u*823.6+v*317.7)*0.12)

def procedural_height(material, u, v):
    if material.startswith("oak") or material == "grain":
        return 0.49*math.sin(78*u+4*math.sin(11*v)) + 0.16*math.sin(173*u+v*19) + 0.12*noise(u, v)
    if material == "oxide":
        return 0.44*noise(u, v) + 0.18*math.sin(34*u)*math.sin(29*v)
    return 0.20*noise(u, v) + 0.07*math.sin(89*u+61*v)

def bake_atlases(size, materials, cols, rows, tiles, basename, outdir):
    base = bytearray(size*size*3)
    orm = bytearray(size*size*3)
    normmap = bytearray(size*size*3)
    materials_by_tile = {value: name for name, value in tiles.items()}
    for y in range(size):
        for x in range(size):
            tx = min(cols-1, x*cols//size)
            ty = min(rows-1, y*rows//size)
            mat = materials_by_tile.get((tx, ty))
            pos = (y*size+x)*3
            if mat is None:
                base[pos:pos+3] = bytes((3, 3, 3))
                orm[pos:pos+3] = bytes((255, 255, 0))
                normmap[pos:pos+3] = bytes((128, 128, 255))
                continue
            color, rough, metallic, strength = BASE[mat]
            uu = (x+0.5)*cols/size-tx
            vv = (y+0.5)*rows/size-ty
            grain = procedural_height(mat, uu, vv)
            n = noise(uu, vv)
            delta = grain*18 + n*10
            if mat.startswith("oak") or mat == "grain":
                rgb = (color[0]+delta, color[1]+delta*.60, color[2]+delta*.44)
                roughness = rough + grain*16
            elif mat == "oxide":
                rgb = (color[0]+delta*.45, color[1]+delta*.50, color[2]+delta*.54)
                roughness = rough + grain*14
            else:
                rgb = (color[0]+delta*.70, color[1]+delta*.71, color[2]+delta*.66)
                roughness = rough + grain*14
            base[pos:pos+3] = bytes(clamp(v) for v in rgb)
            orm[pos:pos+3] = bytes((clamp(235+grain*8), clamp(roughness), metallic))
            eps = 0.005
            dx = (procedural_height(mat, min(1, uu+eps), vv)
                  - procedural_height(mat, max(0, uu-eps), vv))/(2*eps)
            dy = (procedural_height(mat, uu, min(1, vv+eps))
                  - procedural_height(mat, uu, max(0, vv-eps)))/(2*eps)
            nx, ny, nz = normal((-dx*strength/80, dy*strength/80, 1))
            normmap[pos:pos+3] = bytes((clamp((nx+1)*127.5), clamp((ny+1)*127.5), clamp((nz+1)*127.5)))
    names = {
        "BaseColor": outdir / f"T_{basename}_BaseColor.tga",
        "Normal": outdir / f"T_{basename}_Normal.tga",
        "ORM": outdir / f"T_{basename}_ORM.tga",
    }
    for name, data in (("BaseColor", base), ("Normal", normmap), ("ORM", orm)):
        write_tga(names[name], size, data)
    return names

def build(source, destination, size=256):
    if size < 64 or size > 4096 or size & (size-1):
        raise ValueError("Image size must be power-of-two 64..4096")
    destination.mkdir(parents=True, exist_ok=True)
    vertices, faces = read_obj(source)
    mats = sorted({face[2] for face in faces})
    cols, rows, tiles = layout(mats)
    extrema = defaultdict(lambda: [[float("inf"), float("-inf")] for _ in range(3)])
    for ids, name, mat in faces:
        for index in ids:
            p = vertices[index-1]
            for axis in range(3):
                extrema[mat][axis][0] = min(extrema[mat][axis][0], p[axis])
                extrema[mat][axis][1] = max(extrema[mat][axis][1], p[axis])
    uv = []
    uv_lookup = {}
    normals = []
    n_lookup = {}
    rewritten_faces = []
    for ids, obj, mat in faces:
        a, b, c = (vertices[k-1] for k in ids)
        cr = cross(sub(b, a), sub(c, a))
        if math.sqrt(sum(x*x for x in cr)) < 1e-12:
            raise ValueError(f"Degenerate face in {obj}")
        n = normal(cr)
        axes = axis_for(n)
        vt_inds = []
        for index in ids:
            key = (index, mat, axes)
            if key not in uv_lookup:
                p = vertices[index-1]
                uvpoint = []
                for ax in axes:
                    lo, hi = extrema[mat][ax]
                    uvpoint.append((p[ax]-lo)/max(hi-lo, 1e-12))
                tile_x, tile_y = tiles[mat]
                pad = 0.012
                U = (tile_x+pad+(1-2*pad)*uvpoint[0])/cols
                V = (tile_y+pad+(1-2*pad)*uvpoint[1])/rows
                uv_lookup[key] = len(uv)+1
                uv.append((U, 1.0-V))
            vt_inds.append(uv_lookup[key])
        nkey = tuple(round(x, 6) for x in n)
        if nkey not in n_lookup:
            n_lookup[nkey] = len(normals)+1
            normals.append(n)
        rewritten_faces.append((ids, vt_inds, n_lookup[nkey], obj, mat))
    basename = source.stem.removeprefix("SM_")
    maps = bake_atlases(size, mats, cols, rows, tiles, basename, destination)
    mtl_path = destination / (source.stem + "_PBRdraft.mtl")
    mtl_lines = ["# Preview base color only; UE5 needs explicit BaseColor/Normal/ORM wiring."]
    for mat in mats:
        mtl_lines += [f"newmtl {mat}", "Kd 0.75 0.75 0.75", f"map_Kd {maps['BaseColor'].name}", "illum 2", ""]
    mtl_path.write_text("\n".join(mtl_lines), encoding="utf-8")
    out_obj = destination / (source.stem + "_UVdraft.obj")
    with out_obj.open("w", encoding="utf-8") as stream:
        stream.write("# UV draft; not production art approved\n")
        stream.write(f"mtllib {mtl_path.name}\n")
        for v in vertices:
            stream.write("v " + " ".join(f"{a:.6f}" for a in v) + "\n")
        for t in uv:
            stream.write("vt " + " ".join(f"{a:.6f}" for a in t) + "\n")
        for n in normals:
            stream.write("vn " + " ".join(f"{a:.6f}" for a in n) + "\n")
        previous = (None, None)
        for ids, uv_ids, norm_id, obj, mat in rewritten_faces:
            if obj != previous[0]:
                stream.write(f"o {obj}\n")
            if mat != previous[1]:
                stream.write(f"usemtl {mat}\n")
            stream.write("f " + " ".join(f"{idx}/{tid}/{norm_id}" for idx, tid in zip(ids, uv_ids)) + "\n")
            previous = obj, mat
    report = {
        "source": str(source), "output_obj": out_obj.name,
        "vertices": len(vertices), "triangles": len(faces),
        "uv_vertices": len(uv), "normals": len(normals),
        "materials": mats, "atlas_grid": [cols, rows], "texture_resolution": size,
        "textures": {key: val.name for key, val in maps.items()},
        "atlas_tiles": {mat: list(pos) for mat, pos in tiles.items()},
        "claims": {"uv_draft": True, "texture_backed_pbr_draft": True,
                   "sculpt_baked_normals": False, "ue5_import_verified": False,
                   "owner_art_approved": False},
    }
    (destination / "manifest_uv_pbr.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    return report

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("obj", type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--size", type=int, default=256)
    args = parser.parse_args()
    print(json.dumps(build(args.obj, args.out, args.size), indent=2))

if __name__ == "__main__":
    main()
