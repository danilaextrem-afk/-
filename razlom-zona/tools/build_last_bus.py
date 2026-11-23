"""RZ_VEH_LAST_BUS_01. Generate an abandoned rural route bus in metres, Y up."""
import json
import math
from pathlib import Path
import struct

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy.ndimage import gaussian_filter

OUT = Path(__file__).resolve().parents[1] / "assets/transport"
rng = np.random.default_rng(718)
N = 128
atlas = Image.new("RGB", (4*N, 4*N))
colors = [(201,183,134),(69,116,109),(50,47,45),(112,109,97),
          (33,35,36),(125,75,50),(58,75,75),(136,116,77),
          (152,96,43),(112,125,113),(217,155,75),(225,187,101),
          (68,83,77),(129,150,133),(63,60,50),(91,94,80)]
Y,X = np.mgrid[:N,:N]
for slot,color in enumerate(colors):
    noise=rng.normal(0,1,(N,N))
    stain=gaussian_filter(noise,4)
    streak=gaussian_filter(noise,(13,1))*25
    rgb=np.array(color)+np.repeat((noise*3+stain*60+streak)[:,:,None],3,axis=2)
    if slot in [0,1,3,9]:
        rust=np.clip(3.7*(.32*Y/N+.9*stain+.11*noise-.38),0,1)
        rgb=rgb*(1-rust[:,:,None])+np.array([92,59,40])*rust[:,:,None]
    if slot==6:
        rgb+=((1-Y/N)*24+streak)[:,:,None]
    if slot==4:
        rgb+=np.where(((X+(Y//5)*2)%12)<2,-13,0)[:,:,None]
    tile=Image.fromarray(np.uint8(np.clip(rgb,0,255)))
    atlas.paste(tile,((slot%4)*N,(slot//4)*N))
# A dedicated 512x128 band gives the destination board real letter resolution.
panel=Image.new('RGB',(512,128),'#252e2c');d=ImageDraw.Draw(panel)
d.rounded_rectangle((5,6,506,121),radius=7,outline='#aaa083',width=3)
font=ImageFont.truetype('DejaVuSansCondensed-Bold.ttf',58)
d.text((256,64),'07  ЗАЛЕСЬЕ',font=font,fill='#e7d7ae',anchor='mm')
atlas.paste(panel,(0,384));atlas.save(OUT/'bus_atlas.png')

vertices=[];normals=[];uvs=[];batches={'body':[],'lamps':[]}
def face(points,slot,batch='body'):
    p=np.asarray(points,float);normal=np.cross(p[1]-p[0],p[2]-p[0])
    length=np.linalg.norm(normal)
    if length<1e-8:raise ValueError(points)
    base=len(vertices); mapping=[(0,1),(1,1),(1,0),(0,0)]
    for pt,(u,v) in zip(p,mapping):
        vertices.append(pt);normals.append(normal/length)
        if slot=='sign':uvs.append(((2+508*u)/512,(386+124*v)/512))
        else:uvs.append(((slot%4*N+1+(N-2)*u)/512,(slot//4*N+1+(N-2)*v)/512))
    for i in range(1,len(p)-1):batches[batch].extend([base,base+i,base+i+1])
def box(center,size,slot):
    x,y,z=center;a,b,c=np.asarray(size)/2
    p=np.array([[-a,-b,-c],[a,-b,-c],[a,b,-c],[-a,b,-c],[-a,-b,c],[a,-b,c],[a,b,c],[-a,b,c]])+[x,y,z]
    for order in [[3,2,1,0],[4,5,6,7],[0,4,7,3],[5,1,2,6],[7,6,2,3],[0,1,5,4]]:face(p[order],slot)
def tube(a,b,r1,r2,slot,sides=10,caps=True):
    a,b=np.array(a),np.array(b);n=b-a;n=n/np.linalg.norm(n)
    u=np.cross(n,[0,1,0] if abs(n[1])<.95 else [1,0,0]);u/=np.linalg.norm(u);v=np.cross(n,u)
    ring=[u*np.cos(k*math.tau/sides)+v*np.sin(k*math.tau/sides) for k in range(sides)]
    for i,r in enumerate(ring):
        q=ring[(i+1)%sides]
        face([a+r*r1,a+q*r1,b+q*r2,b+r*r2],slot)
        if caps:
            face([a,a+q*r1,a+r*r1],slot);face([b,b+r*r2,b+q*r2],slot)
def profile(y,w,front,back,corner):
    return [(-w+corner,y,front),(w-corner,y,front),(w,y,front-corner),(w,y,back+corner),(w-corner,y,back),(-w+corner,y,back),(-w,y,back+corner),(-w,y,front-corner)]
def belt(a,b,slot):
    for i in range(8):
        j=(i+1)%8;face([a[i],a[j],b[j],b[i]],slot)
# Raised lower sill over the wheels, with polygon-cut wheel arches.
for side in [-1,1]:
    seq=[(-2.98,.46)]
    for axle in [-1.88,1.76]:
        seq.extend([(axle-.54,.46)]+[(axle-.52*math.cos(k*math.pi/10),.55+.52*math.sin(k*math.pi/10)) for k in range(11)]+[(axle+.54,.46)])
    seq.append((3.06,.46))
    for (za,ha),(zb,hb) in zip(seq,seq[1:]):
        face([(side*1.12,ha,za),(side*1.12,hb,zb),(side*1.12,1.23,zb),(side*1.12,1.23,za)],1)
    for y,h,slot in [(1.25,.06,3),(1.42,.27,0),(1.595,.07,1)]:
        box((side*1.105,y,.03),(.05,h,6.04),slot)
    for axle in [-1.88,1.76]:
        def wheel(x):return (side*x,.52,axle)
        for a,b,r1,r2,t in [(.82,1.08,.49,.51,4),(1.08,1.15,.51,.46,4),(1.15,1.16,.34,.32,3),(1.16,1.19,.24,.22,1),(1.19,1.22,.13,.10,3)]:
            tube(wheel(a),wheel(b),r1,r2,t,18)
        for k in range(8):
            a=k*math.tau/8
            y=.52+.172*math.sin(a);z=axle+.172*math.cos(a)
            tube((side*1.19,y,z),(side*1.21,y,z),.026,.026,2,5)
# Rounded front apron and shoulder. Side panels remain open at the wheel wells.
for z,direction in [(3.06,1),(-2.98,-1)]:
    for row,(h0,h1,slot) in enumerate([(.46,1.19,1),(1.19,1.27,3),(1.27,1.63,0)]):
        for xa,xb,offa,offb in [(-1.12,-.90,0,.20),(-.90,.90,.20,.20),(.90,1.12,.20,0)]:
            face([(xa,h0,z+direction*offa),(xb,h0,z+direction*offb),(xb,h1,z+direction*offb),(xa,h1,z+direction*offa)],slot)
# Continuous rolled roof with an arched crown and an inset roof hatch.
last=profile(2.47,1.095,3.11,-3.00,.22)
for h,w,f,b,t in [(2.60,1.12,3.14,-3.04,0),(2.69,1.08,3.07,-2.99,0),(2.79,.94,2.91,-2.86,0),(2.84,.65,2.62,-2.59,9)]:
    ring=profile(h,w,f,b,.24);belt(last,ring,t);last=ring
face([last[0],last[1],last[4],last[5]],9)
for z in [-1.38,.74]:
    box((0,2.88,z),(.80,.10,.61),3)
    for k in range(7):box((0,2.942,z-.21+k*.07),(.59,.025,.027),2)
# Panoramic split windshield with a missing pane; steering and handrails visible.
for side in [-1,1]:
    def ws(x,y):return (side*x,y,3.27-(y-1.58)*.18)
    if side<0:face([ws(.045,1.63),ws(.91,1.63),ws(.85,2.42),ws(.045,2.42)],6)
    for xx in [.035,.93]:tube(ws(xx,1.59),ws(xx,2.47),.032,.032,2,6)
    tube(ws(.04,2.44),ws(.86,2.44),.032,.032,2,6)
    tube(ws(.04,1.60),ws(.92,1.60),.037,.037,3,6)
    tube(ws(.48,1.62),ws(.62,1.82),.012,.012,2,5)
    tube(ws(.45,1.76),ws(.75,1.88),.014,.014,2,5)
    face([ws(.93,1.62),(side*1.10,1.64,2.86),(side*1.08,2.46,2.79),ws(.86,2.45)],6)
box((0,1.47,2.79),(2.06,.18,.50),2)
tube((-.53,1.44,2.59),(-.53,1.76,2.62),.037,.037,3)
for k in range(12):
    a=k*math.tau/12;b=(k+1)*math.tau/12
    tube((-.53+.24*math.cos(a),1.78+.08*math.sin(a),2.63+.22*math.sin(a)),(-.53+.24*math.cos(b),1.78+.08*math.sin(b),2.63+.22*math.sin(b)),.018,.018,2,5,False)
for side in [-1,1]:
    for i in range(6):
        a=-2.81+i*.92;b=a+.92
        # Broken glazing alternates with intact windows and narrow sliding vents.
        if (side+i)%4!=0:
            face([(side*1.10,1.65,a+.045),(side*1.10,1.65,b-.045),(side*1.08,2.43,b-.045),(side*1.08,2.43,a+.045)],6)
        box((side*1.10,2.04,a),(.074,.87,.068),0)
        tube((side*1.108,2.27,a),(side*1.108,2.27,b),.019,.019,3,5)
        if i in [1,3,4]:
            box((side*.55,1.03,a+.32),(.74,.15,.48),5)
            box((side*.55,1.30,a+.07),(.74,.53,.10),5)
        if i in [1,4]:tube((side*.77,.75,a),(side*.77,2.43,a),.021,.021,7,6)
    box((side*1.10,2.03,2.73),(.07,.87,.12),0)
    tube((side*.99,2.38,2.91),(side*1.32,2.42,3.04),.021,.021,3,6)
    box((side*1.32,2.31,3.04),(.13,.26,.085),2)
    box((side*1.32,2.31,3.09),(.11,.23,.02),9)
box((0,.76,0),(2.17,.12,6.0),2)
box((0,1.01,3.285),(1.05,.38,.055),2)
for i in range(7):box((0,.86+i*.051,3.329),(1.01,.018,.028),3)
for side in [-1,1]:
    for x,r in [(.74,.138),(.97,.068)]:
        pos=lambda z:(side*x,1.075,z)
        tube(pos(3.195),pos(3.305),r+.034,r+.011,3,14)
        tube(pos(3.305),pos(3.323),r,r*.92,10 if x>.9 else 11,14)
    box((side*.99,.53,3.16),(.15,.19,.20),2)
box((0,.58,3.29),(2.05,.14,.12),3)
face([(-.89,2.48,3.19),(.89,2.48,3.19),(.89,2.71,3.18),(-.89,2.71,3.18)],'sign')
for z in [-2.98,-2.80]:box((0,1.99,z),(1.9,.76,.035),6)

def export(name):
    binary=bytearray();views=[];acc=[]
    def add(values,fmt,kind):
        a=np.asarray(values,dtype=fmt)
        while len(binary)%4:binary.append(0)
        views.append(dict(buffer=0,byteOffset=len(binary),byteLength=a.nbytes));binary.extend(a.tobytes())
        desc=dict(bufferView=len(views)-1,componentType=5126 if fmt=='<f4' else 5123,count=len(a),type=kind)
        if kind=='VEC3':desc.update(min=a.min(axis=0).tolist(),max=a.max(axis=0).tolist())
        acc.append(desc);return len(acc)-1
    attrs={k:add(a,'<f4',t) for k,a,t in [('POSITION',vertices,'VEC3'),('NORMAL',normals,'VEC3'),('TEXCOORD_0',uvs,'VEC2')]}
    prim=dict(attributes=attrs,indices=add(batches['body'],'<u2','SCALAR'),material=0)
    doc=dict(asset={'version':'2.0'},scene=0,scenes=[{'nodes':[0]}],nodes=[dict(name=name,mesh=0)],meshes=[dict(primitives=[prim])],materials=[dict(name='Weathered enamel',doubleSided=True,pbrMetallicRoughness=dict(baseColorTexture=dict(index=0),metallicFactor=.12,roughnessFactor=.68))],images=[dict(uri='bus_atlas.png')],textures=[dict(source=0,sampler=0)],samplers=[dict(magFilter=9728,minFilter=9984)],buffers=[dict(byteLength=len(binary))],bufferViews=views,accessors=acc)
    js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*(-len(js)%4);binary+=b'\0'*(-len(binary)%4)
    (OUT/(name+'.glb')).write_bytes(struct.pack('<4sII',b'glTF',2,28+len(js)+len(binary))+struct.pack('<I4s',len(js),b'JSON')+js+struct.pack('<I4s',len(binary),b'BIN\0')+binary)
    print(name, 'triangles',len(batches['body'])//3,'size',np.ptp(vertices,axis=0))

export('last_bus')
vertices.clear(); normals.clear(); uvs.clear(); batches['body'].clear()
# Field receiver with a slotted speaker, tuner housing, handles and whip antenna.
box((0,0,0),(.57,.36,.22),1)
box((0,.0,.118),(.51,.29,.026),2)
for i in range(9):
    box((-.115,-.095+i*.024,.141),(.205,.010,.019),3)
box((.117,.063,.142),(.189,.058,.018),11)
for i in range(8):box((.038+i*.022,.057,.155),(.006,.018,.006),2)
for x in [.062,.171]:
    tube((x,-.068,.138),(x,-.068,.177),.041,.039,4,10)
    box((x,-.050,.181),(.011,.022,.009),3)
for x in [-.176,.176]:tube((x,.18,0),(x,.247,0),.016,.016,2,8)
tube((-.176,.247,0),(.176,.247,0),.018,.018,2,8)
tube((.237,.17,-.015),(.282,.519,-.015),.012,.006,3,8)
export('field_receiver')
# First-person field torch. Wrist, folded canvas cuff and individually wrapped fingers.
vertices.clear(); normals.clear(); uvs.clear(); batches['body'].clear()
tube((.16,-.18,.32),(.013,-.062,.087),.092,.059,1,8)
tube((.023,-.066,.103),(.008,-.057,.068),.068,.064,2,8)
tube((.01,-.056,.076),(-.004,-.044,-.025),.053,.044,5,8)
for i in range(4):
    z=.051-i*.025
    tube((.026,-.052,z),(.048,-.023,z-.004),.014,.013,2,6)
    tube((.048,-.023,z-.004),(.031,-.006,z-.009),.013,.009,2,6)
tube((-.031,-.06,.032),(-.042,-.018,-.026),.020,.012,2,7)
for z1,z2,r1,r2,slot in [(.09,.075,.041,.041,3),(.075,-.062,.038,.038,4),(-.062,-.103,.04,.062,1),(-.103,-.156,.062,.061,1),(-.156,-.169,.064,.064,3),(-.169,-.171,.054,.054,9)]:
    tube((0,0,z1),(0,0,z2),r1,r2,slot,16)
for z in [-.112,-.125,-.138]:
    tube((0,0,z),(0,0,z-.004),.063,.063,2,16)
box((0,.039,-.04),(.023,.008,.038),8)
export('field_torch')

# Rigid segmented figure. The local geometry is authored around its joint pivots.
vertices.clear(); normals.clear(); uvs.clear(); batches['body'].clear()
segments=[]
def finish_segment(name,pivot):
    segments.append((name,pivot,batches['body'].copy()))
    batches['body'].clear()
def silhouette(rows,slot):
    rings=[]
    for height,width,depth,z in rows:
        rings.append([(width*math.cos(i*math.tau/10),height,z+depth*math.sin(i*math.tau/10)) for i in range(10)])
    for a,b in zip(rings,rings[1:]):
        for i in range(10):
            j=(i+1)%10
            face([a[i],b[i],b[j],a[j]],slot)
    for ring in [rings[0],rings[-1]]:
        center=np.mean(ring,axis=0)
        for i in range(10):face([center,ring[i],ring[(i+1)%10]],slot)
silhouette([(.70,.27,.19,.025),(.84,.25,.17,.02),(1.08,.16,.135,.02),(1.38,.245,.17,-.01),(1.54,.31,.18,-.052),(1.62,.15,.12,-.073)],1)
for side in [-1,1]:
    tube((side*.21,1.51,-.19),(side*.115,1.02,-.12),.028,.024,5,6)
box((0,1.34,.195),(.29,.38,.19),5)
tube((.15,1.26,.26),(.18,1.72,.27),.055,.042,3,9)
for k in range(4):
    box((-.17+k*.11,.98,-.145),(.093,.14,.075),2)
silhouette([(1.48,.16,.14,-.10),(1.62,.208,.19,-.125),(1.78,.182,.17,-.15),(1.94,.11,.13,-.12),(1.98,.046,.075,-.095)],2)
box((0,1.74,-.298),(.205,.074,.026),4)
box((-.038,1.75,-.315),(.079,.014,.008),11)
tube((.045,1.65,-.27),(.068,1.60,-.36),.067,.050,3,10)
finish_segment('Coat',(0,0,0))
for sign,label in [(-1,'L'),(1,'R')]:
    tube((0,0,0),(sign*.029,-.355,.046),.111,.076,2,8)
    tube((sign*.029,-.355,.046),(sign*.028,-.68,-.010),.077,.056,1,8)
    box((sign*.028,-.72,-.072),(.145,.135,.263),4)
    finish_segment('Leg'+label,(sign*.118,.79,0))
    tube((0,0,0),(sign*.065,-.30,.029),.111,.075,1,8)
    tube((sign*.065,-.30,.029),(sign*.054,-.59,-.070),.08,.057,2,8)
    tube((sign*.054,-.59,-.070),(sign*.048,-.69,-.106),.059,.041,5,7)
    for finger in range(3):
        dx=sign*.049+(finger-1)*.024
        tube((dx,-.68,-.104),(dx,-.749+abs(finger-1)*.014,-.128),.013,.009,2,6)
    finish_segment('Arm'+label,(sign*.255,1.51,-.045))

def export_watchman():
    data=bytearray();views=[];accessors=[]
    def block(values,fmt,kind):
        a=np.asarray(values,dtype=fmt)
        while len(data)%4:data.append(0)
        views.append(dict(buffer=0,byteOffset=len(data),byteLength=a.nbytes));data.extend(a.tobytes())
        acc=dict(bufferView=len(views)-1,componentType=5126 if fmt=='<f4' else 5123,count=len(a),type=kind)
        if kind=='VEC3':acc.update(min=a.min(axis=0).tolist(),max=a.max(axis=0).tolist())
        accessors.append(acc);return len(accessors)-1
    meshes=[];nodes=[dict(name='Watchman',children=list(range(1,len(segments)+1)))]
    for name,pivot,indices in segments:
        lo=min(indices);hi=max(indices)+1
        attrs=dict(POSITION=block(vertices[lo:hi],'<f4','VEC3'),NORMAL=block(normals[lo:hi],'<f4','VEC3'),TEXCOORD_0=block(uvs[lo:hi],'<f4','VEC2'))
        meshes.append(dict(primitives=[dict(attributes=attrs,indices=block(np.asarray(indices)-lo,'<u2','SCALAR'),material=0)]))
        nodes.append(dict(name=name,translation=pivot,mesh=len(meshes)-1))
    doc=dict(asset={'version':'2.0'},scene=0,scenes=[{'nodes':[0]}],nodes=nodes,meshes=meshes,materials=[dict(doubleSided=True,pbrMetallicRoughness=dict(baseColorTexture=dict(index=0),metallicFactor=.03,roughnessFactor=.9))],images=[dict(uri='bus_atlas.png')],textures=[dict(source=0,sampler=0)],samplers=[dict(magFilter=9728,minFilter=9984)],buffers=[dict(byteLength=len(data))],bufferViews=views,accessors=accessors)
    js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*(-len(js)%4);data+=b'\0'*(-len(data)%4)
    (OUT/'watchman.glb').write_bytes(struct.pack('<4sII',b'glTF',2,28+len(js)+len(data))+struct.pack('<I4s',len(js),b'JSON')+js+struct.pack('<I4s',len(data),b'BIN\0')+data)
    print('watchman triangles',sum(len(s[2]) for s in segments)//3)
export_watchman()
