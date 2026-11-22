# Zalesye facade rework — Blender-free procedural mesh generator
# Requires: pip install trimesh pillow numpy
# Run: python generate.py
import trimesh, numpy as np, json
from PIL import Image, ImageDraw
from pathlib import Path
p=Path(__file__).parent
palette=[(57,62,66),(87,85,77),(123,108,90),(43,49,53),(112,46,47),(45,87,86),(38,36,43),(160,128,83),(68,71,61),(29,35,39),(99,86,72)]
atlas=Image.new("RGB",(256,256))
d=ImageDraw.Draw(atlas)
for i,c in enumerate(palette):
 x=i%4*64;y=i//4*64
 d.rectangle((x,y,x+63,y+63),fill=c)
atlas.save(p/"atlas.png")
s=trimesh.Scene()
def part(name,pos,size,tile):
 m=trimesh.creation.box(extents=size);m.apply_translation(pos)
 uv=np.tile([(tile%4+.5)/4,(tile//4+.5)/4],(len(m.vertices),1))
 m.visual=trimesh.visual.texture.TextureVisuals(uv=uv,image=atlas)
 s.add_geometry(m,geom_name=name)
for name,pos,size,tile in [
 ("plinth",(0,.35,0),(6,.7,.65),0),
 ("left_wall",(-2.25,2.15,0),(1.5,2.9,.48),2),
 ("middle_wall",(-.65,2.65,0),(.95,1.9,.48),1),
 ("right_wall",(2.1,2.15,0),(1.8,2.9,.48),2),
 ("lintel",(0,3.55,0),(6,.4,.62),1),
 ("broken_roof",(0,4.08,-.14),(6.6,.27,1.15),3),
 ("door_shadow",(.55,1.6,-.29),(1.38,2.42,.08),11),
 ("door_panel",(.55,1.53,-.42),(1.12,2.24,.10),5),
 ("window_void",(-1.65,2.18,-.31),(1.22,1.25,.09),11),
 ("fuse_box",(2.3,1.8,-.42),(.47,.66,.19),8),
 ("sign",(2.3,3.2,-.47),(1.25,.33,.06),4),
 ("lamp_mount",(.5,3.32,-.46),(.22,.13,.20),9)]:
 part(name,pos,size,tile)
for i in range(11):
 if i not in (3,7):part("roof_sheet_%02d"%i,(-2.9+i*.58,4.31,-.2),(.54,.10,1.35),3 if i%4 else 4)
for i,(x,z) in enumerate([(-2.6,-.7),(-2.15,-.95),(2.6,-.8),(1.9,-1.0)]):
 part("rubble_%d"%i,(x,.10,z),(.23+i*.06,.16,.27),i%3)
 for j in range(3):part("weed_%d_%d"%(i,j),(x+j*.07-.06,.23,z+j*.06),(.035,.31+j*.08,.035),8)
for i in range(3):part("step_%d"%i,(.55,.14-i*.06,-.85-i*.34),(1.75+i*.2,.18,.4),0)
s.export(p/"zalesye_facade_rework.glb")
r=trimesh.load(p/"zalesye_facade_rework.glb",force="scene")
triangles=sum(len(g.faces) for g in r.geometry.values())
assert triangles>0 and all(np.all(g.area_faces>1e-9) for g in r.geometry.values())
print("GLB export/reimport PASS",triangles,"triangles",len(r.geometry),"parts")
