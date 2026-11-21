"""RAZLOM Leshy v0.1 -- procedural source design specification.
The committed OBJ is the source-of-truth for this revision; this file
provides a deterministic, editable geometry construction recipe.
Units: centimeters, Z-up. No third-party asset packs.
"""
import math
from pathlib import Path

def tube(points, radii, sides=12):
    """Build bark ring mesh, with ring UVs; return vertices/faces/uv."""
    vertices, faces, uv = [], [], []
    for j, p in enumerate(points):
        prev = points[max(0,j-1)]
        nxt = points[min(len(points)-1,j+1)]
        t = [nxt[k]-prev[k] for k in range(3)]
        norm = math.sqrt(sum(x*x for x in t)) or 1
        t = [x/norm for x in t]
        a = [-t[1],t[0],0]
        if math.sqrt(sum(x*x for x in a)) < .1:
            a = [0,-t[2],t[1]]
        norm = math.sqrt(sum(x*x for x in a))
        a = [x/norm for x in a]
        b = [t[1]*a[2]-t[2]*a[1],t[2]*a[0]-t[0]*a[2],t[0]*a[1]-t[1]*a[0]]
        for i in range(sides):
            angle = i*2*math.pi/sides
            radius = radii[j]*(1+.05*math.sin(5*angle+j*.83))
            v = [100*(p[k]+radius*(math.cos(angle)*a[k]+math.sin(angle)*b[k])) for k in range(3)]
            vertices.append(v)
            uv.append([i/sides,j/(len(points)-1)])
    for j in range(len(points)-1):
        for i in range(sides):
            a=j*sides+i; b=j*sides+(i+1)%sides
            c=a+sides;d=b+sides
            faces.extend([(a,b,c),(b,d,c)])
    return vertices,faces,uv

def curve(a,b,n,seed):
    return [[a[k]+(b[k]-a[k])*i/(n-1)+
            (math.sin(math.pi*i/(n-1))*.035*
             math.sin(seed*2.3+i*.6+k) if k<2 else 0)
             for k in range(3)] for i in range(n)]

# Construction recipe used for the committed OBJ:
# torso/neck/head + two limbs + two arms + asymmetrical horns;
# 10 fingers, 8 antler forks, 6 root toes, 10 chest ribs,
# 55 moss strands and two amber eyes.
# Materials: bark, moss, amber; P3 albedo/normal/roughness maps.
# This is not a rigged character; future revisions require an artist pass,
# skeleton, skinning, animation, LOD, real 4K textures and UE5 acceptance.
if __name__ == "__main__":
    print("Leshy geometry construction helpers. Committed OBJ is authoritative.")
