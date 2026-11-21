#!/usr/bin/env python3
"""Original procedural mesh for occult stairwell creature, centimeters, Z-up. Stdlib only."""
import math, json, pathlib, random
P=pathlib.Path(__file__).resolve().parent
random.seed(13482026)
verts=[];uv=[];faces=[];groups=[]
def ring_surface(name, centers, radii, material, sides=16, phase=0):
    start=len(verts); n=len(centers)
    for j,((x,y,z),(rx,ry)) in enumerate(zip(centers,radii)):
        for k in range(sides):
            a=2*math.pi*k/sides+phase
            verts.append((x+rx*math.cos(a),y+ry*math.sin(a),z))
            uv.append((k/sides,j/(n-1)))
    for j in range(n-1):
        for k in range(sides):
            a=start+j*sides+k;b=start+j*sides+(k+1)%sides
            c=start+(j+1)*sides+k;d=start+(j+1)*sides+(k+1)%sides
            faces.extend([(a,b,c,material,name),(b,d,c,material,name)])
def tapered(name, a,b,r0,r1,mat,segments=9):
    # tube oriented along arbitrary 3D axis, with slight irregularity
    vx,vy,vz=[b[i]-a[i] for i in range(3)]
    length=math.sqrt(vx*vx+vy*vy+vz*vz); direction=(vx/length,vy/length,vz/length)
    up=(0,0,1) if abs(direction[2])<0.9 else (0,1,0)
    u=(direction[1]*up[2]-direction[2]*up[1],direction[2]*up[0]-direction[0]*up[2],direction[0]*up[1]-direction[1]*up[0])
    l=math.sqrt(sum(v*v for v in u));u=tuple(v/l for v in u)
    w=(direction[1]*u[2]-direction[2]*u[1],direction[2]*u[0]-direction[0]*u[2],direction[0]*u[1]-direction[1]*u[0])
    start=len(verts);rings=5
    for j in range(rings):
        t=j/(rings-1);r=r0*(1-t)+r1*t
        for k in range(segments):
            theta=2*math.pi*k/segments
            point=tuple(a[i]+t*(b[i]-a[i])+r*(math.cos(theta)*u[i]+math.sin(theta)*w[i]) for i in range(3))
            verts.append(point);uv.append((k/segments,t))
    for j in range(rings-1):
        for k in range(segments):
            p=start+j*segments+k;q=start+j*segments+(k+1)%segments
            s=start+(j+1)*segments+k;t=start+(j+1)*segments+(k+1)%segments
            faces.extend([(p,q,s,mat,name),(q,t,s,mat,name)])
# Height ~275cm; bent stairwell humanoid with elongated forearms
ring_surface("trunk",[(0,0,55),(0,-3,80),(0,-8,115),(0,-14,151),(0,-20,187),(0,-24,210),(0,-21,227)],
 [(19,14),(26,17),(27,17),(22,15),(32,20),(35,22),(14,14)],"soot_cloth",24)
ring_surface("hood",[(0,-21,209),(0,-24,225),(0,-27,245),(0,-30,264),(0,-31,274),(0,-31,278)],
 [(21,18),(28,23),(27,22),(21,17),(12,12),(1,1)],"soot_cloth",24)
ring_surface("faceless_void",[(0,-47,229),(0,-49,242),(0,-49,255),(0,-47,263)],
 [(12,2),(16,3),(14,3),(8,1)],"void",18)
for side in (-1,1):
    x=side
    tapered("thigh", (x*14,0,70),(x*21,1,36),11,8,"pale_skin",14)
    tapered("shin", (x*21,1,36),(x*25,-9,8),8,4,"pale_skin",14)
    tapered("foot", (x*25,-9,8),(x*25,-29,3),6,2,"pale_skin",12)
    tapered("shoulder",(x*28,-17,203),(x*47,-22,167),11,8,"soot_cloth",14)
    tapered("forearm",(x*47,-22,167),(x*63,-44,91),9,5,"pale_skin",14)
    tapered("hand",(x*63,-44,91),(x*64,-48,68),6,4,"pale_skin",12)
    for finger in range(4):
        tapered("finger",(x*(59+finger*3),-48,69),(x*(61+finger*4),-58,46-finger*2),2.4,0.6,"pale_skin",8)
    # hanging tendrils / ripped cloth
    for j in range(5):
        phi=j*0.83+side*0.3
        tapered("rag",(x*(10+j*4),-8,124-j*4),(x*(13+j*5),-13+math.sin(phi)*6,60-j*7),3.8,0.3,"soot_cloth",7)
# crooked vertebral protrusions
for j in range(7):
    z=113+j*14
    tapered("spine",(0,12,z),(math.sin(j*1.7)*5,21,z+7),4.5,0.3,"bone",9)
# old apartment door keys fused into ribs
for j in range(6):
    side=-1 if j%2 else 1
    z=131+j*11
    tapered("key_shard",(side*21,-19,z),(side*35,-27,z+7),3.1,0.4,"oxidized_iron",9)
with (P/"SM_Podezdnik_2026_v01.obj").open("w") as f:
    f.write("# RAZLOM original mesh, centimeters Z-up; no animation rig\nmtllib podezdnik.mtl\n")
    for x,y,z in verts:f.write(f"v {x:.5f} {y:.5f} {z:.5f}\n")
    for u,v in uv:f.write(f"vt {u:.5f} {v:.5f}\n")
    prev=None
    for a,b,c,mat,name in faces:
        if (mat,name)!=prev:f.write(f"g {name}\nusemtl {mat}\n");prev=(mat,name)
        f.write("f "+" ".join(f"{i+1}/{i+1}" for i in (a,b,c))+"\n")
with (P/"podezdnik.mtl").open("w") as f:
    for name,col in [("soot_cloth",(0.09,0.10,0.11)),("pale_skin",(0.32,0.30,0.29)),("void",(0.015,0.014,0.020)),("bone",(0.44,0.41,0.36)),("oxidized_iron",(0.13,0.15,0.16))]:
        f.write(f"newmtl {name}\nKd {' '.join(map(str,col))}\nPr 0.85\nPm {1 if name=='oxidized_iron' else 0}\n\n")
# 4 draft PBR maps per material, ASCII PPM for portability, not production baked maps
for name,base in [("soot_cloth",(25,28,30)),("pale_skin",(83,78,74)),("void",(5,4,7)),("bone",(113,104,90)),("oxidized_iron",(40,45,48))]:
    for kind in ("basecolor","roughness","metallic","normal"):
        with (P/f"{name}_{kind}.ppm").open("w") as f:
            f.write("P3\n16 16\n255\n")
            for y in range(16):
                for x in range(16):
                    noise=((x*23+y*17+x*y*7)%19)-9
                    if kind=="basecolor": rgb=[max(0,min(255,v+noise)) for v in base]
                    elif kind=="roughness":rgb=[min(255,200+noise)]*3
                    elif kind=="metallic":rgb=[255 if name=="oxidized_iron" else 0]*3
                    else:rgb=[128,128,255]
                    f.write(" ".join(map(str,rgb))+" ")
                f.write("\n")
manifest={"asset_id":"razlom.creature.modern.podezdnik","version":"0.1.0","epoch":2026,"category":"occult humanoid","name":"Подъездник","mesh":"SM_Podezdnik_2026_v01.obj","source":"generate_podezdnik.py","materials":"podezdnik.mtl","vertices":len(verts),"triangles":len(faces),"uv":True,"texture_format":"P3 PPM (draft; requires conversion/bake)","height_cm":278,"rig":"not authored","lod":"not authored","ue5_tested":False,"status":"materials_wip","license":"original project asset"}
(P/"asset_manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n")
print(json.dumps(manifest,ensure_ascii=False))
