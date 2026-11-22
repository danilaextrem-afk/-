"""Reproducible procedural source for SM_AnomalyBeacon_v1.obj (metres, Z up)."""
from pathlib import Path
BOXES = [["concrete_foot",0,0,0.08,0.84,0.84,0.16,"concrete"],["steel_plinth",0,0,0.19,0.64,0.64,0.08,"steel"],["anchor",-0.25,-0.25,0.24,0.08,0.08,0.06,"rust"],["anchor",-0.25,0.25,0.24,0.08,0.08,0.06,"rust"],["anchor",0.25,-0.25,0.24,0.08,0.08,0.06,"rust"],["anchor",0.25,0.25,0.24,0.08,0.08,0.06,"rust"],["lower_column",0,0,0.47,0.28,0.28,0.48,"steel"],["waist_guard",0,0,0.75,0.38,0.38,0.09,"rust"],["mast",0,0,1.06,0.17,0.17,0.56,"steel"],["sensor_cage_lower",0,0,1.36,0.44,0.44,0.08,"rust"],["glow_core",0,0,1.54,0.22,0.22,0.31,"cyan"],["cage_bar",-0.19,-0.19,1.54,0.045,0.045,0.4,"steel"],["cage_bar",-0.19,0.19,1.54,0.045,0.045,0.4,"steel"],["cage_bar",0.19,-0.19,1.54,0.045,0.045,0.4,"steel"],["cage_bar",0.19,0.19,1.54,0.045,0.045,0.4,"steel"],["sensor_cage_upper",0,0,1.76,0.44,0.44,0.09,"rust"],["cap",0,0,1.84,0.31,0.31,0.09,"steel"],["antenna",0,0,2.02,0.035,0.035,0.3,"steel"],["antenna_tip",0,0,2.2,0.09,0.09,0.06,"cyan"],["console_housing",0,-0.22,0.88,0.32,0.15,0.24,"steel"],["console_screen",0,-0.302,0.89,0.23,0.012,0.13,"cyan"],["console_button",-0.085,-0.318,0.78,0.045,0.012,0.025,"rust"],["console_button",0,-0.318,0.78,0.045,0.012,0.025,"rust"],["console_button",0.085,-0.318,0.78,0.045,0.012,0.025,"rust"],["side_fin",-0.22,0,1.1,0.09,0.29,0.23,"rust"],["cable_vertical",-0.32,0,0.59,0.035,0.035,0.39,"dark"],["cable_elbow",-0.29,0,0.79,0.09,0.035,0.035,"dark"],["side_fin",0.22,0,1.1,0.09,0.29,0.23,"rust"],["cable_vertical",0.32,0,0.59,0.035,0.035,0.39,"dark"],["cable_elbow",0.29,0,0.79,0.09,0.035,0.035,"dark"]]
COLORS = {"concrete":[0.26,0.28,0.28],"steel":[0.16,0.22,0.23],"rust":[0.45,0.22,0.12],"cyan":[0.09,0.9,0.83],"dark":[0.06,0.07,0.08]}
def build(folder):
    folder=Path(folder);folder.mkdir(parents=True,exist_ok=True)
    out=["# RAZLOM ZONA original 3D asset: anomaly beacon","# metre units, Z up; each cuboid is a real closed mesh with UV0","mtllib SM_AnomalyBeacon_v1.mtl"]
    vi=ti=1
    corners=[(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]
    faces=((4,5,6,7),(1,0,3,2),(0,4,7,3),(5,1,2,6),(3,7,6,2),(0,1,5,4))
    for name,x,y,z,w,d,h,mat in BOXES:
        out.extend((f"o {name}_{vi}",f"usemtl {mat}"))
        for a,b,c in corners: out.append(f"v {x+a*w/2:.5f} {y+b*d/2:.5f} {z+c*h/2:.5f}")
        for face in faces:
            out.extend(("vt 0 0","vt 1 0","vt 1 1","vt 0 1"))
            out.append("f "+" ".join(f"{vi+a}/{ti+i}" for i,a in enumerate(face)))
            ti+=4
        vi+=8
    (folder/"SM_AnomalyBeacon_v1.obj").write_text("\n".join(out)+"\n")
    mats=["# Material previews only; colors are placeholders for Godot StandardMaterial3D"]
    for name,c in COLORS.items():
        mats.extend((f"newmtl {name}","Kd "+" ".join(map(str,c)),"Ka 0 0 0","Ks 0.05 0.05 0.05","Ns 25","illum 2",""))
    (folder/"SM_AnomalyBeacon_v1.mtl").write_text("\n".join(mats)+"\n")
if __name__=="__main__":
    build(Path(__file__).resolve().parents[2]/"Content/Art/Zona/Props/AnomalyBeacon")
