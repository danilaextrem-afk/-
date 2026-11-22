"""Generate original nine-entrance Watcher ritual face mask (metres, Y-forward)."""
import math
from pathlib import Path

def build():
    v=[]; f=[]
    def vertex(x,y,z):
        v.append((x,y,z)); return len(v)
    def quad(a,b,c,d):
        f.append((a,b,c)); f.append((a,c,d))
    # Closed radial shell, nine angular panels and 17 vertical rings.
    rings=[]
    for j in range(17):
        z=.12+j*.026
        row=[]
        for i in range(72):
            t=2*math.pi*i/72
            x=.145*math.cos(t)*(1-.22*abs(z-.32))
            y=.085*math.sin(t)+.018*math.cos(3*t)+.035*math.sin(math.pi*j/16)
            row.append(vertex(x,y,z))
        rings.append(row)
    for j in range(16):
        for i in range(72):
            k=(i+1)%72
            quad(rings[j][i],rings[j][k],rings[j+1][k],rings[j+1][i])
    lo=vertex(0,0,.12); hi=vertex(0,0,.536)
    for i in range(72):
        k=(i+1)%72
        f.append((lo,rings[0][k],rings[0][i]))
        f.append((hi,rings[-1][i],rings[-1][k]))
    # Nine raised ritual ridges, independent decorative closed triangular strips.
    for spoke in range(9):
        t=2*math.pi*spoke/9
        for j in range(12):
            z=.17+j*.027
            x=.15*math.cos(t); y=.09*math.sin(t)+.03
            a=vertex(x-.006,y,z); b=vertex(x+.006,y,z)
            c=vertex(x,y+.012,z+.026)
            f.append((a,b,c))
    # Asymmetric eye sockets: two recessed-looking, dark inset surfaces.
    for cx in (-.071,.071):
        rim=[]
        for i in range(20):
            t=2*math.pi*i/20
            rim.append(vertex(cx+.038*math.cos(t),-.083,.388+.022*math.sin(t)))
        pupil=vertex(cx,-.09,.388)
        for i in range(20):
            f.append((pupil,rim[i],rim[(i+1)%20]))
    # Vertical nose blade, nine-entrance seal; independent geometry.
    a=vertex(0,-.108,.374); b=vertex(-.019,-.08,.26)
    c=vertex(.019,-.08,.26); d=vertex(0,-.137,.292)
    f.extend(((a,b,d),(a,d,c),(b,c,d)))
    return v,f

def obj_text():
    v,f=build()
    out=["# RAZLOM ORIGINAL: Ninth Entrance Watcher ritual face mask v1",
         "# Units: metres; +Z up; proxy visual prop, NOT a skinned character",
         "o SM_NinthEntranceWatcher_Mask_v1","s off"]
    out += ["v %.6f %.6f %.6f"%p for p in v]
    out += ["f %d %d %d"%p for p in f]
    return "\n".join(out)+"\n"

if __name__=="__main__":
    path=Path(__file__).resolve().parents[2]/"Content/Art/Characters/Watcher/SM_NinthEntranceWatcher_Mask_v1.obj"
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(obj_text(),encoding="utf-8")
    print(path)
