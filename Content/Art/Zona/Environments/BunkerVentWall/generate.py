"""Original RAZLOM ZONA bunker ventilation wall. pip install trimesh pillow numpy"""
import json, pathlib, numpy as np, trimesh
from PIL import Image, ImageDraw
root=pathlib.Path(__file__).parent
atlas=Image.new("RGB",(128,128))
draw=ImageDraw.Draw(atlas)
for i,c in enumerate(("#626d6a","#303b40","#a05c43","#b3a27b")):
 x=(i%2)*64;y=(i//2)*64;draw.rectangle((x,y,x+63,y+63),fill=c)
atlas.save(root/"atlas.png")
mat=trimesh.visual.material.PBRMaterial(baseColorTexture=atlas,metallicFactor=.15,roughnessFactor=.85)
scene=trimesh.Scene()
def cube(name,center,size,swatch):
 m=trimesh.creation.box(extents=size);m.apply_translation(center)
 v=[];f=[];uv=[]
 for face in m.faces:
  pts=m.vertices[face];start=len(v);v.extend(pts);f.append([start,start+1,start+2])
  n=np.cross(pts[1]-pts[0],pts[2]-pts[0]);axis=int(np.argmax(abs(n)));dims=[i for i in range(3) if i!=axis]
  uv.extend([[(swatch%2)*.5+.04+.42*((p[dims[0]]*1.7)%1),(swatch//2)*.5+.04+.42*((p[dims[1]]*1.7)%1)] for p in pts])
 m=trimesh.Trimesh(vertices=v,faces=f,process=False)
 m.visual=trimesh.visual.TextureVisuals(uv=np.array(uv),material=mat)
 scene.add_geometry(m,node_name=name,geom_name=name)
cube("wall",(0,1.5,.1),(2,3,.2),0)
cube("vent_frame",(0,2.08,-.08),(1.14,.78,.09),1)
cube("vent_recess",(0,2.08,-.138),(.96,.60,.03),1)
for i in range(8):cube("louver_%02d"%i,(0,1.81+i*.076,-.19),(.88,.038,.065),1)
for x in (-.52,.52):
 for y in (1.75,2.41):cube("bolt_%s_%s"%(x,y),(x,y,-.153),(.055,.055,.025),3)
for x in (-.79,.79):
 cube("pipe_%s"%x,(x,1.22,-.08),(.09,1.92,.12),2)
 for y in (.44,1.17,1.92):cube("clamp_%s_%s"%(x,y),(x,y,-.17),(.17,.09,.09),1)
cube("warning_plate",(0,.92,-.055),(.53,.30,.025),3)
cube("warning_mark",(0,.93,-.073),(.055,.18,.01),2)
cube("warning_dot",(0,.79,-.074),(.055,.045,.01),2)
cube("skirt",(0,.13,-.04),(2,.26,.13),1)
scene.export(root/"bunker_vent_wall.glb")
loaded=trimesh.load(root/"bunker_vent_wall.glb",force="scene")
assert sum(len(g.faces) for g in loaded.geometry.values())==324
assert all(np.isfinite(g.vertices).all() and len(g.visual.uv)==len(g.vertices) for g in loaded.geometry.values())
print("PASS: GLB roundtrip, 324 triangles, 27 parts")
