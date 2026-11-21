#!/usr/bin/env python3
"""Append original raised Gothic relief to the existing Black Bell v2 OBJ.

The resulting OBJ is a real, importable geometry change; not a generated 2D image.
Keeps the author's v2 MTL and texture set, no third-party assets. Work-in-progress.
"""
import argparse, json, math
from pathlib import Path
import numpy as np
TAU=2*math.pi

def radius(z):
    u=max(0,min(1,(z-18)/143))
    return 13+49.2*(1-u)**.65+1.4*math.sin(math.pi*u)+.6*math.sin(4*math.pi*u)
def shell(t,z,extra):
    r=radius(z)+extra
    return (r*math.cos(t),r*math.sin(t),z)

class Sculpt:
    def __init__(self,nv,nt,nn):
        self.nv=nv;self.nt=nt;self.nn=nn;self.pos=[];self.uv=[];self.nrm=[];self.faces=[]
    def vertex(self,p,normal,uv):
        self.pos.append(p);self.nrm.append(normal);self.uv.append(uv)
        return (self.nv+len(self.pos),self.nt+len(self.uv),self.nn+len(self.nrm))
    def triangle(self,a,b,c,mat): self.faces.append((a,b,c,mat))
    def tube(self,points,r,mat,sides=8):
        rings=[]
        for i,p in enumerate(points):
            xyz=np.array(p,dtype=float)
            prev=np.array(points[max(i-1,0)],dtype=float)
            nex=np.array(points[min(i+1,len(points)-1)],dtype=float)
            direction=nex-prev
            direction/=max(1e-9,np.linalg.norm(direction))
            ref=np.array((0,0,1.),dtype=float) if abs(direction[2])<.93 else np.array((1,0,0.),dtype=float)
            n=np.cross(direction,ref);n/=max(1e-9,np.linalg.norm(n));t=np.cross(direction,n)
            ring=[]
            for k in range(sides):
                vec=n*math.cos(TAU*k/sides)+t*math.sin(TAU*k/sides)
                ring.append(self.vertex(tuple(xyz+r*vec),tuple(vec),(k/sides,i/max(1,len(points)-1))))
            rings.append(ring)
        for i in range(len(rings)-1):
            for k in range(sides):
                a,b=rings[i][k],rings[i][(k+1)%sides]
                c,d=rings[i+1][(k+1)%sides],rings[i+1][k]
                self.triangle(a,b,c,mat);self.triangle(a,c,d,mat)
    def bead(self,p,rad,mat):
        for z in range(1,5):
            a=math.pi*z/5
            ring=[]
            for k in range(9):
                theta=TAU*k/9
                v=np.array([math.sin(a)*math.cos(theta),math.sin(a)*math.sin(theta),math.cos(a)])
                ring.append(self.vertex(tuple(np.array(p)+rad*v),tuple(v),(k/9,z/5)))
            if z>1:
                for k in range(9):
                    ka=(k+1)%9
                    self.triangle(old[k],old[ka],ring[ka],mat)
                    self.triangle(old[k],ring[ka],ring[k],mat)
            old=ring

def build(source,dest):
    text=source.read_text(encoding='utf-8')
    nv=sum(1 for line in text.splitlines() if line.startswith('v '))
    nt=sum(1 for line in text.splitlines() if line.startswith('vt '))
    nn=sum(1 for line in text.splitlines() if line.startswith('vn '))
    m=Sculpt(nv,nt,nn)
    for k in range(7):
        th=TAU*k/7+.24
        for left in (-1,1):
            pts=[]
            for j in range(33):
                q=j/32
                z=55+69*q
                ang=th+left*(.092*(1-q)**1.25)
                pts.append(shell(ang,z,2.4+1.1*math.sin(q*math.pi)))
            m.tube(pts,1.45,'age_gilt',9)
        m.bead(shell(th,124,3.0),3.2,'cyan_enamel')
        for j in (-2,-1,0,1,2):
            pts=[]
            for i in range(13):
                q=i/12
                z=124+21*q-3*abs(j)
                ang=th+j*.021+math.sin(q*math.pi)*j*.010
                pts.append(shell(ang,z,2.8+(1-q)*1.9))
            m.tube(pts,.38 if abs(j)==2 else .8,'golden_rim',7)
    for k in range(21):
        th=TAU*k/21+.11 + .006*math.sin(k*5.3)
        pts=[]
        for j in range(17):
            q=j/16
            pts.append(shell(th+(.015*math.sin(q*5+th))*(1-q),39+35*q,1.8+.7*q))
        m.tube(pts,.5 if k%3 else .95,'forged_iron',8)
        m.bead(shell(th,39,3),1.35,'age_gilt')
    for k in range(29):
        th=TAU*(k+.17)/29
        pts=[]
        for j in range(23):
            q=j/22
            zz=23+21*q+3*math.sin(th*11)
            tt=th+.026*q+.006*math.sin(q*24+th*3)
            pts.append(shell(tt,zz,1.35))
        m.tube(pts,.35,'black_patina',6)
    for k in range(64):
        th=TAU*k/64
        m.bead(shell(th,29,2.7),1.45,'golden_rim')
    for k in range(8):
        th=TAU*(k+.25)/8
        pts=[]
        for j in range(31):
            q=j/30
            z=127-29*q
            pts.append(shell(th+.025*math.sin(7*q),z,3+2*math.sin(math.pi*q)))
        m.tube(pts,1.0,'forged_iron',10)
    dest.parent.mkdir(parents=True,exist_ok=True)
    with dest.open('w',encoding='utf-8') as f:
        f.write(text)
        f.write('\n# original gothic chased-metal relief, geometry not 2D concept art\n')
        f.write('o SM_BlackBell_Hero_v3_GothicRelief\n')
        for p in m.pos: f.write('v '+' '.join(f'{n:.5f}' for n in p)+'\n')
        for uv in m.uv:f.write('vt %.5f %.5f\n'%uv)
        for normal in m.nrm:f.write('vn '+' '.join(f'{n:.5f}' for n in normal)+'\n')
        mat=None
        for a,b,c,material in m.faces:
            if mat!=material:mat=material;f.write('usemtl '+mat+'\n')
            f.write('f '+' '.join('%d/%d/%d'%v for v in (a,b,c))+'\n')
    info={'base':source.name,'asset':dest.name,'new_vertices':len(m.pos),'new_triangles':len(m.faces),'total_vertices':nv+len(m.pos),'status':'Original augmented OBJ: UE5 import, visual fidelity and AAA quality unverified'}
    (dest.with_suffix('.json')).write_text(json.dumps(info,indent=2)+'\n')
    print(json.dumps(info))
if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--source',type=Path,default=Path('Content/Art/HeroProps/SM_BlackBell_Hero_v2.obj'))
    p.add_argument('--out',type=Path,default=Path('Content/Art/HeroProps/SM_BlackBell_Hero_v3.obj'))
    a=p.parse_args();build(a.source,a.out)
