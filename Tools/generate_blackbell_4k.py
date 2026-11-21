#!/usr/bin/env python3
"""RAZLOM / Black Bell v3: original, native-resolution procedural PBR maps.

Runs with Python + numpy + pillow. Does NOT bake from a sculpt, does NOT claim
UE5 visual verification or AAA fidelity. Output is an intermediate art source.
    python Tools/generate_blackbell_4k.py --size 4096 --out build/BlackBell_4K
    python Tools/generate_blackbell_4k.py --size 256 --out /tmp/smoke
"""
from __future__ import annotations
import argparse
import gc
import json
from pathlib import Path
import numpy as np
from PIL import Image

def octave_noise(rng, size):
    """Native-resolution multi-scale noise; final octaves add real 4K detail."""
    weights = ((13, .32), (53, .25), (177, .17), (601, .13), (0, .13))
    result = np.zeros((size, size), np.float32)
    for cells, weight in weights:
        if cells:
            cells = min(cells, size)
            source = rng.integers(0, 256, (cells, cells), dtype=np.uint8)
            layer = np.asarray(Image.fromarray(source, 'L').resize((size, size), Image.Resampling.BICUBIC), dtype=np.float32) / 255.
        else:
            layer = rng.random((size, size), dtype=np.float32)
        result += weight * layer
        del layer
    return np.clip(result, 0, 1)

def u8(array):
    return np.clip(array, 0, 255).astype(np.uint8)

def save(array, path, mode):
    Image.fromarray(u8(array), mode).save(path, compress_level=4)

def material(group, size, output):
    rng = np.random.default_rng({'bronze': 1348, 'iron': 2026, 'wood': 777}[group])
    base = octave_noise(rng, size)
    small = octave_noise(rng, size)
    # Nonuniform oxide/rust pores and fine chisel marks, generated at full resolution.
    pores = np.maximum(small - (.63 if group != 'wood' else .76), 0.) * 3.
    grain = None
    if group == 'bronze':
        patina = np.clip((base - .51)*2.8 + pores*.42, 0, 1)
        channels = (70 + 90*base - 57*patina,
                    52 + 73*base + 33*patina,
                    36 + 58*base + 29*patina)
        rough = 111 + 86*base + 35*patina + 12*pores
        metal = 224 - 83*patina
        height = (base-.5)*.54 + (small-.5)*.25 - pores*.22
    elif group == 'iron':
        oxide = np.clip((base - .58)*2.5 + pores*.18, 0, 1)
        channels = (39 + 85*base + 27*oxide,
                    43 + 72*base + 11*oxide,
                    46 + 76*base)
        rough = 140 + 72*base + 28*oxide
        metal = 218 - 98*oxide
        height = (base-.5)*.39 + (small-.5)*.25 - pores*.4
    else:
        # Physical wood grain has directional streaks, unlike generic isotropic noise.
        x = np.arange(size, dtype=np.float32)[None, :]
        y = np.arange(size, dtype=np.float32)[:, None]
        grain = np.sin(x*(.24*4096/size) + base*16 + 2*np.sin(y*(.012*4096/size)))
        channels = (65 + 87*base + 25*grain,
                    32 + 57*base + 15*grain,
                    18 + 41*base + 8*grain)
        rough = 166 + 57*base - 7*grain
        metal = np.zeros((size,size),np.float32)
        height = .4*base + .27*small + .17*grain
    rgb = np.empty((size,size,3), np.uint8)
    for k,c in enumerate(channels):
        rgb[:,:,k] = u8(c)
    save(rgb, output / f'{group}_albedo.png', 'RGB')
    del rgb, channels
    save(rough, output / f'{group}_roughness.png', 'L')
    del rough
    save(metal, output / f'{group}_metallic.png', 'L')
    del metal
    # Tangent-space, +Z normal map using height derivatives.
    gy,gx = np.gradient(height)
    strength = 2.5 if group == 'wood' else 3.0
    nx = np.clip(-gx*strength, -0.9, 0.9)
    ny = np.clip(-gy*strength, -0.9, 0.9)
    norm = np.sqrt(1 + nx*nx + ny*ny)
    normal = np.empty((size,size,3), np.uint8)
    normal[:,:,0] = u8(127.5*(nx/norm+1))
    normal[:,:,1] = u8(127.5*(ny/norm+1))
    normal[:,:,2] = u8(127.5*(1/norm+1))
    save(normal, output / f'{group}_normal.png', 'RGB')
    del base,small,height,gx,gy,nx,ny,norm,normal,pores,grain
    gc.collect()

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--size', type=int, default=4096, choices=(256,512,1024,2048,4096))
    ap.add_argument('--out', type=Path, default=Path('build/BlackBell_4K'))
    a = ap.parse_args()
    a.out.mkdir(parents=True,exist_ok=True)
    for group in ('bronze','iron','wood'):
        material(group,a.size,a.out)
    maps = sorted(a.out.glob('*.png'))
    assert len(maps) == 12, len(maps)
    for p in maps:
        with Image.open(p) as im:
            assert im.size == (a.size,a.size), (p,im.size)
    manifest = {
      'asset':'RAZLOM Black Bell 1348',
      'texture_resolution':f'{a.size}x{a.size}',
      'materials':['bronze','iron','wood'],
      'texture_count':len(maps),
      'source':'Tools/generate_blackbell_4k.py',
      'method':'deterministic native-resolution procedural generation, not upscaling',
      'status':'WIP: NOT baked from sculpt, not imported into UE5, not approved AAA',
      'textures':[p.name for p in maps],
    }
    (a.out/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print('PASS:',a.size,'square; 12 valid procedural PBR maps generated:',a.out)

if __name__ == '__main__':
    main()
