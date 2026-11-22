"""Deterministic original static censer OBJ generator (metres, Z-up)."""
from pathlib import Path
import math
OUT=Path(__file__).resolve().parents[2]/"Content/Art/Characters/Watcher/SM_Watcher_RitualCenser_v1.obj"
def build():
 v=[]; f=[]
 def vertex(x,y,z):
  v.append((x,y,z));return len(v)
 def tri(a,b,c,g):f.append((a,b,c,g))
 def ring(name,profile,n=24):
  rings=[[vertex(r*math.cos(2*math.pi*j/n),r*math.sin(2*math.pi*j/n),z) for j in range(n)] for r,z in profile]
  for a,b in zip(rings,rings[1:]):
   for j in range(n):
    k=(j+1)%n;tri(a[j],a[k],b[k],name);tri(a[j],b[k],b[j],name)
 ring("Iron_Cage",[(.04,0),(.13,.025),(.18,.09),(.205,.17),(.20,.27),(.155,.355),(.095,.41),(.07,.44)])
 ring("Bronze_Rim",[(.204,.18),(.215,.18),(.215,.20),(.204,.20)])
 ring("Bronze_Rim",[(.151,.35),(.164,.35),(.164,.366),(.151,.366)])
 for j in range(9):
  a=2*math.pi*j/9;old=None
  for r,z in [(.133,.05),(.19,.15),(.2,.27),(.15,.35)]:
   cx,cy=r*math.cos(a),r*math.sin(a);t=(-math.sin(a),math.cos(a));n=(math.cos(a),math.sin(a))
   cur=[vertex(cx+t[0]*sx*.013+n[0]*sy*.009,cy+t[1]*sx*.013+n[1]*sy*.009,z) for sx,sy in [(-1,-1),(1,-1),(1,1),(-1,1)]]
   if old:
    for k in range(4):
     q=(k+1)%4;tri(old[k],old[q],cur[q],"Nine_Forged_Ribs");tri(old[k],cur[q],cur[k],"Nine_Forged_Ribs")
   old=cur
  def p(r,z,offset):return vertex(r*math.cos(a+offset),r*math.sin(a+offset),z)
  A=p(.13,.366,-.18);B=p(.13,.366,.18);C=p(.065,.428,.09);D=p(.065,.428,-.09)
  tri(A,B,C,"Vent_Vanes");tri(A,C,D,"Vent_Vanes")
 ring("Suspension_Neck",[(.068,.43),(.068,.47),(.04,.49),(.04,.52)])
 def torus(name,major,minor,center,axis="z",n=32,m=8):
  rr=[]
  for j in range(n):
   a=2*math.pi*j/n; row=[]
   for k in range(m):
    b=2*math.pi*k/m;r=major+minor*math.cos(b)
    if axis=="y":x,y,z=r*math.cos(a),minor*math.sin(b),r*math.sin(a)
    else:x,y,z=r*math.cos(a),r*math.sin(a),minor*math.sin(b)
    row.append(vertex(x+center[0],y+center[1],z+center[2]))
   rr.append(row)
  for j in range(n):
   for k in range(m):
    j2=(j+1)%n;k2=(k+1)%m
    tri(rr[j][k],rr[j2][k],rr[j2][k2],name);tri(rr[j][k],rr[j2][k2],rr[j][k2],name)
 torus("Suspension_Hoop",.077,.013,(0,0,.57),"y")
 for j in range(9):
  a=2*math.pi*j/9;torus("Rivets",.009,.005,(.207*math.cos(a),.207*math.sin(a),.192),n=12,m=6)
 ring("Inner_Clapper",[(.003,.055),(.055,.12),(.06,.16),(.037,.2),(.012,.24)])
 return v,f
def serialize(v,f):
 lines=["# RAZLOM original Watcher censer; metres, Z-up, no UV"]
 lines.extend(f"v {x:.6f} {y:.6f} {z:.6f}" for x,y,z in v)
 last=None
 for a,b,c,g in f:
  if g!=last:lines.append("g "+g);last=g
  lines.append(f"f {a} {b} {c}")
 return "\n".join(lines)+"\n"
def main():
 v,f=build();OUT.parent.mkdir(parents=True,exist_ok=True);OUT.write_text(serialize(v,f),encoding="utf-8")
 print(len(v),len(f),OUT)
if __name__=="__main__":main()
