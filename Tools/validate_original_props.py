#!/usr/bin/env python3
"""Offline validation of Wavefront OBJ mesh data and Razlom asset manifest. No third-party packages."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "Content/Art/OriginalProps/asset_manifest.json"

def validate_obj(path):
    n_v=n_vt=n_faces=0
    mats=set()
    current_material=None
    with path.open(encoding="utf-8") as handle:
        for no,line in enumerate(handle,1):
            if line.startswith("v "):
                xyz=line.split()[1:]
                assert len(xyz)==3 and all(abs(float(x))<1000000 for x in xyz), (path,no)
                n_v+=1
            elif line.startswith("vt "):
                uv=line.split()[1:]
                assert len(uv)>=2,(path,no)
                _=[float(x) for x in uv]
                n_vt+=1
            elif line.startswith("usemtl "):
                current_material=line.split(None,1)[1].strip()
                mats.add(current_material)
            elif line.startswith("f "):
                parts=line.split()[1:]
                assert len(parts)==3,(path,no,"faces must be triangles")
                for part in parts:
                    idx=part.split("/")
                    assert 0<int(idx[0])<=n_v,(path,no,part)
                    if len(idx)>1 and idx[1]:
                        assert 0<int(idx[1])<=n_vt,(path,no,part)
                assert current_material is not None,(path,no,"no material")
                n_faces+=1
    assert n_v>0 and n_faces>0 and n_vt>0,(path,"empty geometry")
    return dict(vertices=n_v,uv_coords=n_vt,triangles=n_faces,materials=sorted(mats))

def main():
    d=json.loads(MANIFEST.read_text(encoding="utf-8"))
    artifacts = d["artifacts"]
    if not isinstance(artifacts, list) or not artifacts:
        raise SystemExit("Asset manifest must contain a nonempty artifacts list")
    if len(artifacts) != d["number_of_assets"]:
        raise SystemExit(f"Expected {d['number_of_assets']} assets, found {len(artifacts)}")
    failures=0
    for asset in artifacts:
        path=ROOT/asset["file"]
        try:
            stats=validate_obj(path)
            assert stats["vertices"]==asset["vertices"], (stats,asset["vertices"])
            assert stats["triangles"]==asset["triangles"], (stats,asset["triangles"])
            print("OK",asset["id"],stats)
        except Exception as exc:
            failures+=1
            print("FAIL",asset["id"],repr(exc))
    if failures:
        raise SystemExit(f"{failures} asset(s) failed")
    print(f"PASS: {len(d['artifacts'])} OBJ files validated; UE5 engine validation is separate.")

if __name__ == "__main__":
    main()
