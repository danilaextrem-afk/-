"""Reimport shipped GLBs, inspect geometry, render two camera/light setups."""
import bpy, json, math, struct
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parent

def aim(o,at):o.rotation_euler=(at-o.location).to_track_quat('-Z','Y').to_euler()
for name in ['Gauge','Entrance']:
    folder=ROOT/name
    bpy.ops.wm.read_factory_settings(use_empty=True)
    path=folder/(name+'.glb')
    raw=path.read_bytes()
    j=json.loads(raw[20:20+struct.unpack_from('<I',raw,12)[0]])
    assert all('bufferView' in im for im in j['images'])
    for mesh in j['meshes']:
        for p in mesh['primitives']:
            assert {'NORMAL','TEXCOORD_0','TANGENT'}<=p['attributes'].keys()
    bpy.ops.import_scene.gltf(filepath=str(path),bone_heuristic='BLENDER',disable_bone_shape=True)
    scene=bpy.context.scene; scene.frame_set(14)
    meshes=[o for o in scene.objects if o.type=='MESH']
    points=[o.matrix_world@v.co for o in meshes for v in o.data.vertices]
    lo=Vector([min(p[i] for p in points) for i in range(3)])
    hi=Vector([max(p[i] for p in points) for i in range(3)])
    at=(lo+hi)/2
    span=max(hi-lo)
    assert abs(span-(.113 if name=='Gauge' else .4))<.002, span
    degenerate=sum(p.area<1e-14 for o in meshes for p in o.data.polygons)
    assert not degenerate,degenerate
    scene.render.engine='CYCLES';scene.cycles.samples=40; scene.cycles.use_denoising=False
    scene.world=bpy.data.worlds.new('Review ambient');scene.world.use_nodes=True
    scene.render.resolution_x=800;scene.render.resolution_y=680;scene.render.resolution_percentage=100
    bpy.ops.object.camera_add()
    cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=span*(1.38 if name=='Gauge' else 1.15)
    cam.data.clip_start=.001
    def light(label,off,energy,color,size):
        bpy.ops.object.light_add(type='AREA',location=at+Vector(off)*span)
        ob=bpy.context.object;ob.name=label;ob.data.energy=energy*span**2;ob.data.color=color;ob.data.shape='DISK';ob.data.size=size*span
        aim(ob,at);return ob
    key=light('Window',(-1,-1.6,1.5),125,(.93,.97,1),1.3)
    side=light('Edge',(1.5,-.4,.4),80,(1,.85,.69),.8)
    for mode in ['Neutral','Raking']:
        neutral=mode=='Neutral'
        scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.19,.21,.23,1) if neutral else (.047,.059,.075,1)
        cam.location=at+Vector((.33,-1.95,.24) if neutral else (.92,-1.8,.33))*span;aim(cam,at)
        if not neutral:
            key.location=at+Vector((-1.3,-.5,.65))*span;aim(key,at);key.data.color=(1,.73,.44)
            side.data.color=(.54,.72,1)
        scene.render.filepath=str(folder/(mode+'.jpg'));scene.render.image_settings.file_format='JPEG'
        bpy.ops.render.render(write_still=True)
    # A separate simple hull prevents small fittings from catching the player.
    # Export OBJ with centimetre coordinates in the authored Z-up axes.
    hull=[(x,y,z) for z in [lo.z,hi.z] for y in [lo.y,hi.y] for x in [lo.x,hi.x]]
    lines=['# unit: cm; Z up', 'o UCX_'+name+'_00']
    lines+=['v '+' '.join(f'{100*c:.6f}' for c in v) for v in hull]
    lines+=['f '+f for f in ['1 3 4 2','5 6 8 7','1 2 6 5','3 7 8 4','1 5 7 3','2 4 8 6']]
    (folder/'Collision.obj').write_text('\n'.join(lines)+'\n')
    if name=='Gauge':
        assert len(j['skins'])==1 and len(j['animations'])==1
        rig=next(o for o in scene.objects if o.type=='ARMATURE')
        poses=[]
        for frame in (1,21,41):
            scene.frame_set(frame)
            poses.append(rig.pose.bones['Spindle'].matrix.to_quaternion())
        assert all(abs(a.dot(b))<.9 for a,b in zip(poses,poses[1:]))
    report=dict(asset=name,epoch=2026,version=1,source='build.py + textures.py',size_xyz_cm=[round(x*100,2) for x in hi-lo],triangles=sum(len(o.data.polygons) for o in meshes),degenerate_triangles=degenerate,mesh_nodes=len(meshes),animations=[a['name'] for a in j.get('animations',[])],embedded_images=len(j['images']),render='Blender Cycles CPU, reimported GLB',ue5='pending',collision='Collision.obj, separate convex box (cm); UE5 association pending',lod='base mesh only')
    (folder/'review.json').write_text(json.dumps(report,indent=2)+'\n')
