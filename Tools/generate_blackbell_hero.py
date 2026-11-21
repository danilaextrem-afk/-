#!/usr/bin/env python3
"""RAZLOM: Black Bell 1348 hero asset builder.
Original procedural geometry and generated PBR maps, no third-party asset packs.
Call: python Tools/generate_blackbell_hero.py [repo_root]
Output is a work-in-progress 3D source asset, NOT an Unreal Engine verified final render.
"""
import math, json, sys
from pathlib import Path
import numpy as np
from PIL import Image
ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path(__file__).resolve().parents[1]
OUT=ROOT/'Content/Art/HeroProps'; TEX=OUT/'Textures'
OUT.mkdir(parents=True,exist_ok=True);TEX.mkdir(parents=True,exist_ok=True)
PI=math.pi; TAU=2*PI
MATERIALS={
'black_bronze':((.095,.087,.078),.83,.62,'bronze'),
'cast_bronze':((.145,.113,.087),.8,.57,'bronze'),
'black_patina':((.04,.085,.082),.40,.84,'bronze'),
'golden_rim':((.54,.365,.14),.83,.37,'bronze'),
'age_gilt':((.30,.22,.125),.77,.55,'bronze'),
'forged_iron':((.10,.12,.13),.88,.75,'iron'),
'polished_iron':((.28,.30,.32),.87,.37,'iron'),
'old_oak':((.26,.152,.078),.02,.79,'wood'),
'wood_crack':((.09,.050,.031),.01,.94,'wood'),
'cyan_enamel':((.025,.23,.21),.16,.28,None),
'rune_inlay':((.44,.37,.20),.81,.51,'bronze')}
class Mesh:
 def __init__(self):self.vertices=[];self.uv=[];self.faces=[]
 def v(self,pos,uv=(0,0)):
  self.vertices.append(tuple(float(x) for x in pos));self.uv.append(tuple(float(x) for x in uv));return len(self.vertices)
 def tri(self,a,b,c,m):self.faces.append((a,b,c,m))
 def quad(self,a,b,c,d,m):self.tri(a,b,c,m);self.tri(a,c,d,m)
 def tube(self,pts,r,material,sides=8):
  if len(pts)<2:return
  old=None
  for j,pt in enumerate(pts):
   a=np.array(pts[max(0,j-1)],dtype=np.float64);b=np.array(pts[min(len(pts)-1,j+1)],dtype=np.float64)
   t=b-a;t/=max(np.linalg.norm(t),1e-9)
   ref=np.array([0.,0.,1.]) if abs(t[2])<.86 else np.array([1.,0.,0.])
   n=np.cross(t,ref);n/=max(np.linalg.norm(n),1e-9);v=np.cross(t,n)
   row=[self.v(np.array(pt)+r*(n*math.cos(TAU*k/sides)+v*math.sin(TAU*k/sides)),(k/sides,j/max(1,len(pts)-1))) for k in range(sides+1)]
   if old:
    for k in range(sides):self.quad(old[k],old[k+1],row[k+1],row[k],material)
   old=row
 def sphere(self,pos,r,mat,ns=12,nt=7):
  old=None
  for j in range(nt+1):
   p=PI*j/nt;row=[self.v((pos[0]+r*math.cos(TAU*k/ns)*math.sin(p),pos[1]+r*math.sin(TAU*k/ns)*math.sin(p),pos[2]+r*math.cos(p)),(k/ns,j/nt)) for k in range(ns+1)]
   if old:
    for k in range(ns):self.quad(old[k],old[k+1],row[k+1],row[k],mat)
   old=row
 def medal(self,theta,z,r,mat):
  axis=np.array((math.cos(theta),math.sin(theta),0.));tangent=np.array((-math.sin(theta),math.cos(theta),0.))
  center=axis*(R(z)+2)+np.array((0.,0.,z));rows=[]
  for depth in [0,2]:
   row=[self.v(center+depth*axis+r*(tangent*math.cos(TAU*i/24)+np.array((0,0,1.))*math.sin(TAU*i/24)),(i/24,depth/2)) for i in range(25)]
   rows.append(row)
  for i in range(24):self.quad(rows[0][i],rows[0][i+1],rows[1][i+1],rows[1][i],mat)
  c=self.v(center+axis*2)
  for i in range(24):self.tri(c,rows[1][i],rows[1][i+1],mat)
M=Mesh()
def R(z):
 u=max(0,min(1,(z-18)/143));return 13+49.2*(1-u)**.65+1.4*math.sin(PI*u)+.6*math.sin(4*PI*u)
def surf(a,z,offset=0):
 r=R(z)+.32*math.sin(13*a+.16*z)*math.sin(7*a-.2*z)+.13*math.sin(35*a+.7*z)+offset
 return (r*math.cos(a),r*math.sin(a),z)
NS=256;NR=132;old=None
for j in range(NR+1):
 z=18+143*j/NR;row=[M.v(surf(TAU*i/NS,z),(i/NS,j/NR)) for i in range(NS+1)]
 if old:
  for i in range(NS):
   mat='black_patina' if j%19==0 and math.sin(i*.31+j*.17)>.8 else 'cast_bronze'
   if j<12 or j>126:mat='black_bronze'
   M.quad(old[i],old[i+1],row[i+1],row[i],mat)
 old=row
old=None
for j in range(49):
 z=18+112*j/48;r=R(z)-3.8-1.5*(z-18)/112
 row=[M.v((r*math.cos(TAU*i/NS),r*math.sin(TAU*i/NS),z),(i/NS,j/48)) for i in range(NS+1)]
 if old:
  for i in range(NS):M.quad(old[i+1],old[i],row[i],row[i+1],'black_patina')
 old=row
outer=[M.v(surf(TAU*i/NS,18),(i/NS,0)) for i in range(NS+1)]
inner=[M.v(((R(18)-3.8)*math.cos(TAU*i/NS),(R(18)-3.8)*math.sin(TAU*i/NS),18),(i/NS,0)) for i in range(NS+1)]
for i in range(NS):M.quad(outer[i],inner[i],inner[i+1],outer[i+1],'golden_rim')
def stripe(z,offset,rad,mat,wave=0):
 M.tube([surf(TAU*k/256,z+wave*math.sin(21*TAU*k/256),offset) for k in range(257)],rad,mat,8)
for z,d,r,mat in [(20,1.8,2.4,'age_gilt'),(24,2.1,1.8,'golden_rim'),(33,1.6,1.6,'age_gilt'),(41,1.2,1.2,'golden_rim'),(48,1.4,1.4,'age_gilt'),(62,1.4,1.,'golden_rim'),(114,1.3,1.6,'age_gilt'),(121,1.2,.9,'golden_rim'),(142,.8,1.6,'age_gilt')]:stripe(z,d,r,mat,.2)
for z in (28,43,125):
 stripe(z,1.4,.65,'golden_rim',1)
 stripe(z+1.6,1.4,.55,'age_gilt',1)
for z in (27,42):
 for k in range(48):M.sphere(surf(TAU*(k+.5)/48,z,2.5),1.25,'golden_rim',10,6)
for k in range(18):
 th=TAU*k/18;M.medal(th,95,7.4,'age_gilt');M.medal(th,95,3.8,'cyan_enamel');M.sphere(surf(th,95,5.2),2.15,'golden_rim',10,6)
STROKES=[[(-.9,1),(-.9,-1)],[(.9,1),(.9,-1)],[(-.9,.85),(.8,.1),(-.9,-.82)],[(-1.15,0),(1.15,0)],[(-.55,.93),(.5,1.34)],[(-.25,-1.23),(.45,-1.04)]]
for k in range(12):
 th=TAU*k/12+.13
 for j,stroke in enumerate(STROKES):
  pts=[]
  for (xa,za),(xb,zb) in zip(stroke,stroke[1:]):
   for i in range(7):
    t=i/7;xx=xa*(1-t)+xb*t;zz=za*(1-t)+zb*t
    pts.append(surf(th+xx*5.1/R(79),79+zz*5.1,1.6))
  xx,zz=stroke[-1];pts.append(surf(th+xx*5.1/R(79),79+zz*5.1,1.6))
  M.tube(pts,.62 if j==3 else .94,'rune_inlay',7)
for k in range(7):
 th=TAU*k/7+.2;M.tube([surf(th+(j/20-.5)*.35,133+10*math.sin(PI*j/20),1.5) for j in range(21)],1.2,'golden_rim',8)
for z,r,mat in [(156,17,'age_gilt'),(161,13,'black_bronze'),(167,10.5,'forged_iron')]:
 a=[M.v((r*math.cos(TAU*i/96),r*math.sin(TAU*i/96),z),(i/96,0)) for i in range(97)]
 b=[M.v((r*math.cos(TAU*i/96),r*math.sin(TAU*i/96),z+5),(i/96,1)) for i in range(97)]
 for i in range(96):M.quad(a[i],a[i+1],b[i+1],b[i],mat)
for side in (-1,1):M.tube([(side*(11+17*math.sin(PI*i/28)),0,173+17*math.cos(PI*i/28)) for i in range(29)],4.2,'forged_iron',12)
M.tube([(0,0,132),(0,0,114),(1,0,91),(2,0,77),(2,0,62)],3.4,'polished_iron',12)
M.sphere((2,0,58),10.5,'polished_iron',24,14)
# Old oak yoke with an octagonal bevel and forged bands.
section=[(-15,191),(15,191),(21,197),(21,219),(15,225),(-15,225),(-21,219),(-21,197)]
a=[M.v((-115,y,z),(0,i/8)) for i,(y,z) in enumerate(section)]
b=[M.v((115,y,z),(1,i/8)) for i,(y,z) in enumerate(section)]
for i in range(8):M.quad(a[i],b[i],b[(i+1)%8],a[(i+1)%8],'old_oak')
for i in range(1,7):M.tri(a[0],a[i+1],a[i],'old_oak');M.tri(b[0],b[i],b[i+1],'old_oak')
for x in (-85,-40,0,40,85):
 section=[(-20,205),(-17,226),(17,226),(20,205),(18,193),(-18,193)]
 a=[M.v((x-3.75,y,z),(0,i/6)) for i,(y,z) in enumerate(section)]
 b=[M.v((x+3.75,y,z),(1,i/6)) for i,(y,z) in enumerate(section)]
 for i in range(6):M.quad(a[i],a[(i+1)%6],b[(i+1)%6],b[i],'forged_iron')
 for side in (-1,1):M.sphere((x,side*20.6,207),2.35,'age_gilt',10,6)
for j in range(25):
 y=-18.7+37.4*j/25;z=224.7 if j%2 else 191.2
 M.tube([(-111+i*3.7,y+.45*math.sin(i*.19+j),z+.35*math.sin(i*.23+j*2)) for i in range(61)],.26,'wood_crack' if j%4==0 else 'old_oak',5)
for x in (-52,52):
 M.tube([(x,-25,212),(x,-22,194),(.8*x,-15,181),(.5*x,-8,170),(.4*x,-4,166)],4.4,'forged_iron',12)
 for y in (-23,23):
  for z in (199,216):M.sphere((x,y,z),2.7,'age_gilt',10,7)
for k in range(8):M.sphere((-100+k*28,-22.2,211),1.5,'polished_iron',10,7)
vertices=np.array(M.vertices,dtype=np.float32)
tris=np.array([[a-1,b-1,c-1] for a,b,c,_ in M.faces],dtype=np.int32)
fn=np.cross(vertices[tris[:,1]]-vertices[tris[:,0]],vertices[tris[:,2]]-vertices[tris[:,0]])
vn=np.zeros_like(vertices)
for j in range(3):np.add.at(vn,tris[:,j],fn)
vn/=np.maximum(np.linalg.norm(vn,axis=1,keepdims=True),1e-6)
# Deterministic original PBR maps for bronze / iron / oak.
rng=np.random.default_rng(1348);SIZE=1024
def noise():
 n=np.zeros((SIZE,SIZE),dtype=np.float32)
 for cell,w in ((12,.48),(42,.26),(134,.16),(420,.1)):
  a=(rng.random((cell,cell))*255).astype('uint8')
  b=np.asarray(Image.fromarray(a,'L').resize((SIZE,SIZE),Image.Resampling.BICUBIC),dtype=np.float32)/255
  n+=w*b
 return np.clip((n-.5)*1.5+.5,0,1)
for group in ('bronze','iron','wood'):
 n=noise();fine=noise();y,x=np.mgrid[:SIZE,:SIZE]
 if group=='bronze':
  patina=np.clip((n-.54)*3,0,1)
  rgb=np.stack((83+67*n-55*patina,63+49*n+25*patina,49+35*n+26*patina),2)
  rough=130+74*n;metal=215-65*patina
 elif group=='iron':
  rgb=np.stack((40+80*n+22*fine,45+66*n,48+71*n),2)
  rough=130+95*n;metal=213-62*np.clip(n-.67,0,1)
 else:
  grain=np.sin(x*.12+8*n+2*np.sin(x*.018))
  rgb=np.stack((70+68*n+22*grain,35+42*n+15*grain,18+26*n+9*grain),2)
  rough=167+64*n;metal=np.zeros_like(n)
 normal=np.stack((128-np.gradient(n,axis=1)*200,128+np.gradient(n,axis=0)*200,255-120*np.abs(fine-n)),2)
 Image.fromarray(np.clip(rgb,0,255).astype('uint8'),'RGB').save(TEX/f'{group}_albedo.png',optimize=True)
 Image.fromarray(np.clip(normal,0,255).astype('uint8'),'RGB').save(TEX/f'{group}_normal.png',optimize=True)
 Image.fromarray(np.clip(rough,0,255).astype('uint8'),'L').save(TEX/f'{group}_roughness.png',optimize=True)
 Image.fromarray(np.clip(metal,0,255).astype('uint8'),'L').save(TEX/f'{group}_metallic.png',optimize=True)
mtl=[]
for name,(rgb,metal,rough,group) in MATERIALS.items():
 mtl.append(f'newmtl {name}\nKa .02 .02 .02\nKd {rgb[0]} {rgb[1]} {rgb[2]}\nKs .25 .25 .25\nNs {round((1-rough)*110+8)}\nillum 2\n# ue5_metallic={metal} roughness={rough}\n')
 if group:mtl.append(f'map_Kd Textures/{group}_albedo.png\nmap_Bump Textures/{group}_normal.png\n')
(OUT/'BlackBell_Hero_v2.mtl').write_text('\n'.join(mtl),encoding='utf8')
with (OUT/'SM_BlackBell_Hero_v2.obj').open('w',encoding='utf8') as h:
 h.write('# RAZLOM ORIGINAL HERO MESH; centimeters; Z-up\nmtllib BlackBell_Hero_v2.mtl\no SM_BlackBell_Hero_v2\ns 1\n')
 for a in vertices:h.write('v '+' '.join(f'{x:.4f}' for x in a)+'\n')
 for u,w in M.uv:h.write(f'vt {u:.5f} {w:.5f}\n')
 for a in vn:h.write('vn '+' '.join(f'{x:.5f}' for x in a)+'\n')
 prev=''
 for a,b,c,m in M.faces:
  if m!=prev:h.write('usemtl '+m+'\n');prev=m
  h.write(f'f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}\n')
state={'project':'RAZLOM: CHERNY ZVON','asset':'Black Bell 1348 v2','source':'Tools/generate_blackbell_hero.py','file':'Content/Art/HeroProps/SM_BlackBell_Hero_v2.obj','vertices':len(M.vertices),'triangles':len(M.faces),'uvs':len(M.uv),'materials':len(MATERIALS),'pbr_textures':12,'target':'UE5 Windows PC','quality':'Work in progress, not UE5 verified nor AAA approved'}
(OUT/'BlackBell_Hero_v2.json').write_text(json.dumps(state,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps(state,ensure_ascii=False))
