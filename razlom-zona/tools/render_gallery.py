import bpy
from mathutils import Vector
from pathlib import Path
p=Path(__file__).resolve().parents[1]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=str(p/'assets/bunker/gallery.gltf'))
mesh=next(o for o in bpy.context.scene.objects if o.type=='MESH')
mesh.matrix_world=mesh.matrix_world.copy()
mx=mesh.matrix_world.copy()
mesh.parent=None
mesh.matrix_world=mx
for i in range(1,5):
    obj=mesh.copy()
    bpy.context.collection.objects.link(obj)
    obj.location+=Vector((0,i*4,-i))
sc=bpy.context.scene
sc.world=bpy.data.worlds.new('Sky')
sc.world.use_nodes=True
sc.world.node_tree.nodes['Background'].inputs[0].default_value=(.26,.33,.35,1)
sc.world.node_tree.nodes['Background'].inputs[1].default_value=.65
for i in range(5):
    bpy.ops.object.light_add(type='POINT',location=(.48,i*4+1.2,2.12-i))
    lamp=bpy.context.object.data
    lamp.energy=95
    lamp.color=(.65,.96,.82)
    lamp.shadow_soft_size=.24
def render(name,eye,target):
    bpy.ops.object.camera_add(location=eye)
    cam=bpy.context.object
    cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler()
    cam.data.lens=22
    sc.camera=cam
    sc.render.filepath=str(p/'docs/gallery'/name)
    bpy.ops.render.render(write_still=True)
sc.render.engine='CYCLES'
sc.cycles.samples=32
sc.cycles.use_denoising=False
sc.render.resolution_x=800
sc.render.resolution_y=480
sc.render.resolution_percentage=100
sc.render.image_settings.file_format='JPEG'
sc.render.image_settings.quality=82
sc.render.threads_mode='FIXED'
sc.render.threads=4
render('threshold.jpg',(1.05,-3.5,1.62),(-.14,5,.7))
render('return.jpg',(-.7,10,-1.0),(.3,1,1.48))
