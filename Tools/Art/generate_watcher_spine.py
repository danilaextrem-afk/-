"""Original procedural articulated dorsal reliquary for Ninth Entrance Watcher.
Units metres, Z up. Disconnected components intended for later attachment.
"""
from pathlib import Path
V=[]; F=[]; groups=[]
def vertex(p):
    V.append(tuple(round(float(x),6) for x in p));return len(V)
def box(name,center,half):
    cx,cy,cz=center;hx,hy,hz=half
    ids=[vertex((cx+sx*hx,cy+sy*hy,cz+sz*hz)) for sz in (-1,1) for sy in (-1,1) for sx in (-1,1)]
    groups.append((name,len(F)))
    for q in [(0,2,3,1),(4,5,7,6),(0,1,5,4),(2,6,7,3),(0,4,6,2),(1,3,7,5)]:
        a,b,c,d=(ids[i] for i in q);F.extend(((a,b,c),(a,c,d)))
def tapered(name,z,outer,inner,depth):
    y=depth
    a=vertex((-outer,y,z));b=vertex((outer,y,z));c=vertex((inner,y,z+.115));d=vertex((-inner,y,z+.115))
    e=vertex((-outer,y-.035,z));f=vertex((outer,y-.035,z));g=vertex((inner,y-.035,z+.115));h=vertex((-inner,y-.035,z+.115))
    groups.append((name,len(F)))
    for q in [(a,b,c,d),(e,h,g,f),(a,e,f,b),(d,c,g,h),(a,d,h,e),(b,f,g,c)]:
        x1,x2,x3,x4=q;F.extend(((x1,x2,x3),(x1,x3,x4)))
for i in range(7):
    z=1.05+i*.115;width=.16-abs(i-3)*.014
    tapered(f'Rib_{i+1:02d}',z,width,.024,-.165)
    box(f'Rivet_L_{i+1:02d}',(-width+.018,-.133,z+.024),(.012,.012,.012))
    box(f'Rivet_R_{i+1:02d}',(width-.018,-.133,z+.024),(.012,.012,.012))
for i in range(6):box(f'Spine_Link_{i+1:02d}',(0,-.19,1.157+i*.115),(.026,.023,.052))
box('Casket_Core',(0,-.22,1.89),(.10,.055,.12))
for s in (-1,1):
    box(f'Casket_Side_{s}',(s*.09,-.225,1.89),(.015,.065,.145))
    box(f'Chain_Anchor_{s}',(s*.085,-.18,1.75),(.018,.02,.035))
for i in range(3):box(f'Suspended_Talisman_{i+1}',((i-1)*.068,-.19,1.015-i*.055),(.024,.013,.035))
def write(path):
    lines=['# RAZLOM ORIGINAL: Watcher dorsal spine reliquary v1','# metre units; Z-up; disconnected rigid parts; UV/PBR pending','o SM_Watcher_DorsalReliquary_v1']
    for x,y,z in V:lines.append(f'v {x:.6f} {y:.6f} {z:.6f}')
    for j,(name,start) in enumerate(groups):
        lines.append('g '+name)
        end=groups[j+1][1] if j+1<len(groups) else len(F)
        for a,b,c in F[start:end]:lines.append(f'f {a} {b} {c}')
    Path(path).write_text('\n'.join(lines)+'\n')
if __name__=='__main__':
    write(Path(__file__).resolve().parents[2]/'Content/Art/Characters/Watcher/SM_Watcher_DorsalReliquary_v1.obj')
    print('vertices',len(V),'triangles',len(F),'parts',len(groups))
