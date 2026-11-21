"""blender -b -P build.py. Scene dimensions are metres; glTF uses metres."""
import bpy, math, json, bmesh
from mathutils import Vector
from math import sin,cos,pi
from pathlib import Path
ROOT=Path(__file__).resolve().parent

def mat(name,color,metal,rough):
    m=bpy.data.materials.new(name)
    m.use_nodes=True
    p=m.node_tree.nodes.get('Principled BSDF')
    for key,val in [('Base Color',(*color,1)),('Metallic',metal),('Roughness',rough)]:
        p.inputs[key].default_value=val
    return m

def artwork(name):
    m=mat(name,(1,1,1),0,.5)
    tree=m.node_tree
    p=tree.nodes.get('Principled BSDF')
    def tex(file,data=False):
        t=tree.nodes.new('ShaderNodeTexImage')
        t.image=bpy.data.images.load(str(folder/'Maps'/(file+'.png')))
        if data:t.image.colorspace_settings.name='Non-Color'
        return t.outputs['Color']
    tree.links.new(tex('Color'),p.inputs['Base Color'])
    sep=tree.nodes.new('ShaderNodeSeparateColor')
    tree.links.new(tex('MetalRough',True),sep.inputs[0])
    for channel,key in [('Green','Roughness'),('Blue','Metallic')]:
        tree.links.new(sep.outputs[channel],p.inputs[key])
    n=tree.nodes.new('ShaderNodeNormalMap')
    tree.links.new(tex('Normal',True),n.inputs['Color'])
    tree.links.new(n.outputs[0],p.inputs['Normal'])
    return m

def mesh(name,v,f,m,uvsize=None):
    me=bpy.data.meshes.new(name); me.from_pydata(v,[],f); me.update()
    ob=bpy.data.objects.new(name,me); bpy.context.collection.objects.link(ob)
    ob.data.materials.append(m)
    # Planar mapping on caps, local face mapping on the narrow trim.
    uv=me.uv_layers.new()
    for face in me.polygons:
        axis=max(range(3),key=lambda i:abs(face.normal[i]))
        a,b=[(1,2),(0,2),(0,1)][axis]
        for loop in face.loop_indices:
            co=me.vertices[me.loops[loop].vertex_index].co
            sx,sy=uvsize if uvsize and axis==2 else (.1,.1)
            uv.data[loop].uv=(.5+co[a]/sx,.5+co[b]/sy)
    return ob

def shell(name,rings,m,cap=True,uvsize=None):
    n=len(rings[0])
    faces=[(i*n+j,i*n+(j+1)%n,(i+1)*n+(j+1)%n,(i+1)*n+j) for i in range(len(rings)-1) for j in range(n)]
    if cap:faces.extend([tuple(reversed(range(n))),tuple(range((len(rings)-1)*n,len(rings)*n))])
    return mesh(name,sum(rings,[]),faces,m,uvsize)

def turned(name,profile,m,n=96,uvsize=None):
    rings=[[(r*cos(2*pi*j/n),r*sin(2*pi*j/n),z) for j in range(n)] for r,z in profile]
    ob=shell(name,rings,m,uvsize=uvsize)
    for p in list(ob.data.polygons)[:-2]:p.use_smooth=True
    return ob

def screw(x,y,z,r,angle):
    o=turned('Recessed_slotted_fastener',[(r*.83,0),(r,.1*r),(r*.93,.27*r),(r*.76,.38*r)],steel,32)
    o.location=(x,y,z)
    bpy.ops.mesh.primitive_cube_add(size=1,location=(x,y,z+.42*r))
    cutter=bpy.context.object
    cutter.dimensions=(2.3*r,.23*r,.52*r)
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    cutter.rotation_euler.z=angle
    mod=o.modifiers.new('Milled slot','BOOLEAN'); mod.object=cutter
    bpy.context.view_layer.objects.active=o; bpy.ops.object.modifier_apply(modifier=mod.name)
    bpy.data.objects.remove(cutter,do_unlink=True)

def outline(w,h,r,z):
    pts=[]
    for j,(cx,cy) in enumerate([(w/2-r,h/2-r),(-w/2+r,h/2-r),(-w/2+r,-h/2+r),(w/2-r,-h/2+r)]):
        for k in range(13):
            a=(j+k/12)*pi/2
            pts.append((cx+r*cos(a),cy+r*sin(a),z))
    return pts

def gauge():
    turned('Drawn_steel_cup',[(.036,-.031),(.038,-.030),(.040,-.026),(.040,-.006),(.042,-.005),(.042,-.002),(.038,-.002)],case)
    rim=[(.038,-.004),(.041,-.004),(.042,-.002),(.042,.002),(.041,.004),(.039,.0048),(.0376,.0039),(.0372,.002),(.0372,-.001)]
    # Annular rolled bezel with open aperture and closed cross section.
    rings=[[(r*cos(j*pi/64),r*sin(j*pi/64),z) for j in range(128)] for r,z in rim]
    ring=shell('Rolled_bezel',rings,steel,False)
    bm=bmesh.new(); bm.from_mesh(ring.data); bm.verts.ensure_lookup_table()
    for i in range(128):bm.faces.new([bm.verts[(len(rim)-1)*128+i],bm.verts[(len(rim)-1)*128+(i+1)%128],bm.verts[(i+1)%128],bm.verts[i]])
    bm.to_mesh(ring.data); bm.free()
    for p in ring.data.polygons:p.use_smooth=True
    dial=turned('Printed_dial',[(.0377,-.0015),(.0377,-.0012)],face,128,(.0754,.0754))
    pts=[(-.0016,-.008),(.0016,-.008),(.0021,.002),(.00045,.031),(0,.033),(-.00045,.031),(-.0021,.002)]
    needle=shell('Pointer_bar', [[(x,y,z) for x,y in pts] for z in [.0001,.00055]],case)
    arm=bpy.data.armatures.new('Gauge_movement')
    rig=bpy.data.objects.new('Gauge_movement',arm)
    bpy.context.collection.objects.link(rig)
    bpy.context.view_layer.objects.active=rig
    bpy.ops.object.mode_set(mode='EDIT')
    bone=arm.edit_bones.new('Spindle')
    bone.head=(0,0,0); bone.tail=(0,.012,0)
    bpy.ops.object.mode_set(mode='OBJECT')
    group=needle.vertex_groups.new(name='Spindle')
    group.add(list(range(len(needle.data.vertices))),1,'REPLACE')
    needle.parent=rig
    skin=needle.modifiers.new('Rigid needle bind','ARMATURE'); skin.object=rig
    pose=rig.pose.bones['Spindle']; pose.rotation_mode='XYZ'
    for frame,angle in [(1,135),(21,0),(41,-135)]:
        pose.rotation_euler.z=math.radians(angle)
        pose.keyframe_insert(data_path='rotation_euler',frame=frame)
    bpy.context.scene.frame_set(14)
    turned('Spindle_cap',[(.0022,.0006),(.0024,.0009),(.0019,.0015)],brass,32)
    for x in [-.016,.016]:screw(x,-.013,-.0011,.00115,1.7+x*49)
    # Union nut, seat and a continuous helical ridge on the stem.
    for name,p,n in [('Socket',[(.006,0),(.007,.002),(.007,.011),(.006,.012)],48),('Hex_union',[(.007,.010),(.010,.012),(.010,.021),(.008,.022)],6),('Stem',[(.006,.021),(.006,.034),(.0048,.035)],48)]:
        ob=turned(name,p,brass,n); ob.rotation_euler.x=pi/2; ob.location=(0,-.036,-.016)
        if n==6:
            for poly in ob.data.polygons:poly.use_smooth=False
    rings=[]
    for i in range(6*56+1):
        a=i*2*pi/56
        rings.append([((.006+dr)*cos(a),(.006+dr)*sin(a),.0225+i*.0018/56+dz) for dr,dz in [(0,-.0006),(.0008,-.00012),(.0008,.00012),(0,.0006)]])
    thread=shell('Helical_thread',rings,brass);thread.rotation_euler.x=pi/2;thread.location=(0,-.036,-.016)
    glass=mat('Clear_watch_glass',(.94,.98,1),0,.075)
    shader=glass.node_tree.nodes.get('Principled BSDF')
    shader.inputs['Alpha'].default_value=.018
    glass.surface_render_method='BLENDED'
    shader.inputs['IOR'].default_value=1.49
    turned('Lens',[(.0376,.0022),(.0378,.0025),(.0375,.0030),(.031,.00325),(.017,.00345)],glass,96)

def entrance():
    profile=[(.390,.190,.015,0),(.398,.198,.019,.001),(.400,.200,.020,.0025),(.398,.198,.019,.0035),(.388,.188,.017,.0035),(.382,.182,.015,.006),(.374,.174,.012,.0068)]
    ob=shell('Pressed_enamel_steel', [outline(*p) for p in profile],case,uvsize=(.400,.200))
    ob.data.materials.append(face)
    for p in ob.data.polygons:
        if p.normal.z>0 or p.index<len(ob.data.polygons)-2:p.material_index=1
    for x in [-.178,.178]:
        for y in [-.070,.070]:screw(x,y,.0068,.004,1+x*11+y*6)

for name,fn in [('Gauge',gauge),('Entrance',entrance)]:
    bpy.ops.wm.read_factory_settings(use_empty=True)
    folder=ROOT/name
    steel=mat('Satin_nickel',(.48,.47,.43),.94,.29)
    brass=mat('Machined_brass',(.46,.29,.10),.87,.32)
    case=mat('Graphite_stove_enamel',(.023,.031,.032),.18,.31)
    face=artwork(name+'_face')
    fn()
    objs=list(bpy.context.scene.objects)
    root=bpy.data.objects.new(name+'_Mount',None);bpy.context.collection.objects.link(root)
    for ob in objs:
        if ob.parent is None: ob.parent=root
        if ob.type!='MESH':continue
        tri=ob.modifiers.new("Export triangles","TRIANGULATE")
        bpy.context.view_layer.objects.active=ob
        bpy.ops.object.modifier_move_to_index(modifier=tri.name,index=0)
        bpy.ops.object.modifier_apply(modifier=tri.name)
    root.rotation_euler.x=pi/2
    bpy.context.view_layer.update()
    bpy.ops.export_scene.gltf(filepath=str(folder/(name+'.glb')),export_image_format='AUTO',export_tangents=True)
