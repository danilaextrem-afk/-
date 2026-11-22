// Reproducible original asset generator. Run: node generate.mjs
const fs=require('node:fs');
const build=function build(){
 const mats=["wood_dark","wood_endgrain","iron_worn","bronze_lock"];let V=[],T=[],F=[],N=[],groups=[], current="";
 function box(name,x,y,z,w,d,h,mat){let base=V.length, uv=T.length;let vs=[[0,0,0],[w,0,0],[w,d,0],[0,d,0],[0,0,h],[w,0,h],[w,d,h],[0,d,h]];for(let p of vs)V.push([x+p[0],y+p[1],z+p[2]]);let faces=[[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]];for(let q=0;q<6;q++){let a=faces[q],u=T.length;T.push([0,0],[1,0],[1,1],[0,1]);for(let tri of [[0,1,2],[0,2,3]])F.push({name,mat,v:tri.map(i=>base+a[i]+1),t:tri.map(i=>u+i+1)});}}
 // 0.95m chest, separately modeled planks, ironwork and functional lock.
 for(let i=0;i<5;i++){box("front_plank_"+i,-.48,-.30,.08+i*.105,.96,.035,.101,"wood_dark");box("back_plank_"+i,-.48,.265,.08+i*.105,.96,.035,.101,"wood_dark")}
 for(let i=0;i<4;i++){box("left_plank_"+i,-.48,-.265,.08+i*.132,.038,.53,.126,"wood_dark");box("right_plank_"+i,.442,-.265,.08+i*.132,.038,.53,.126,"wood_dark")}
 box("bottom",-.46,-.27,.07,.92,.54,.05,"wood_endgrain");
 for(let i=0;i<6;i++)box("lid_plank_"+i,-.48+i*.16,-.30,.63,.156,.60,.065,"wood_dark");
 for(let x of [-.39,.34]){box("front_iron_band_"+x,x,-.323,.06,.055,.015,.65,"iron_worn");box("lid_iron_band_"+x,x,-.32,.69,.055,.64,.018,"iron_worn");box("back_hinge_"+x,x,.296,.58,.06,.04,.15,"iron_worn")}
 box("lock_plate",-.10,-.346,.40,.20,.025,.20,"iron_worn");
 box("lock_keyhole_vertical",-.012,-.354,.46,.024,.012,.070,"bronze_lock");
 box("lock_keyhole_lower",-.032,-.354,.46,.064,.012,.020,"bronze_lock");
 for(let x of [-.41,.36])for(let z of [.13,.29,.45,.61])box("rivet_"+x+"_"+z,x,-.342,z,.018,.019,.018,"bronze_lock");
 return {V,T,F,mats};
};
const obj=function obj(model,low=false){let {V,T,F}=model;let out=["# RAZLOM original 3D geometry; meters; Z up","# Generated from generate.mjs","mtllib chest.mtl"];out.push(...V.map(v=>"v "+v.map(n=>n.toFixed(5)).join(" ")));out.push(...T.map(v=>"vt "+v.join(" ")));let last="";for(let f of F){if(low && (/rivet|keyhole|hinge/.test(f.name)))continue;if(f.mat!==last){out.push("usemtl "+f.mat);last=f.mat;}out.push("f "+f.v.map((v,i)=>v+"/"+f.t[i]).join(" "));}return out.join("\n")+"\n";};
const mesh=build();
fs.writeFileSync('Chest_LOD0.obj',obj(mesh,false));
fs.writeFileSync('Chest_LOD1.obj',obj(mesh,true));
console.log(JSON.stringify({vertices:mesh.V.length,uvs:mesh.T.length,triangles:mesh.F.length}));
