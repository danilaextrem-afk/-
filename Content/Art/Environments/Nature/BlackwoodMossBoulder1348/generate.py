#!/usr/bin/env python3
"""Original procedural boulder. Standard-library only. Run: python generate.py"""
import math,json,pathlib
P=pathlib.Path(__file__).parent
def mesh(name,lat,lon):
 v=[];f=[]
 for i in range(lat+1):
  t=math.pi*i/lat
  for j in range(lon+1):
   p=2*math.pi*j/lon
   r=1+0.13*math.sin(3*p+t)*math.sin(4*t)+0.075*math.cos(5*p-2*t)
   v.append((1.28*r*math.sin(t)*math.cos(p),0.89*r*math.sin(t)*math.sin(p),0.74*r*math.cos(t)+0.06*math.sin(4*p)*math.sin(t)))
 for i in range(lat):
  for j in range(lon):
   a=i*(lon+1)+j+1;b=a+1;c=a+lon+1;d=c+1
   if i>0:f.append((a,c,b))
   if i<lat-1:f.append((b,c,d))
 s=['mtllib materials.mtl','o BlackwoodMossBoulder','usemtl StoneMoss']
 s+=['v %.7f %.7f %.7f'%q for q in v]
 s+=['vt %.7f %.7f'%(j/lon,1-i/lat) for i in range(lat+1) for j in range(lon+1)]
 s+=['f %d/%d %d/%d %d/%d'%(a,a,b,b,c,c) for a,b,c in f]
 (P/name).write_text('\n'.join(s)+'\n')
def ppm(name,fn):
 n=32;s=['P3',f'{n} {n}','255']
 for y in range(n):
  for x in range(n):s.append(' '.join(str(max(0,min(255,int(q)))) for q in fn(x/n,y/n)))
 (P/name).write_text('\n'.join(s)+'\n')
mesh('LOD0.obj',24,48);mesh('LOD1.obj',12,24)
ppm('basecolor.ppm',lambda u,v:(52,62,51) if math.sin(19*u+4*math.sin(11*v))*math.cos(15*v)>0.22 else (93+16*math.sin(20*u),91+12*math.sin(16*v),78))
ppm('roughness.ppm',lambda u,v:(205+20*math.sin(16*u)*math.cos(17*v),)*3)
ppm('metallic.ppm',lambda u,v:(0,0,0))
ppm('normal.ppm',lambda u,v:(128+12*math.sin(25*u),128+12*math.cos(22*v),252))
(P/'materials.mtl').write_text('newmtl StoneMoss\nKd 0.35 0.36 0.31\nKs 0.02 0.02 0.02\nNs 12\nmap_Kd basecolor.ppm\n')
