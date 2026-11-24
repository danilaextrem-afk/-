"""Bake the settlement approach as one static draw surface per material."""
import io, json, math, random, struct
from collections import defaultdict
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import gaussian_filter

OUT=Path(__file__).resolve().parents[1]/'assets/village'
rng=random.Random(283)
noise=np.random.default_rng(283)
N=64
palette=[(110,91,70),(68,53,46),(104,114,100),(73,69,68),(103,65,45),(63,71,65),(43,50,52),(120,116,97),(76,71,57),(58,58,54),(99,75,53),(144,97,58),(56,67,50),(90,92,63),(207,130,59),(133,63,41)]
atlas=Image.new('RGBA',(4*N,4*N))
v,u=np.mgrid[:N,:N]
for t,color in enumerate(palette):
    jitter=noise.normal(0,8,(N,N))
    field=gaussian_filter(jitter,3)*9
    tex=jitter*.64+field
    if t in [0,1,2,10]:
        tex+=noise.normal(0,6,(N,1))+np.where(v%13==0,-27,0)+np.where(v%13==1,12,0)
    if t==3:
        tex+=np.where(u%8<2,19,-5)+np.where(v%31==0,-25,0)
    if t==7:
        tex+=np.where((v%11==0)|((u+(v//11)*12)%24==0),-24,0)
    a=np.uint8(np.clip(np.array(color)+tex[:,:,None],0,255))
    tile=Image.fromarray(a).convert('RGBA');d=ImageDraw.Draw(tile)
    if t in [2,4,5,15]:
        for _ in range(36):
            p,q=rng.randrange(N),rng.randrange(N);d.rectangle((p,q,p+rng.randrange(1,7),q+rng.randrange(1,4)),fill=rng.choice(['#463a32','#816049','#5b4939']))
    if t in [12,13]:
        tile=Image.new('RGBA',(N,N));d=ImageDraw.Draw(tile)
        if t==12:
            for k in range(58):
                yy=rng.randrange(5,62);span=int((yy+8)*.46)
                xx=32+rng.randint(-span,span)
                shade=rng.choice(['#323e33','#414b39','#505742','#5c6046','#676746'])
                d.line((32,max(0,yy-13),xx,yy),fill=shade,width=3)
                d.rectangle((xx-2,yy-2,xx+2,yy+3),fill=shade)
        else:
            for _ in range(37):
                xx=rng.randrange(N);yy=rng.randrange(8,56);ex=xx+rng.randint(-8,8)
                shade=rng.choice(['#9d8150','#877142','#716442','#5e623e','#bf985d'])
                d.line((xx,63,ex,yy),fill=shade,width=2)
                if yy<24:d.rectangle((ex-1,yy-3,ex+1,yy+4),fill=shade)
    atlas.paste(tile,(t%4*N,t//4*N))
# Source swatches are downsampled into one fixed-layout atlas for all street meshes.
from PIL import ImageEnhance, ImageFont
source=Image.open(Path(__file__).parent/'sources/zalesye_surfaces.jpg').convert('RGB')
tile_size=256
atlas=atlas.resize((1024,1024),Image.Resampling.NEAREST)
swatches=[source.crop((0,0,512,512)),source.crop((512,0,1024,512)),source.crop((0,512,512,1024))]
def put(slot,img):
    atlas.paste(img.resize((tile_size,tile_size),Image.Resampling.LANCZOS),(slot%4*tile_size,slot//4*tile_size))
put(2,swatches[0]);put(3,swatches[1]);put(7,swatches[2])
put(0,ImageEnhance.Color(swatches[0]).enhance(.28))
put(1,ImageEnhance.Brightness(ImageEnhance.Color(swatches[0]).enhance(.18)).enhance(.48))
# Weathered pale paint separates car body from the orange roadside timber.
metal=np.asarray(swatches[1].resize((256,256)),dtype=float)
wear=gaussian_filter(noise.normal(size=(256,256)),5)
chip=np.clip((wear-.047)*27,0,1)
paint=np.array([104,134,124])+noise.normal(0,3,(256,256,1))
put(15,Image.fromarray(np.uint8(np.clip(paint*(1-chip[:,:,None])+metal*chip[:,:,None],0,255))))
sign=Image.new('RGB',(840,112),'#234c52')
pen=ImageDraw.Draw(sign)
pen.rounded_rectangle((6,5,833,106),radius=12,outline='#c8ba94',width=4)
font=ImageFont.truetype('DejaVuSans-Bold.ttf',69)
pen.text((420,53),'П О Ч Т А',font=font,anchor='mm',fill='#e2dac0')
for i in range(620):
    x,y=rng.randrange(840),rng.randrange(112)
    pen.line((x,y,x+rng.randrange(1,5),y+1),fill=rng.choice(['#42514a','#604a39','#738079']))
put(11,sign)
# Leaf cluster with visible gaps rather than solid pyramid silhouettes.
fol=Image.new('RGBA',(256,256));pen=ImageDraw.Draw(fol)
for k in range(470):
    x,y=rng.gauss(128,47),rng.gauss(145,39)
    shade=rng.choice(['#4e5336','#606c3e','#797944','#8f8548','#aa9555'])
    pen.polygon([(x,y-4),(x+4,y-2),(x+2,y+6),(x-4,y+3)],fill=shade)
put(8,fol)
atlas.save(OUT/'street_atlas.png')


parts=defaultdict(lambda:[[],[],[],[]])
origin=np.zeros(3);rotation=np.eye(3)
def frame(x=0,z=0,angle=0):
    global origin,rotation
    c,s=math.cos(angle),math.sin(angle)
    rotation=np.array([[c,0,s],[0,1,0],[-s,0,c]])
    origin=np.array([x,0,z])
def face(points,t,group='street',uv=None):
    p=np.asarray(points)@rotation.T+origin
    n=np.cross(p[1]-p[0],p[2]-p[0]);length=np.linalg.norm(n)
    if length<1e-7:raise ValueError(points)
    pos,norm,tex,idx=parts[group]
    start=len(pos)
    if uv is None:uv=[(0,1),(1,1),(1,0),(0,0)][:len(p)]
    for pp,(a,b) in zip(p,uv):
        pos.append(pp);norm.append(n/length)
        tex.append(((t%4*N+1+a*(N-2))/(4*N),(t//4*N+1+b*(N-2))/(4*N)))
    idx.extend([start,start+1,start+2])
    if len(p)==4:idx.extend([start,start+2,start+3])
def cube(x,y,z,w,h,l,t,g='street'):
    a,b,c=w/2,h/2,l/2
    pts=np.array([[-a,-b,-c],[a,-b,-c],[a,b,-c],[-a,b,-c],[-a,-b,c],[a,-b,c],[a,b,c],[-a,b,c]])+[x,y,z]
    for order in [(3,2,1,0),(4,5,6,7),(0,4,7,3),(5,1,2,6),(7,6,2,3),(0,1,5,4)]:
        face(pts[list(order)],t,g)
def beam(p,q,w,t):
    p,q=np.array(p),np.array(q);axis=q-p;axis=axis/np.linalg.norm(axis)
    a=np.cross(axis,[0,1,0] if abs(axis[1])<.95 else [1,0,0]);a*=w/2/np.linalg.norm(a);b=np.cross(axis,a)
    ring=[a+b,-a+b,-a-b,a-b]
    for i,r in enumerate(ring):
        s=ring[(i+1)%4];face([p+r,p+s,q+s,q+r],t)

def repair_shed():
    frame(-8.0,-7.5,1.06)
    # Open repair bay, half-collapsed corrugated roof and a braced loft.
    cube(0,.12,0,6.6,.24,5.1,7)
    cube(0,1.62,-2.35,6.4,3.0,.16,2)
    cube(-3.08,1.5,0,.18,2.8,4.7,0)
    for x in [-3.08,.72,3.08]:
        beam((x,.22,2.14),(x,3.05,2.14),.21,1)
    for z in [-2.28,2.14]:
        beam((-3.2,3.05,z),(3.19,3.05,z),.24,1)
    for i in range(11):
        x=-3.4+i*.67
        ridge=4.55-.48*max(0,(x-.9)/2.5)
        beam((x,3.02,2.57),(x,ridge,-.38),.125,0)
        beam((x,ridge,-.38),(x,2.91,-2.60),.125,0)
        if i<10:
            face([(x,2.94,-2.7),(x+.67,2.94,-2.7),(x+.67,ridge,-.38),(x,ridge,-.38)],3)
        if i<7 or i==9:
            sag=-.27 if i==6 else 0
            face([(x,3.09,2.65),(x+.67,3.09+sag,2.65),(x+.67,ridge,-.38),(x,ridge,-.38)],3)
    for x in [-2.91,-2.62,-2.33,-2.04,-1.75]:
        cube(x,1.54,2.16,.27,2.6,.14,2)
    beam((-2.99,.52,2.27),(-1.68,2.52,2.27),.13,1)
    for x in [-1.05,1.55]:cube(x,.56,-1.54,.16,.94,.59,1)
    cube(.25,1.09,-1.54,3.03,.16,.71,0)
    for i in range(4):
        cube(2.28,.35+i*.46,-1.43,1.0,.085,.64,4)
    for x in [1.79,2.76]:beam((x,0,-1.16),(x,2.09,-1.16),.07,4)
    cube(-1.22,2.49,2.29,.19,.28,.18,14,'lamps')
    frame()

repair_shed()
# Leaning palisades with missing slats, posts and rails.
for side in [-1,1]:
    for a,b in [(-3,2),(-15,-10),(-26,-21)]:
        x=side*4.25
        for y in [.43,.93]:beam((x,y,a),(x,y,b),.095,1)
        for i in range(int((b-a)/.29)):
            if rng.random()<.13:continue
            z=a+i*.29;h=rng.uniform(.98,1.39)
            beam((x,0,z),(x+rng.uniform(-.08,.08),h,z+rng.uniform(-.12,.12)),.14,0)
# Weathered electricity poles, ceramic insulators and sagging overhead wires.
poles=[(3.65,2),(4.0,-13),(3.7,-28),(4.8,-44)]
for x,z in poles:
    beam((x,0,z),(x-.18,6.4,z),.20,0)
    cube(x-.18,5.96,z,1.95,.16,.16,1)
    for dx in [-.96,-.37,.44]:
        cube(x+dx,6.12,z,.11,.23,.11,6);cube(x+dx,6.20,z,.17,.10,.17,2)
for (x,z),(xx,zz) in zip(poles,poles[1:]):
    for dx in [-.96,-.37,.44]:
        last=(x+dx,6.25,z)
        for i in range(1,8):
            k=i/7;pt=(x+dx+(xx-x)*k,6.25-2.4*k*(1-k),z+(zz-z)*k)
            beam(last,pt,.026,1);last=pt

# Normals and winding are recorded explicitly. The batch uses double-sided
# leaves and weathered sheet metal so torn edges are visible from both sides.
def export_cluster(filename):
    binary=bytearray();views=[];accessors=[]
    def add(values,dtype,kind):
        a=np.asarray(values,dtype=dtype);data=a.tobytes()
        while len(binary)%4:binary.append(0)
        views.append(dict(buffer=0,byteOffset=len(binary),byteLength=len(data)))
        binary.extend(data);item=dict(bufferView=len(views)-1,componentType=5126 if dtype=='<f4' else 5125,count=len(a),type=kind)
        if kind=='VEC3':item.update(min=a.min(axis=0).tolist(),max=a.max(axis=0).tolist())
        accessors.append(item);return len(accessors)-1
    materials=[];prims=[]
    for group,(pos,norm,uv,idx) in parts.items():
        mat=dict(name=group,doubleSided=True,pbrMetallicRoughness=dict(baseColorTexture=dict(index=0),metallicFactor=0,roughnessFactor=.87))
        if group=='water':mat['pbrMetallicRoughness']=dict(baseColorFactor=[.31,.19,.17,1],metallicFactor=.88,roughnessFactor=.08)
        if group=='leaves':mat.update(alphaMode='MASK',alphaCutoff=.45)
        if group=='lamps':mat.update(emissiveFactor=[1,.29,.045])
        materials.append(mat)
        prims.append(dict(attributes=dict(POSITION=add(pos,'<f4','VEC3'),NORMAL=add(norm,'<f4','VEC3'),TEXCOORD_0=add(uv,'<f4','VEC2')),indices=add(idx,'<u4','SCALAR'),material=len(materials)-1))
    doc=dict(asset=dict(version='2.0'),scene=0,scenes=[dict(nodes=[0])],nodes=[dict(name=Path(filename).stem,mesh=0)],meshes=[dict(primitives=prims)],materials=materials,textures=[dict(source=0,sampler=0)],images=[dict(uri='street_atlas.png')],samplers=[dict(magFilter=9728,minFilter=9984)],accessors=accessors,bufferViews=views,buffers=[dict(byteLength=len(binary))])
    js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*(-len(js)%4);binary+=b'\0'*(-len(binary)%4)
    (OUT/filename).write_bytes(struct.pack('<4sII',b'glTF',2,28+len(js)+len(binary))+struct.pack('<I4s',len(js),b'JSON')+js+struct.pack('<I4s',len(binary),b'BIN\0')+binary)
    print({k:len(v[3])//3 for k,v in parts.items()})

export_cluster("street.glb")
parts.clear()
frame()
# A separate cullable stand shelters the access lane outside the checkpoint.
for x,z,h in [(13,-4,7.3),(11,-7,6.3),(14,-9,8.8),(12.8,-14,7.2),(10,-17,8.2),
              (15,-21,7.0),(12,-24,9.1),(22.8,-2.9,6.3),(31,-5,8.5),(32,-14,6.8),(31,-21,8.3)]:
    beam((x,0,z),(x+.12,h,z-.08),h*.029,1)
    for j in range(7):
        level=.17+j*.108
        for k in range(6):
            a=j*.97+k*math.tau/6+rng.uniform(-.13,.13)
            outward=np.array([math.cos(a),0,math.sin(a)])
            w=h*(.24-j*.025)*rng.uniform(.76,1.18)
            stem=np.array([x,h*(level+.12),z])
            elbow=stem+outward*w*.63-[0,.59,0]
            tip=elbow+outward*w*.45-[0,.17,0]
            if j<5:beam(stem,elbow,.065,1)
            tangent=np.array([-outward[2],0,outward[0]])*w*.54
            face([tip-tangent,tip+tangent,stem+tangent*.27,stem-tangent*.27],12,'leaves')
for _ in range(560):
    z=rng.uniform(-22,0);x=rng.choice([rng.uniform(12.3,15.4),rng.uniform(22.6,23.8)])
    d=rng.uniform(.16,.40);h=rng.uniform(.16,.44)
    face([(x-d,.05,z-d*.45),(x+d,.05,z+d*.45),(x+d,h,z+d*.45),(x-d,h,z-d*.45)],13,'leaves')
export_cluster("access_woods.glb")
