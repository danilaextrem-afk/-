#!/usr/bin/env python3
"""Check the actual 21 procedural PBR maps in the 1348 Izba kit."""
import argparse
import json
from pathlib import Path
import numpy as np
from PIL import Image

NAMES = ("Timber_Log","Timber_Endgrain","Timber_Plank","Timber_Trim",
         "Chinking_Dark","Iron_Forged","Shingle_Wood")

def validate(root:Path,size:int):
    manifest = json.loads((root/"texture_manifest.json").read_text(encoding="utf8"))
    assert manifest["size"]==size and len(manifest["maps"])==7
    assert {x["materialSlot"] for x in manifest["maps"]}==set(NAMES)
    checked=0
    for name in NAMES:
        images={}
        for suffix in ("BaseColor","Normal_DX","ORM"):
            file=root/f"T_{name}_{suffix}_{size}.png"
            assert file.is_file(),file
            with Image.open(file) as im:
                im.verify()
            with Image.open(file) as im:
                assert im.size==(size,size) and im.mode=="RGB",file
                images[suffix]=np.asarray(im,dtype=np.uint8)
            checked+=1
        base=images["BaseColor"].astype(np.float32)
        normal=images["Normal_DX"].astype(np.float32)
        orm=images["ORM"].astype(np.float32)
        assert np.std(base[:,:,0])>1.1,name+": flat albedo"
        assert normal[:,:,2].mean()>180,name+": invalid normal Z"
        length=np.sqrt(np.sum((normal/255*2-1)**2,axis=2))
        assert np.abs(length-1).mean()<.025,name+": nonunit normal"
        assert 5<=orm[:,:,1].min()<=orm[:,:,1].max()<=255,name+": roughness"
        assert orm[:,:,0].min()>40,name+": cavity AO"
        if name=="Iron_Forged":
            assert orm[:,:,2].min()>=210 and orm[:,:,2].max()<=235,name+": iron metallic"
        else:
            assert orm[:,:,2].max()==0,name+": nonmetal has metallic signal"
        print("PASS",name)
    print("PASS",checked,"PNG maps; format, dimensions, normals, ORM channels")
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--size",type=int,default=1024)
    p.add_argument("--dir",type=Path,default=Path("Content/Art/Environment/Medieval1348/IzbaKit/Textures"))
    a=p.parse_args()
    validate(a.dir,a.size)
if __name__=="__main__":
    main()
