"""RAZLOM real OBJ technical studio render using *actual imported mesh*.

Run in headless Blender, e.g.:
blender --background --python Tools/blender_cloud_preview.py -- \
    --mesh Content/Art/HeroProps/SM_BlackBell_Hero_v3.obj \
    --output build/cloud_preview/blackbell_v3_blender.png --width 1280 --height 720

This is not an UE5 screenshot, art approval or AAA-quality claim.
"""
import argparse
import math
import os
import sys
import bpy
from mathutils import Vector

parser = argparse.ArgumentParser()
parser.add_argument("--mesh",required=True)
parser.add_argument("--output",required=True)
parser.add_argument("--width",type=int,default=1280)
parser.add_argument("--height",type=int,default=720)
parser.add_argument("--samples",type=int,default=14)
args=parser.parse_args(sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else [])
assert os.path.isfile(args.mesh),args.mesh
assert 64 <= args.width <= 4096 and 64 <= args.height <= 2160
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)

# Prefer Blender 4.x built-in wavefront importer.
if hasattr(bpy.ops.wm,"obj_import"):
    bpy.ops.wm.obj_import(filepath=os.path.abspath(args.mesh))
else:
    bpy.ops.import_scene.obj(filepath=os.path.abspath(args.mesh))
objects=[o for o in bpy.context.scene.objects if o.type=="MESH"]
assert objects,"No real mesh geometry imported"

def principled_color(name):
    n=name.lower()
    if "gold" in n or "gilt" in n or "rim" in n or "rune" in n:
        return ((.27,.16,.056,1),.75,.3)
    if "patina" in n:
        return ((.025,.086,.078,1),.55,.65)
    if "bronze" in n:
        return ((.10,.072,.045,1),.83,.42)
    if "enamel" in n or "cyan" in n:
        return ((.015,.14,.125,1),.24,.35)
    if "oak" in n or "wood" in n:
        return ((.17,.075,.029,1),.0,.72)
    if "iron" in n:
        return ((.035,.042,.047,1),.85,.48)
    return ((.10,.09,.08,1),.5,.5)

for obj in objects:
    for m in obj.data.materials:
        if m is None:continue
        c,metal,rough=principled_color(m.name)
        m.use_nodes=True
        bs=m.node_tree.nodes.get("Principled BSDF")
        if bs is not None:
            bs.inputs["Base Color"].default_value=c
            bs.inputs["Metallic"].default_value=metal
            bs.inputs["Roughness"].default_value=rough

# Visualize real OBJ silhouette and material slot separation with controlled lighting.
bbox=[o.matrix_world @ Vector(corner) for o in objects for corner in o.bound_box]
center=sum(bbox,Vector((0,0,0)))/len(bbox)
extent=max(max((p-center).length for p in bbox),1)
def point_at(o,target):
    o.rotation_euler=(target-o.location).to_track_quat("-Z","Y").to_euler()

bpy.ops.object.camera_add(location=center+Vector((extent*1.7,-extent*2.4,extent*1.1)))
camera=bpy.context.object
camera.name="RAZLOM actual OBJ technical camera"
point_at(camera,center)
camera.data.type="ORTHO";camera.data.ortho_scale=extent*2.25
bpy.context.scene.camera=camera

def area(name,pos,power,size,color):
    bpy.ops.object.light_add(type="AREA",location=center+Vector(pos)*extent)
    o=bpy.context.object;o.name=name
    o.data.energy=power;o.data.shape="DISK";o.data.size=size*extent
    o.data.color=color
    point_at(o,center)
area("Bronze key",(-1.5,-1.8,1.9),2200,2.6,(1,.75,.54))
area("Cold rim",(1.7,1.4,1.0),1700,2,(.48,.68,1))
area("Top bounce",(.3,.5,2.7),850,1.5,(1,.95,.8))
world=bpy.context.scene.world
world.use_nodes=True
bg=world.node_tree.nodes.get("Background")
bg.inputs["Color"].default_value=(.021,.026,.036,1)
bg.inputs["Strength"].default_value=.5

scene=bpy.context.scene
scene.render.engine="CYCLES"
scene.cycles.device="CPU"
scene.cycles.samples=args.samples
# Ubuntu's packaged Blender can be built without OpenImageDenoiser.
# Disable denoising explicitly for portable CPU render farm jobs.
scene.cycles.use_denoising=False
for view_layer in scene.view_layers:
    view_layer.cycles.use_denoising=False
scene.render.resolution_x=args.width
scene.render.resolution_y=args.height
scene.render.resolution_percentage=100
scene.render.image_settings.file_format="PNG"
scene.render.filepath=os.path.abspath(args.output)
scene.render.film_transparent=False
scene.render.image_settings.color_mode="RGBA"
scene.render.use_file_extension=True
scene.camera.data.lens=46
try:scene.view_settings.view_transform="AgX"
except Exception:pass
os.makedirs(os.path.dirname(scene.render.filepath),exist_ok=True)
bpy.ops.render.render(write_still=True)
print("RAZLOM blender actual OBJ render:",scene.render.filepath)
