"""Original checkpoint frame generator for RAZLOM: ZONA. Requires trimesh, pillow, numpy."""
from pathlib import Path
import json
import numpy as np
import trimesh
from PIL import Image,ImageDraw
from trimesh.visual.texture import TextureVisuals,SimpleMaterial
root=Path(__file__).resolve().parent
colors=[(91,89,80),(48,54,56),(139,57,43),(196,137,57),(29,39,42),(87,32,43),(37,98,104),(75,49,102)]
atlas=Image.new("RGB",(256,32));d=ImageDraw.Draw(atlas)
for i,c in enumerate(colors):
 d.rectangle((i*32,0,i*32+31,31),fill=c)
 for j in range(35):d.point((i*32+(j*17+7)%32,(j*11+i*7)%32),fill=tuple(max(0,min(255,v+(-12 if j%2 else 9))) for v in c))
atlas.save(root/"atlas.png")
mat=SimpleMaterial(image=atlas,metallicFactor=.05,roughnessFactor=.9)
scene=trimesh.Scene();parts=[]
def box(name,pos,size,tile):
 mesh=trimesh.creation.box(extents=size);mesh.apply_translation(pos)
 mesh.visual=TextureVisuals(uv=np.tile([(tile+.5)/8,.5],(len(mesh.vertices),1)),material=mat)
 scene.add_geometry(mesh,geom_name=name);parts.append(name)
for side,x in (("left",-1.65),("right",1.65)):
 box("concrete_post_"+side,(x,1.25,0),(.65,2.5,.65),0)
 box("concrete_foot_"+side,(x,.13,0),(1.05,.26,1.15),0)
 for z in (-.29,.29):box("rust_plate_"+side+str(z),(x,1.55,z),(.7,.22,.035),2)
 for y in (.5,1.2,1.9):box("bolt_"+side+str(y),(x,y,.347),(.09,.09,.035),3)
box("overhead_crossbar",(0,2.62,0),(3.65,.22,.24),1)
for i,x in enumerate(np.linspace(-1.05,1.05,6)):box("hazard_stripe_"+str(i),(float(x),2.62,.135),(.21,.16,.012),3 if i%2 else 4)
box("warning_sign",(-1.65,1.95,.365),(.45,.35,.04),5)
box("warning_sign_insert",(-1.65,1.95,.39),(.30,.12,.012),3)
box("lamp_housing",(0,2.86,0),(.38,.26,.32),4)
box("lamp_emissive",(0,2.86,.18),(.27,.15,.03),6)
for x in (-1.65,1.65):box("ground_weed_"+str(x),(x,.19,.58),(.38,.38,.18),6)
scene.export(root/"industrial_checkpoint_frame.glb")
check=trimesh.load(root/"industrial_checkpoint_frame.glb",force="scene")
assert len(check.geometry)==len(parts)
assert all(np.isfinite(g.vertices).all() and np.all(g.area_faces>1e-10) and getattr(g.visual,"uv",None) is not None for g in check.geometry.values())
(root/"manifest.json").write_text(json.dumps({"asset_id":"ZONA_ENV_INDUSTRIAL_CHECKPOINT_FRAME_001","version":"0.1.0","units":"meters","triangles":sum(len(g.faces) for g in check.geometry.values()),"parts":len(parts),"license":"CC0-1.0 original","godot_import":"NOT TESTED","android_fps":"NOT TESTED"},indent=2))
print("GLB verified:",len(parts),"meshes")
