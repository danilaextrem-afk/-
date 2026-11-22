// Reproducible procedural asset generator. Node.js >=18.
// Run: node generate.cjs ; outputs two OBJ files.
const fs=require('node:fs');
const buildMesh=function buildMesh(detail){
 const parts=[]; const materials=["blackened_iron","oak","linen","soot"];
 const verts=[],uvs=[],normals=[],faces=[]; let current="";
 function tri(a,b,c,mat){for(const p of [a,b,c]){verts.push(p.p);uvs.push(p.uv);normals.push(p.n)} faces.push({start:verts.length-2,mat});}
 function quad(a,b,c,d,mat){tri(a,b,c,mat);tri(a,c,d,mat)}
 function cylinder(name,center,r0,r1,length,axis,segments,mat){
  const [cx,cy,cz]=center; const rings=[];
  for(let k=0;k<2;k++){const arr=[]; for(let i=0;i<segments;i++){let t=2*Math.PI*i/segments,rr=k?r1:r0,along=(k-.5)*length;let p=axis==="z"?[cx+rr*Math.cos(t),cy+rr*Math.sin(t),cz+along]:axis==="x"?[cx+along,cy+rr*Math.cos(t),cz+rr*Math.sin(t)]:[cx+rr*Math.cos(t),cy+along,cz+rr*Math.sin(t)];arr.push(p)}rings.push(arr)}
  function vertex(p,u,v,n){return {p,uv:[u,v],n}}
  for(let i=0;i<segments;i++){const j=(i+1)%segments;let t=(i+.5)*Math.PI*2/segments,n=axis==="z"?[Math.cos(t),Math.sin(t),0]:axis==="x"?[0,Math.cos(t),Math.sin(t)]:[Math.cos(t),0,Math.sin(t)];quad(vertex(rings[0][i],i/segments,0,n),vertex(rings[0][j],(i+1)/segments,0,n),vertex(rings[1][j],(i+1)/segments,1,n),vertex(rings[1][i],i/segments,1,n),mat);
  const n0=axis==="z"?[0,0,-1]:axis==="x"?[-1,0,0]:[0,-1,0],n1=n0.map(x=>-x);
  tri(vertex(center.map((x,k)=>x+(k===(axis==="z"?2:axis==="x"?0:1)?-length/2:0)),.5,.5,n0),vertex(rings[0][j],.5+.5*Math.cos(2*Math.PI*j/segments),.5+.5*Math.sin(2*Math.PI*j/segments),n0),vertex(rings[0][i],.5+.5*Math.cos(2*Math.PI*i/segments),.5+.5*Math.sin(2*Math.PI*i/segments),n0),mat);
  tri(vertex(center.map((x,k)=>x+(k===(axis==="z"?2:axis==="x"?0:1)?length/2:0)),.5,.5,n1),vertex(rings[1][i],.5+.5*Math.cos(2*Math.PI*i/segments),.5+.5*Math.sin(2*Math.PI*i/segments),n1),vertex(rings[1][j],.5+.5*Math.cos(2*Math.PI*j/segments),.5+.5*Math.sin(2*Math.PI*j/segments),n1),mat);
  }
 }
 function box(name,c,size,mat){const [x,y,z]=c,[a,b,d]=size.map(v=>v/2);const corners=[[x-a,y-b,z-d],[x+a,y-b,z-d],[x+a,y+b,z-d],[x-a,y+b,z-d],[x-a,y-b,z+d],[x+a,y-b,z+d],[x+a,y+b,z+d],[x-a,y+b,z+d]];for(const [ids,n] of [[[0,3,2,1],[0,0,-1]],[[4,5,6,7],[0,0,1]],[[0,1,5,4],[0,-1,0]],[[1,2,6,5],[1,0,0]],[[2,3,7,6],[0,1,0]],[[3,0,4,7],[-1,0,0]]]){let p=ids.map((i,k)=>({p:corners[i],uv:[[0,0],[1,0],[1,1],[0,1]][k],n}));quad(...p,mat)}}
 const seg=detail?16:8;
 box("backplate",[0,0,0.55],[0.24,0.065,0.76],"blackened_iron");
 for(let z of [0.24,0.85]) cylinder("rivet",[0,-0.049,z],.047,.047,.04,"y",seg,"blackened_iron");
 box("bracket_arm",[0,-.36,.59],[.075,.67,.08],"blackened_iron");
 box("diagonal_brace",[0,-.27,.37],[.065,.41,.055],"blackened_iron");
 cylinder("socket",[0,-.72,.67],.115,.125,.24,"z",seg,"blackened_iron");
 cylinder("torch_oak_core",[0,-.72,1.03],.062,.045,.84,"z",seg,"oak");
 cylinder("iron_lower_collar",[0,-.72,.78],.09,.09,.10,"z",seg,"blackened_iron");
 cylinder("iron_upper_collar",[0,-.72,1.31],.084,.084,.07,"z",seg,"blackened_iron");
 cylinder("linen_wrapping",[0,-.72,1.48],.135,.17,.31,"z",seg,"linen");
 cylinder("charred_top",[0,-.72,1.65],.14,.095,.08,"z",seg,"soot");
 if(detail){for(let i=0;i<6;i++){let a=i*Math.PI/3;cylinder("binding_rivet",[.1*Math.cos(a),-.72+.1*Math.sin(a),1.29],.017,.017,.034,"z",8,"blackened_iron")}}
 let out="# RAZLOM original procedural wall torch, units meters, Z up; not UE5 validated\nmtllib materials.mtl\n";
 let last="";
 for(let i=0;i<verts.length;i++){const p=verts[i],uv=uvs[i],n=normals[i];out+=`v ${p.map(v=>v.toFixed(6)).join(" ")}\nvt ${uv.map(v=>v.toFixed(6)).join(" ")}\nvn ${n.map(v=>v.toFixed(6)).join(" ")}\n`}
 for(let i=0;i<faces.length;i++){const f=faces[i];if(f.mat!==last){out+=`usemtl ${f.mat}\n`;last=f.mat}let j=f.start;out+=`f ${j}/${j}/${j} ${j+1}/${j+1}/${j+1} ${j+2}/${j+2}/${j+2}\n`}
 return {obj:out,triangles:faces.length,vertices:verts.length,uvs:uvs.length,valid:faces.every(f=>f.start>=1&&f.start+2<=verts.length)&&verts.every(v=>v.every(Number.isFinite))};
};
fs.writeFileSync('LOD0.obj',buildMesh(true).obj);fs.writeFileSync('LOD1.obj',buildMesh(false).obj);
