#!/usr/bin/env python3
"""RAZLOM original procedural PBR maps for the 1348 Izba kit.

Tileable synthesized BASECOLOR, DIRECTX NORMAL and ORM maps. No stock textures.
Requires Python, numpy, Pillow. Works offline. Not yet final AAA surface art.
"""
import argparse
import gc
import json
import subprocess
import sys
from pathlib import Path
import numpy as np
from PIL import Image

PROFILES = {
    "Timber_Log": {"albedo": (102, 66, 38), "metal": 0, "rough": .85, "strength": 2.8, "type": "grain", "seed": 1101},
    "Timber_Endgrain": {"albedo": (113, 75, 42), "metal": 0, "rough": .87, "strength": 3.2, "type": "rings", "seed": 1102},
    "Timber_Plank": {"albedo": (125, 78, 41), "metal": 0, "rough": .78, "strength": 2.4, "type": "grain", "seed": 1103},
    "Timber_Trim": {"albedo": (92, 55, 32), "metal": 0, "rough": .74, "strength": 2.1, "type": "grain", "seed": 1104},
    "Chinking_Dark": {"albedo": (53, 46, 37), "metal": 0, "rough": .96, "strength": 4.4, "type": "mortar", "seed": 1105},
    "Iron_Forged": {"albedo": (56, 56, 58), "metal": .88, "rough": .58, "strength": 2.5, "type": "iron", "seed": 1106},
    "Shingle_Wood": {"albedo": (100, 78, 50), "metal": 0, "rough": .90, "strength": 3.0, "type": "grain", "seed": 1107},
}

def lattice_noise(size: int, cells: int, rng: np.random.Generator) -> np.ndarray:
    """Smooth toroidal periodic lattice noise with repeatable edge behavior."""
    g = rng.uniform(-1, 1, (cells, cells)).astype(np.float32)
    ax = np.arange(size, dtype=np.float32) * cells / size
    i = np.floor(ax).astype(np.int32)
    t = ax - i
    t = t * t * (3 - 2 * t)
    j = (i + 1) % cells
    a = g[i[:, None], i[None, :]]
    b = g[i[:, None], j[None, :]]
    c = g[j[:, None], i[None, :]]
    d = g[j[:, None], j[None, :]]
    ty, tx = t[:, None], t[None, :]
    return ((a * (1-tx) + b*tx) * (1-ty) + (c*(1-tx)+d*tx)*ty).astype(np.float32)

def fractal(size, rng, frequencies=(3, 9, 25, 68), weights=(.5, .27, .15, .08)):
    out = np.zeros((size, size), dtype=np.float32)
    for freq, weight in zip(frequencies, weights):
        out += weight*lattice_noise(size, freq, rng)
    return out

def build(profile, size):
    rng = np.random.default_rng(profile["seed"])
    micro = fractal(size, rng, (12, 37, 93, 218), (.43, .29, .18, .10))
    macro = fractal(size, rng, (2, 4, 9, 17), (.45, .30, .16, .09))
    x = np.arange(size, dtype=np.float32)[None, :] / size
    y = np.arange(size, dtype=np.float32)[:, None] / size
    kind = profile["type"]
    if kind == "grain":
        warp = .012*macro + .008*lattice_noise(size, 7, rng)
        bands = np.sin(2*np.pi*(y*84 + 3.5*warp*84 + .16*np.sin(2*np.pi*x*4)))
        fine = np.sin(2*np.pi*(y*197 + 2.4*warp*197))
        grain = (.28*bands + .11*fine + .49*micro + .2*macro).astype(np.float32)
        check = np.maximum(0, np.sin(2*np.pi*(y*26 + warp*26))-.96)**2 * 15
        height = grain - check
        grime = .35*macro + .42*micro - .18*check
    elif kind == "rings":
        xx, yy = x-.5, y-.5
        radius = np.sqrt((xx*1.12)**2 + (yy*.84)**2)
        distortion = .019*macro + .008*lattice_noise(size, 11, rng)
        rings = np.sin(2*np.pi*(radius*31 + distortion*31))
        growth = np.sin(2*np.pi*(radius*68 + distortion*25))
        height = (.42*rings + .12*growth + .38*micro + .14*macro).astype(np.float32)
        grime = .46*macro + .30*micro - .09*rings
    elif kind == "mortar":
        grains = np.abs(fractal(size, rng, (24, 59, 133, 291), (.43,.29,.19,.09)))
        height = (.7*macro + .50*micro + .65*grains).astype(np.float32)
        grime = .58*macro + .4*micro - .34*grains
    else:
        hammer = fractal(size, rng, (14, 34, 76, 160), (.52,.28,.14,.06))
        pinholes = np.maximum(0, .28-hammer)**2 * 4
        height = (.47*macro + .45*micro - .7*pinholes).astype(np.float32)
        grime = .57*macro + .43*micro - .18*pinholes
    if kind == "grain" and profile["seed"] == 1107:
        grime = grime - .22*np.maximum(0, macro)

    base = np.array(profile["albedo"], dtype=np.float32)
    color = base[None,None,:] * (1 + grime*.31)[...,None]
    if kind == "mortar":
        color[...,0] += 10*micro
        color[...,1] += 9*micro
        color[...,2] += 8*micro
    if kind == "iron":
        color[...,0] += 8*macro
        color[...,1] += 8*macro
        color[...,2] += 6*macro
    color = np.uint8(np.clip(np.rint(color),0,255))

    height = np.ascontiguousarray(height)
    dx = (np.roll(height,-1,axis=1) - np.roll(height,1,axis=1))*profile["strength"]*.5
    dy = (np.roll(height,-1,axis=0) - np.roll(height,1,axis=0))*profile["strength"]*.5
    nx, ny, nz = -dx, +dy, np.ones_like(dx)  # DirectX tangent-normal Y convention
    length = np.sqrt(nx*nx + ny*ny + nz*nz)
    normal = np.uint8(np.clip(np.rint(np.stack(((nx/length*.5+.5),
              (ny/length*.5+.5), (nz/length*.5+.5)),axis=2)*255),0,255))
    rough = np.clip(profile["rough"]+.075*micro-.035*macro,.08,.99)
    cavities = np.maximum(0,-.45*height-.3*macro)
    ao = np.clip(1-.35*cavities,.6,1)
    orm = np.empty((size,size,3),dtype=np.uint8)
    orm[:,:,0] = np.uint8(np.rint(ao*255))
    orm[:,:,1] = np.uint8(np.rint(rough*255))
    orm[:,:,2] = round(profile["metal"]*255)
    return color,normal,orm

def export(out:Path,size:int,selected=None):
    out.mkdir(parents=True,exist_ok=True)
    rows=[]
    for name,profile in PROFILES.items():
        if selected and name not in selected:
            continue
        color,normal,orm=build(profile,size)
        maps={}
        for channel,pixels in (("BaseColor",color),("Normal_DX",normal),("ORM",orm)):
            fn=f"T_{name}_{channel}_{size}.png"
            Image.fromarray(pixels,mode="RGB").save(out/fn,format="PNG",optimize=True)
            maps[channel]=fn
        rows.append({"materialSlot":name,"size":[size,size],"maps":maps,
          "normalConvention":"DirectX Y-","channels":{"ORM_R":"procedural cavity AO",
          "ORM_G":"roughness","ORM_B":"metallic"}})
        print("Generated",name,flush=True)
        del color,normal,orm
        gc.collect()
    data={"assetPack":"RAZLOM_1348_IZBA_PBR","engine":"UE5 Windows PC",
       "size":size,"maps":rows,"authoring":"original procedural, no stock texture imagery",
       "limitations":"Draft PBR texture channels; not high-poly baked AO or UE5 tested."}
    (out/"texture_manifest.json").write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding="utf8")

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--size",type=int,default=1024,choices=[256,512,1024,2048])
    parser.add_argument("--out",type=Path,default=Path("Content/Art/Environment/Medieval1348/IzbaKit/Textures"))
    parser.add_argument("--material",choices=list(PROFILES),default=None)
    args=parser.parse_args()
    if args.material:
        export(args.out,args.size,[args.material])
        return
    # Fresh process per material: bounded peak RAM for 2K high-frequency maps.
    for name in PROFILES:
        subprocess.run([sys.executable,str(Path(__file__).resolve()),"--size",
                        str(args.size),"--out",str(args.out),"--material",name],check=True)
    rows=[]
    for name,p in PROFILES.items():
        rows.append({"materialSlot":name,"size":[args.size,args.size],
        "maps":{c:f"T_{name}_{c}_{args.size}.png" for c in ("BaseColor","Normal_DX","ORM")},
        "normalConvention":"DirectX Y-","channels":{"ORM_R":"procedural cavity AO",
        "ORM_G":"roughness","ORM_B":"metallic"}})
    data={"assetPack":"RAZLOM_1348_IZBA_PBR","engine":"UE5 Windows PC",
      "size":args.size,"maps":rows,"authoring":"original procedural, no stock texture imagery",
      "limitations":"Draft PBR texture channels; not high-poly baked AO or UE5 tested."}
    (args.out/"texture_manifest.json").write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding="utf8")

if __name__=="__main__":
    main()
