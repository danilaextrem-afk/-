"""Blender 4.3: assemble pinned original art into one metre-scale GLB and render it."""
from pathlib import Path
import sys
import tempfile

import bpy
import bmesh
import numpy as np
from mathutils import Vector, Matrix

ROOT = Path(__file__).resolve().parents[2]
WORK = tempfile.TemporaryDirectory(prefix="razlom-entry-")
TEMP = Path(WORK.name)
INPUT = ROOT / "SourceArt/Integration/Inputs"


def obj(path, scale):
    file = INPUT / path
    bpy.ops.wm.obj_import(filepath=str(file), forward_axis='Y', up_axis='Z',
                          global_scale=scale, use_split_groups=False, use_split_objects=False)
    bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
    return bpy.context.selected_objects[0]


def noise(grid_x, grid_y, rng, size):
    knots = rng.random((grid_y, grid_x)).astype(np.float32)
    x = np.arange(size) * grid_x / size
    y = np.arange(size) * grid_y / size
    ix, iy = x.astype(int), y.astype(int)
    fx, fy = x - ix, y - iy
    fx = fx * fx * (3 - 2 * fx)
    fy = (fy * fy * (3 - 2 * fy))[:, None]
    rows = knots[:, ix] * (1 - fx) + knots[:, (ix + 1) % grid_x] * fx
    return rows[iy] * (1 - fy) + rows[(iy + 1) % grid_y] * fy


def plate(name, color, rough, metallic, seed):
    size = 1024
    rng = np.random.default_rng(seed)
    grain = rng.random((size, size))
    coarse = noise(10, 10, rng, size)
    runs = noise(90, 5, rng, size)
    height = .55 * noise(180, 180, rng, size) + .45 * grain
    variation = .55 + .36 * coarse + .30 * runs + .32 * height
    corrosion = np.clip((coarse * .42 + runs * .30 + grain * .28 - .73) * 6, 0, 1)
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    shader = nodes.get("Principled BSDF")
    shader.inputs["Metallic"].default_value = metallic
    def texture(label, rgb, colorspace):
        image = bpy.data.images.new(name + "_" + label, size, size, alpha=False)
        image.colorspace_settings.name = colorspace
        pixels = np.ones((size, size, 4), dtype=np.float32)
        pixels[:, :, :3] = rgb
        image.pixels.foreach_set(pixels.ravel())
        image.filepath_raw = str(TEMP / (image.name + ".png"))
        image.file_format = 'PNG'
        image.save()
        image.pack()
        node = nodes.new("ShaderNodeTexImage")
        node.image = image
        return node.outputs["Color"]
    base = variation[:,:,None] * np.array(color)
    base = base * (1-corrosion[:,:,None]*.55) + corrosion[:,:,None]*np.array([.12,.057,.026])
    links.new(texture("Base", base, "sRGB"), shader.inputs["Base Color"])
    links.new(texture("Rough", np.clip(rough + (height-.5)*.25 + corrosion*.24,0,1)[:,:,None], "Non-Color"), shader.inputs["Roughness"])
    dy, dx = np.gradient(height)
    normal = np.stack((-dx, -dy, np.full_like(dx, .6)), axis=2)
    normal /= np.linalg.norm(normal,axis=2)[:,:,None]
    bump = nodes.new("ShaderNodeNormalMap")
    bump.inputs["Strength"].default_value = .25
    links.new(texture("Normal", normal*.5+.5,"Non-Color"),bump.inputs["Color"])
    links.new(bump.outputs["Normal"],shader.inputs["Normal"])
    return mat


def flat(name, color, metal=0, rough=.4, glow=0):
    mat=bpy.data.materials.new(name)
    mat.use_nodes=True
    node=mat.node_tree.nodes.get("Principled BSDF")
    for key,val in [("Base Color",(*color,1)),("Metallic",metal),("Roughness",rough),
                    ("Emission Color",(*color,1)),("Emission Strength",glow)]:
        node.inputs[key].default_value=val
    return mat


def bevel(mesh, width):
    bpy.context.view_layer.objects.active=mesh
    mod=mesh.modifiers.new("Machined edges","BEVEL")
    mod.width=width
    mod.segments=3
    bpy.ops.object.modifier_apply(modifier=mod.name)


def area(name, xyz, target, power, color, size):
    bpy.ops.object.light_add(type='AREA',location=xyz)
    light=bpy.context.object
    light.name=name
    light.rotation_euler=(Vector(target)-light.location).to_track_quat('-Z','Y').to_euler()
    light.data.energy=power
    light.data.color=color
    light.data.shape='DISK'
    light.data.size=size


bpy.ops.wm.read_factory_settings(use_empty=True)
wall=obj("PanelEntrance9.obj", .01)
wall.name="SM_EntranceNine"
stone=plate("Rainwashed aggregate",(.31,.31,.27),.79,0,10)
steel=plate("Green painted steel",(.081,.16,.133),.42,.37,20)
iron=flat("Graphite steel",(.052,.062,.059),.75,.38)
glass=flat("Night glazing",(.07,.105,.104),.48,.13)
bm=bmesh.new()
bm.from_mesh(wall.data)
bm.faces.ensure_lookup_table()
bmesh.ops.delete(bm,geom=[f for f in bm.faces if wall.material_slots[f.material_index].name=="stain"],context="FACES")
bmesh.ops.remove_doubles(bm,verts=bm.verts[:],dist=.00001)
bm.to_mesh(wall.data)
bm.free()
# Project the repeating weathered surfaces in metre space to preserve texel density.
uv=wall.data.uv_layers.active
for face in wall.data.polygons:
    axis=max(range(3),key=lambda n:abs(face.normal[n]))
    u,v=((1,2),(0,2),(0,1))[axis]
    for i in face.loop_indices:
        pt=wall.data.vertices[wall.data.loops[i].vertex_index].co
        uv.data[i].uv=(pt[u]/1.4,pt[v]/1.4)
for slot in wall.material_slots:
    slot.material={"concrete":stone,"door":steel,"glass":glass}.get(slot.name,iron)
bevel(wall,.007)
lamp=obj("StairwellCageLamp.obj",1)
lamp.scale=(.48,)*3
lamp.location=(.03,-.59,2.12)
warm=flat("Amber diffuser",(1,.59,.25),0,.28,3)
for slot in lamp.material_slots:
    slot.material=warm if slot.name in ("glass","filament") else iron
bpy.ops.import_scene.gltf(filepath=str(INPUT / "Entrance.glb"))
sign=[o for o in bpy.context.selected_objects if o.type=="MESH"]
for part in sign:
    matrix=part.matrix_world.copy()
    part.parent=None
    part.matrix_world=Matrix.Translation((1.82,-.14,2.35)) @ matrix
# Mount the original pieces with visible fasteners, door pulls and wire-glass mullions.
mounts = []
def trim(name, xyz, dimensions, material, radius=.002):
    bpy.ops.mesh.primitive_cube_add(size=1, location=xyz)
    item=bpy.context.object
    item.name=name
    item.dimensions=dimensions
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    item.data.materials.append(material)
    bevel(item,radius)
    mounts.append(item)
    return item

nickel=flat("Brushed nickel",(.30,.29,.255),.89,.31)
rubber=flat("Glazing gasket",(.014,.017,.015),.02,.65)
inside=flat("Frosted stairwell light",(.70,.47,.23),.08,.46,.32)
for side in (-1,1):
    x=side*.73
    trim("Rubber glazing bead",(x,-.331,2.30),(1.218,.016,.827),rubber)
    trim("Stairwell pane",(x,-.344,2.30),(1.173,.012,.785),inside if side<0 else glass)
    for i in range(1,27):
        trim("Glazing wire",(x-.587+i*.044,-.353,2.30),(.0018,.001,.785),iron,.0002)
    for i in range(1,18):
        trim("Glazing wire",(x,-.353,1.907+i*.044),(1.174,.001,.0018),iron,.0002)
    pull_x=side*.21
    for z in (.99,1.26):
        trim("Handle mounting shoe",(pull_x,-.285,z),(.078,.063,.065),nickel)
        trim("Handle standoff",(pull_x,-.343,z),(.038,.095,.035),nickel)
    trim("Door pull",(pull_x,-.391,1.125),(.040,.038,.306),nickel,.014)
    trim("Door kick plate",(x,-.268,.247),(1.28,.010,.24),iron)
    for px in (x-.60,x+.60):
        for z in (.15,.34):
            trim("Rivet",(px,-.276,z),(.013,.007,.013),nickel)
# Keep the keypad face proud of the original intercom ribs.
trim("Entryphone brushed face",(1.89,-.453,1.38),(.207,.020,.49),iron)
trim("Display",(1.89,-.467,1.54),(.164,.007,.068),rubber)
for i in range(12):
    trim("Intercom key",(1.831+(i%3)*.058,-.476,1.444-(i//3)*.055),(.042,.021,.036),nickel)
bpy.ops.object.select_all(action='DESELECT')
for item in [wall,lamp,*sign,*mounts]:item.select_set(True)
bpy.context.view_layer.objects.active=wall
bpy.ops.object.join()
wall.data.name="SM_EntranceNine"
glb=ROOT/"SourceArt/Integration/EntranceNine.glb"
glb.parent.mkdir(parents=True,exist_ok=True)
bpy.ops.export_scene.gltf(filepath=str(glb),use_selection=True,export_format='GLB',export_yup=True)
def render_preview():
    # Render the shipped interchange file after a clean reimport.
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(glb))
    meshes = [o for o in bpy.context.scene.objects if o.type == "MESH"]
    if len(meshes) != 1 or (meshes[0].dimensions - Vector((5.2, 2.58, 4.71))).length > .02:
        raise RuntimeError("Reimported courtyard assembly has unexpected bounds")
    if len(meshes[0].material_slots) < 8 or not meshes[0].data.uv_layers:
        raise RuntimeError("Reimport lost UVs or materials")
    scene=bpy.context.scene
    bpy.ops.mesh.primitive_plane_add(size=80,location=(0,0,-.515))
    bpy.context.object.data.materials.append(flat("Wet pavement",(.035,.039,.045),.22,.27))
    area("Entry lamp",(.03,-.83,3.10),(0,-.3,1.1),92,(1,.56,.27),.35)
    area("Overcast sky",(-3,-4,5),(0,0,1.6),410,(.46,.61,.85),5)
    area("Reflected courtyard light",(4,-3,2.4),(1.2,0,1.5),132,(.94,.81,.62),2.6)
    scene.world=bpy.data.worlds.new("Evening")
    scene.world.color=(.075,.10,.15)
    bpy.ops.object.camera_add(location=(1.65,-5.64,1.63))
    cam=bpy.context.object
    cam.rotation_euler=(Vector((.25,-.23,1.62))-cam.location).to_track_quat('-Z','Y').to_euler()
    cam.data.lens=38
    scene.camera=cam
    scene.render.engine='CYCLES'
    scene.cycles.samples=48
    scene.cycles.use_denoising=False
    scene.render.resolution_x=1440
    scene.render.resolution_y=900
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format='JPEG'
    scene.render.image_settings.quality=92
    scene.render.filepath=str(ROOT/"Docs/Frames/Visual/EntranceNine.jpg")
    bpy.ops.render.render(write_still=True)

if "--mesh-only" not in sys.argv:
    render_preview()
