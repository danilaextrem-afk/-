#!/usr/bin/env python3
"""Validate original B2 mesh and render from the ACTUAL OBJ in Blender if installed.
Run: blender -b -P preview_blender.py -- <absolute path to old_master.obj>
No network, external asset packs, or paid services.
"""
import bpy, sys, json, math, os
from mathutils import Vector
argv=sys.argv
path=argv[argv.index("--")+1] if "--" in argv else os.path.join(os.path.dirname(__file__),"old_master.obj")
bpy.ops.object.select_all(action="SELECT"); bpy.ops.object.delete(use_global=False)
bpy.ops.wm.obj_import(filepath=os.path.abspath(path))
meshes=[o for o in bpy.context.scene.objects if o.type=="MESH"]
assert meshes, "OBJ has no meshes"
triangles=sum(len(p.vertices)-2 for o in meshes for p in o.data.polygons)
verts=sum(len(o.data.vertices) for o in meshes)
assert triangles>0 and verts>0
for o in meshes:
    assert len(o.data.uv_layers)>0, "UV missing"
    for p in o.data.polygons: assert p.area>1e-9, "degenerate face"
print(json.dumps({"mesh_objects":len(meshes),"vertices":verts,"triangles":triangles,"uv":True}))
for o in meshes:
    o.select_set(True)
bpy.ops.object.origin_set(type="ORIGIN_GEOMETRY",center="BOUNDS")
bpy.ops.object.select_all(action="DESELECT")
world=bpy.context.scene.world
world.color=(0.07,0.07,0.07)
bpy.ops.object.light_add(type="AREA",location=(400,-500,850))
bpy.context.object.data.energy=2500; bpy.context.object.data.shape="DISK"; bpy.context.object.data.size=650
bpy.ops.object.camera_add(location=(850,-1150,750))
cam=bpy.context.object
target=Vector((0,0,330))
cam.rotation_euler=(target-cam.location).to_track_quat("-Z","Y").to_euler()
bpy.context.scene.camera=cam
scene=bpy.context.scene
scene.render.engine="CYCLES"; scene.cycles.samples=16
scene.render.resolution_x=1024; scene.render.resolution_y=1024; scene.render.resolution_percentage=100
scene.render.filepath=os.path.join(os.path.dirname(path),"old_master_mesh_preview.png")
bpy.ops.render.render(write_still=True)
print("ACTUAL MESH PREVIEW:",scene.render.filepath)
