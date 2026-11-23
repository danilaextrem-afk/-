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
put(15,ImageEnhance.Brightness(ImageEnhance.Color(swatches[2]).enhance(.43)).enhance(.83))
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

def window(x,y,z,lit=False):
    cube(x,y,z,1.02,1.08,.09,1)
    cube(x,y,z+.06,.82,.88,.04,14 if lit else 6,'lamps' if lit else 'street')
    for dx in [-.48,0,.48]:cube(x+dx,y,z+.11,.07,1.12,.09,2)
    for dy in [-.53,.12,.55]:cube(x,y+dy,z+.12,1.09,.08,.12,2)
    cube(x,y-.60,z+.16,1.23,.10,.25,0)
def house(x,z,angle,w,h,depth,t,lit):
    frame(x,z,angle);f=depth/2
    cube(0,.2,0,w+.1,.4,depth+.1,7)
    cube(0,h/2+.25,0,w,h,depth,t)
    # Narrow overlapping courses cast real shadows along the facade.
    for row in range(12):
        yy=.37+row*(h/12)
        cube(0,yy,f+.018,w,.057,.061,t)
        for side in [-1,1]:cube(side*(w/2+.012),yy,0,.045,.047,depth,t)
    for zz,order in [(f,1),(-f,-1)]:
        p=[(-w/2,h+.24,zz),(w/2,h+.24,zz),(0,h+2.1,zz)]
        face(p if order==1 else p[::-1],t)
    for side in [-1,1]:
        ex=side*(w/2+.40);ez=f+.48;ridge=h+2.22
        face([(0,ridge,ez),(ex,h+.12,ez),(ex,h+.12,-ez),(0,ridge,-ez)],3)
        beam((ex,h+.12,ez),(0,ridge,ez),.17,1)
        beam((ex,h+.12,-ez),(ex,h+.12,ez),.16,1)
        for i in range(1,7):
            k=i/7;beam((ex*k,ridge+(h+.12-ridge)*k+.04,-ez),(ex*k,ridge+(h+.12-ridge)*k+.04,ez),.055,3)
    cube(w*.24,h+1.91,-f*.5,.49,1.54,.51,7);cube(w*.24,h+2.72,-f*.5,.61,.12,.63,1)
    for xx in [-w/2+.08,w/2-.08]:cube(xx,h/2+.22,f+.04,.18,h,.16,1)
    for xx in [-w*.32,w*.28]:window(xx,1.76,f+.04,lit and xx>0)
    for xx in [-w*.32,w*.28]:
        for dx in [-.77,.77]:
            cube(xx+dx,1.77,f+.055,.38,1.11,.08,2 if t==0 else 0)
            for yb in [1.39,2.16]:cube(xx+dx,yb,f+.114,.40,.073,.052,1)
    cube(0,1.11,f+.06,.91,1.92,.15,1)
    for i in range(5):cube(-.34+i*.17,1.12,f+.16,.15,1.77,.05,t)
    cube(.29,1.15,f+.21,.13,.06,.09,4)
    for i in range(3):cube(0,.10+i*.095,f+.80-i*.23,1.44,.19+i*.19,.32,7)
    for i in [-1,1]:cube(i*.83,1.42,f+.81,.12,2.8,.12,0)
    cube(0,2.74,f+.50,1.98,.15,1.03,3)
    # Gable louvre and porch lantern.
    for i in range(5):cube(0,h+.5+i*.12,f+.04,.58-i*.05,.066,.08,1)
    cube(.72,2.05,f+.19,.20,.28,.21,14 if lit else 6,'lamps' if lit else 'street')
    cube(.72,2.23,f+.19,.29,.07,.27,1)
    # Side wall facing the arriving player.
    oldorigin,oldrot=origin.copy(),rotation.copy()
    for side in [-1,1]:
        centre=oldorigin+oldrot@np.array([side*w/2,0,0])
        frame(centre[0],centre[2],angle+side*math.pi/2)
        for wx in [-depth*.24,depth*.24]:window(wx,1.72,.015,False)
    frame()

def post_office():
    # Local +Z faces the road. The counter and shelving are seen through the window.
    frame(7.5,-6.4,-.68)
    f=2.08
    cube(0,.19,0,6.7,.38,4.35,7)
    cube(0,1.77,-2.03,6.5,2.90,.13,7)
    for side in [-1,1]:cube(side*3.20,1.77,0,.16,2.90,4.13,7)
    cube(0,.62,f,6.5,.49,.14,7)
    cube(0,2.87,f,6.5,.7,.14,7)
    for x,w in [(-2.87,.78),(-.45,.35),(1.69,2.99)]:
        cube(x,1.7,f,w,1.74,.18,7)
    cube(0,3.22,0,6.7,.21,4.38,1)
    # Broken hip roof, inset ridge and projecting eaves.
    corners=[(-3.56,3.28,2.46),(3.56,3.28,2.46),(3.56,3.28,-2.46),(-3.56,3.28,-2.46)]
    left=(-1.92,4.71,-.04);right=(1.69,4.63,-.04)
    face([corners[0],corners[1],right,left],3)
    face([corners[1],corners[2],right],3)
    face([corners[2],corners[3],left,right],3)
    face([corners[3],corners[0],left],3)
    for i in range(4):beam(corners[i],corners[(i+1)%4],.12,1)
    beam(left,right,.12,3)
    cube(1.57,4.35,-.78,.50,1.82,.48,7)
    cube(1.57,5.28,-.78,.63,.13,.61,4)
    # Open service window, with a return wall and sorting cubbies inside.
    cube(-1.5,1.17,.69,2.15,.13,1.1,0)
    for row in range(4):cube(-1.5,.93+row*.42,-1.84,2.9,.06,.32,0)
    for col in range(6):cube(-2.95+col*.58,1.55,-1.84,.04,1.32,.32,1)
    for xx in [-2.46,-1.56,-.67]:
        cube(xx,1.68,f+.02,.08,1.71,.16,2)
    for yy in [.87,2.12,2.52]:cube(-1.56,yy,f+.08,1.94,.09,.17,2)
    cube(-1.57,.84,f+.15,2.04,.12,.41,0)
    for xx,yy in [(-2.20,1.51),(-1.55,1.75)]:
        beam((xx-.33,yy-.12,f+.14),(xx+.37,yy+.20,f+.14),.135,0)
    cube(.0,1.48,1.72,.72,2.18,.13,2)
    cube(.24,1.29,1.88,.13,.06,.07,4)
    # Sign with a bevelled steel surround and enamel face.
    cube(-.61,2.91,f+.16,3.74,.57,.12,1)
    face([(-2.42,2.67,f+.228),(1.20,2.67,f+.228),(1.20,3.15,f+.228),(-2.42,3.15,f+.228)],11)
    for row in range(3):
        cube(.15,.10+row*.075,3.02-row*.29,1.63,.20+row*.15,.32,7)
    for xx in [-1.13,1.33]:
        beam((xx,.32,3.11),(xx,2.51,3.11),.10,4)
        beam((xx,2.06,3.11),(xx,2.54,2.57),.07,4)
    face([(-1.3,2.46,3.32),(1.49,2.46,3.32),(1.49,2.71,f),(-1.3,2.71,f)],3)
    # A wall-mounted blue postal box and an amber lamp with an iron cage.
    cube(2.31,1.26,f+.17,.51,.62,.27,2)
    cube(2.31,1.43,f+.32,.38,.05,.04,1)
    cube(2.31,1.59,f+.20,.57,.06,.35,3)
    for xx in [-2.82,1.68]:
        cube(xx,2.13,f+.25,.24,.32,.19,14,'lamps')
        for dx in [-.13,.13]:cube(xx+dx,2.13,f+.36,.03,.39,.04,1)
        cube(xx,2.35,f+.25,.38,.08,.32,4)
    frame()

post_office()
houses=[(-7.6,-6,.97,5.6,2.62,5.5,2,False),(-9.3,-20,1.35,6.9,3.05,5.7,0,False),(8.5,-17,-1.35,4.9,2.42,5.1,2,False),(-6.8,-33,.55,5.4,2.72,4.7,0,True),(7.1,-28,-1.57,6.2,2.77,5.0,5,True)]
for params in houses:house(*params)
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

# Abandoned estate car with a sloped cabin, wheel wells, grille and lamps.
frame(-3.25,-4.7,.95)
cube(0,.59,0,1.55,.48,3.52,15)
cube(0,.86,1.14,1.53,.16,1.22,15)
cube(0,.96,-.40,1.49,.22,1.92,15)
for side in [-1,1]:
    for zz in [-1.12,1.06]:
        ring=[(.40+math.sin(k*math.tau/10)*.36,zz+math.cos(k*math.tau/10)*.36) for k in range(10)]
        for i,(y,z) in enumerate(ring):
            yy,z2=ring[(i+1)%10]
            face([(side*.67,y,z),(side*.84,y,z),(side*.84,yy,z2),(side*.67,yy,z2)],1)
            face([(side*.85,.40,zz),(side*.85,y,z),(side*.85,yy,z2)],1)
        cube(side*.862,.40,zz,.035,.24,.24,4)
    face([(side*.73,1.03,.58),(side*.60,1.51,.12),(side*.60,1.51,-1.08),(side*.73,1.03,-1.39)],6)
    beam((side*.70,1.04,-.54),(side*.60,1.51,-.54),.07,15)
    cube(side*.76,.83,-.37,.05,.05,.17,1)
face([(-.73,1.03,.58),(.73,1.03,.58),(.60,1.51,.12),(-.60,1.51,.12)],6)
face([(.73,1.03,-1.39),(-.73,1.03,-1.39),(-.60,1.51,-1.08),(.60,1.51,-1.08)],6)
cube(0,1.53,-.47,1.3,.075,1.31,15)
for xx in [-.52,.52]:
    beam((xx,1.58,-1.02),(xx,1.58,.02),.045,5)
    beam((xx*.93,1.05,.59),(xx+.11,1.19,.47),.025,1)
for zz in [-.95,-.16]:
    beam((-.56,1.64,zz),(.56,1.64,zz),.05,5)
for side in [-1,1]:
    cube(side*.77,.80,-.48,.05,.04,1.85,5)
    beam((side*.74,1.0,.38),(side*.88,1.12,.40),.05,4)
    cube(side*.91,1.14,.40,.13,.105,.15,5)
cube(0,.38,1.79,1.67,.12,.14,5)
for xx in [-.58,.58]:cube(xx,.70,1.77,.27,.16,.07,7)
for yy in [.57,.64,.71]:cube(0,yy,1.77,.74,.035,.065,1)
frame()

# Octagonal water tower with an exposed lattice, elevated above the roofline.
tx,tz=.6,-49
for dx in [-1,1]:
    for dz in [-1,1]:
        beam((tx+dx*1.62,0,tz+dz*1.62),(tx+dx,7.6,tz+dz),.15,4)
    for mid in [0,3.4]:
        beam((tx+dx*1.44,mid,tz+1.25),(tx-dx*1.16,mid+3.6,tz+1.12),.10,4)
for i in range(12):
    a,b=i*math.tau/12,(i+1)*math.tau/12
    def pt(r,y,ang):return (tx+r*math.cos(ang),y,tz+r*math.sin(ang))
    for lo,hi,r1,r2,t in [(6.8,7.7,.8,2.0,4),(7.7,9.9,2,2,5),(9.9,10.75,2.13,.14,3)]:
        face([pt(r1,lo,a),pt(r2,hi,a),pt(r2,hi,b),pt(r1,lo,b)],t)
def birch(x,z,h):
    frame(x,z,0)
    trunk=[(0,.03,0),(.15,h*.31,.08),(-.04,h*.60,-.04),(.32,h*.88,-.25),(.21,h,-.4)]
    for j,(a,b) in enumerate(zip(trunk,trunk[1:])):
        beam(a,b,.30-j*.061,7)
    for i in range(17):
        y=h*(.33+.036*i)
        angle=i*2.399+rng.uniform(-.21,.21)
        length=h*(.25+.09*rng.random())*(1-.60*i/17)
        direction=np.array([math.cos(angle),0,math.sin(angle)])
        base=np.array([.09,y,-.05]);elbow=base+direction*length*.54+[0,.59,0]
        end=base+direction*length+[0,.98,0]
        beam(base,elbow,.10*(1-i/24),0);beam(elbow,end,.054,0)
        for j in range(6):
            a=end+np.array([rng.uniform(-.66,.66),rng.uniform(-.5,.5),rng.uniform(-.66,.66)])
            span=rng.uniform(.57,1.13);ang=rng.random()*math.pi
            v=np.array([math.cos(ang),0,math.sin(ang)])*span
            face([a-v-[0,.4,0],a+v-[0,.4,0],a+v+[0,.9,0],a-v+[0,.9,0]],8,'leaves')
    frame()
for tree in [(-4.9,.65,7.7),(5.5,-1.1,7.1),(-8,-13,7.9),(5.1,-22,6.4),(-9,-38,7.1)]:
    birch(*tree)

# Pine boughs and seed heads use cutout cards with original pixel masks.
for i in range(88):
    z=rng.uniform(-54,3)
    x=rng.choice([-1,1])*rng.uniform(12,26)
    if i>74:z=rng.uniform(-64,-54);x=rng.uniform(-23,23)
    h=rng.uniform(4.3,10.5)
    beam((x,0,z),(x,h*.94,z),h*.031,1)
    for j in range(4):
        level=.19+j*.16;w=h*(.27-j*.039)
        for k in range(5):
            a=k*math.tau/5+j*.62;d=np.array([math.cos(a),0,math.sin(a)])
            side=np.array([-d[2],0,d[0]])*w*.69
            tip=np.array([x,h*level,z])+d*w;stem=np.array([x,h*(level+.29),z])
            face([tip-side,tip+side,stem+side*.38,stem-side*.38],12,'leaves')
for _ in range(1000):
    z=rng.uniform(-43,4);x=rng.choice([-1,1])*rng.uniform(2.25,10.5)
    w=rng.uniform(.16,.45);h=rng.uniform(.18,.59)
    a=rng.random()*math.pi;dx,dz=w*math.cos(a),w*math.sin(a)
    face([(x-dx,.07,z-dz),(x+dx,.07,z+dz),(x+dx+.06,h,z+dz),(x-dx+.06,h,z-dz)],13,'leaves')
# Normals and winding are recorded explicitly. The batch uses double-sided
# leaves and weathered sheet metal so torn edges are visible from both sides.
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
doc=dict(asset=dict(version='2.0'),scene=0,scenes=[dict(nodes=[0])],nodes=[dict(name='CrimsonStreet',mesh=0)],meshes=[dict(primitives=prims)],materials=materials,textures=[dict(source=0,sampler=0)],images=[dict(uri='street_atlas.png')],samplers=[dict(magFilter=9728,minFilter=9984)],accessors=accessors,bufferViews=views,buffers=[dict(byteLength=len(binary))])
js=json.dumps(doc,separators=(',',':')).encode();js+=b' '*(-len(js)%4);binary+=b'\0'*(-len(binary)%4)
(OUT/'street.glb').write_bytes(struct.pack('<4sII',b'glTF',2,28+len(js)+len(binary))+struct.pack('<I4s',len(js),b'JSON')+js+struct.pack('<I4s',len(binary),b'BIN\0')+binary)
print({k:len(v[3])//3 for k,v in parts.items()})
