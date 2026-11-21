"""Reproducible geometric blockout for Ninth Entrance Watcher body; Z-up metres."""
import math
from pathlib import Path

vertices, faces = [], []
def v(x,y,z):
    vertices.append((x,y,z))
    return len(vertices)
def ring(cx,cy,z,rx,ry,n,phase=0):
    return [v(cx+rx*math.cos(2*math.pi*i/n+phase),
              cy+ry*math.sin(2*math.pi*i/n+phase),z) for i in range(n)]
def bridge(a,b):
    for i in range(len(a)):
        j=(i+1)%len(a)
        faces.extend(((a[i],a[j],b[j]),(a[i],b[j],b[i])))
def tube(points,r,n=10):
    prev=None
    for k,p in enumerate(points):
        radius=r[k] if isinstance(r,list) else r
        cur=ring(p[0],p[1],p[2],radius,radius,n)
        if prev: bridge(prev,cur)
        prev=cur
def build():
    torso=[ring(0,0,z,rx,ry,24) for z,rx,ry in
           ((.92,.33,.20),(1.18,.25,.17),(1.48,.31,.18),(1.72,.20,.15))]
    for i in range(1,len(torso)): bridge(torso[i-1],torso[i])
    hood=[ring(0,0,z,rx,ry,24) for z,rx,ry in
          ((1.66,.19,.16),(1.89,.24,.23),(2.08,.17,.17),(2.19,.07,.08))]
    for i in range(1,len(hood)): bridge(hood[i-1],hood[i])
    for side in (-1,1):
        x=side*.32
        tube(((x,0,1.58),(side*.42,0,1.42),(side*.45,-.04,1.16),(side*.43,-.07,.96)),[.135,.13,.11,.095])
        tube(((side*.15,0,.96),(side*.18,0,.68),(side*.19,-.015,.36),(side*.2,-.08,.12)),[.14,.13,.12,.11])
        tube(((side*.20,-.08,.12),(side*.21,-.15,.075)),[.11,.12])
    cloak=[]
    for k in range(7):
        z=1.65-k*.235
        radius=.29+k*.12
        cur=ring(0,.065,z,radius,radius*.78,32,.098)
        if cloak: bridge(cloak[-1],cur)
        cloak.append(cur)
    for side in (-1,1):
        for j in range(4):
            x=side*(.08+j*.075)
            tube(((x,-.205,1.65),(x*1.12,-.23,1.31),(x*1.22,-.31,.96)),.018,7)
def write(path):
    build()
    lines=["# RAZLOM ORIGINAL PROCEDURAL WATCHER BODY BLOCKOUT v1",
           "# units metres Z up; torso and cloak are separate surface shells; NOT production rig"]
    lines += ["v "+" ".join(f"{q:.5f}" for q in p) for p in vertices]
    lines += ["f "+" ".join(map(str,f)) for f in faces]
    Path(path).write_text("\n".join(lines)+"\n",encoding="utf-8")
if __name__=="__main__":
    write(Path(__file__).resolve().parents[2]/"Content/Art/Characters/Watcher/SM_NinthEntranceWatcher_BodyBlockout_v1.obj")
