#!/usr/bin/env python3
"""RAZLOM Black Bell hero mesh/texture acceptance QA (not a UE5 visual-quality test)."""
import json, math, sys
from pathlib import Path
from PIL import Image

root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path(__file__).resolve().parents[1]
art=root/'Content/Art/HeroProps'
obj=art/'SM_BlackBell_Hero_v2.obj'
meta=json.loads((art/'BlackBell_Hero_v2.json').read_text(encoding='utf8'))
vertices=[]; n_uv=n_normal=n_faces=0; materials=set(); used=set(); degenerate=0; invalid=[]
for no,line in enumerate(obj.open(encoding='utf8'),1):
 if line.startswith('v '):
  p=tuple(float(x) for x in line.split()[1:4])
  if len(p)!=3 or not all(math.isfinite(x) for x in p):invalid.append(('vertex',no))
  vertices.append(p)
 elif line.startswith('vt '):n_uv+=1
 elif line.startswith('vn '):n_normal+=1
 elif line.startswith('usemtl '):used.add(line.split()[1])
 elif line.startswith('f '):
  parts=line.split()[1:]
  if len(parts)!=3:invalid.append(('polygon not triangle',no));continue
  idx=[]
  for part in parts:
   p=part.split('/')
   if len(p)!=3:invalid.append(('no UV/normal index',no));break
   vi,ui,ni=(int(x) for x in p)
   if not 1<=vi<=len(vertices) or not 1<=ui<=n_uv or not 1<=ni<=n_normal:invalid.append(('face index bounds',no))
   idx.append(vi-1)
  if len(idx)!=3:continue
  a,b,c=(vertices[i] for i in idx)
  ab=tuple(b[i]-a[i] for i in range(3));ac=tuple(c[i]-a[i] for i in range(3))
  normal=(ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0])
  if sum(x*x for x in normal)<1e-14:degenerate+=1
  n_faces+=1
with (art/'BlackBell_Hero_v2.mtl').open(encoding='utf8') as handle:
 for line in handle:
  if line.startswith('newmtl '):materials.add(line.split()[1])
errors=[]
if invalid:errors.append(f'{len(invalid)} invalid geometry references; first={invalid[:3]}')
if degenerate:errors.append(f'{degenerate} zero-area triangles')
if n_faces<170_000:errors.append('too few triangles for the authored hero mesh')
if len(vertices)<100_000:errors.append('vertex count unexpectedly low')
if n_uv!=len(vertices) or n_normal!=len(vertices):errors.append('UV/normal vertex count mismatch')
if not used.issubset(materials):errors.append('undefined material name '+str(used-materials))
if n_faces!=meta['triangles']:errors.append('manifest triangle count mismatch')
if len(vertices)!=meta['vertices']:errors.append('manifest vertex count mismatch')
for kind in ('bronze','iron','wood'):
 for mode in ('albedo','normal','roughness','metallic'):
  img=art/'Textures'/f'{kind}_{mode}.png'
  if not img.exists():errors.append(str(img)+' missing');continue
  try:
   with Image.open(img) as im:
    if im.size!=(1024,1024):errors.append(f'{img.name}: bad dimensions')
    if im.format!='PNG':errors.append(f'{img.name}: not PNG')
  except Exception as ex:errors.append(f'{img.name}: cannot open {ex}')
print(json.dumps({'vertices':len(vertices),'triangles':n_faces,'uv':n_uv,'normals':n_normal,
 'materials':len(materials),'degenerate_faces':degenerate,'textures':12,'errors':errors},indent=2))
if errors:raise SystemExit(1)
