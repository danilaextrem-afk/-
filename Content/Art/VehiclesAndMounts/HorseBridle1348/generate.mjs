// Original procedural horse bridle, Razlom 1348. Run: node generate.mjs
import fs from 'node:fs';
const generate=function generate(segments=12){
 const V=[],UV=[],F=[],groups={}; let mat="Leather";
 const v=(x,y,z,u,w)=>{V.push([x,y,z]);UV.push([u,w]);return V.length};
 const tri=(a,b,c)=>{(groups[mat]??=[]).push([a,b,c])};
 function strap(points,width,thick){for(let j=0;j<points.length-1;j++){let p=points[j],q=points[j+1],dx=q[0]-p[0],dy=q[1]-p[1],dz=q[2]-p[2],len=Math.hypot(dx,dy,dz)||1;let nx=-dy/len,ny=dx/len;let corners=[];for(let t of [0,1])for(let s of [-1,1])for(let h of [-1,1]){let a=t?q:p;corners.push(v(a[0]+nx*s*width/2,a[1]+ny*s*width/2,a[2]+h*thick/2,t,s>0?1:0))}let faces=[[0,2,6,4],[1,5,7,3],[0,4,5,1],[2,3,7,6],[0,1,3,2],[4,6,7,5]];for(let [a,b,c,d] of faces){tri(corners[a],corners[b],corners[c]);tri(corners[a],corners[c],corners[d])}}}
 function torus(cx,cy,cz,R,r,n){let start=V.length;for(let i=0;i<n;i++)for(let j=0;j<6;j++){let a=i*2*Math.PI/n,b=j*2*Math.PI/6;v(cx+(R+r*Math.cos(b))*Math.cos(a),cy+(R+r*Math.cos(b))*Math.sin(a),cz+r*Math.sin(b),i/n,j/6)}for(let i=0;i<n;i++)for(let j=0;j<6;j++){let a=start+i*6+j+1,b=start+((i+1)%n)*6+j+1,c=start+((i+1)%n)*6+(j+1)%6+1,d=start+i*6+(j+1)%6+1;tri(a,b,c);tri(a,c,d)}}
 // Horse head reference axes: X forward, Y lateral, Z vertical. Bridle without animal mesh.
 strap([[-.58,-.23,1.52],[-.34,-.29,1.72],[.02,-.30,1.83],[.35,-.25,1.71]],.055,.016);
 strap([[-.58,.23,1.52],[-.34,.29,1.72],[.02,.30,1.83],[.35,.25,1.71]],.055,.016);
 strap([[-.28,-.29,1.7],[-.25,-.35,1.45],[-.15,-.32,1.24]],.046,.014);
 strap([[-.28,.29,1.7],[-.25,.35,1.45],[-.15,.32,1.24]],.046,.014);
 strap([[.34,-.25,1.7],[.42,-.23,1.42],[.47,-.18,1.23]],.055,.016);
 strap([[.34,.25,1.7],[.42,.23,1.42],[.47,.18,1.23]],.055,.016);
 strap([[.47,-.18,1.23],[.58,-.12,1.16],[.68,0,1.18],[.58,.12,1.16],[.47,.18,1.23]],.06,.017);
 mat="Iron";torus(.46,-.19,1.23,.055,.012,segments);torus(.46,.19,1.23,.055,.012,segments);
 torus(-.28,-.29,1.7,.034,.009,segments);torus(-.28,.29,1.7,.034,.009,segments);
 mat="Bronze";torus(.02,-.30,1.83,.027,.009,segments);torus(.02,.30,1.83,.027,.009,segments);
 const header=["# Original 1348 horse bridle, coordinates meters, Z-up","# UV0 procedural; MTL slots Leather/Iron/Bronze","mtllib bridle.mtl"];
 let lines=[...header,...V.map(p=>"v "+p.map(x=>x.toFixed(6)).join(" ")),...UV.map(p=>"vt "+p.map(x=>x.toFixed(6)).join(" "))];
 for(const [m,faces] of Object.entries(groups)){lines.push("usemtl "+m);for(const f of faces)lines.push("f "+f.map(i=>i+"/"+i).join(" "))}
 return {obj:lines.join("\n")+"\n",vertices:V.length,triangles:Object.values(groups).reduce((s,a)=>s+a.length,0),uv:UV.length,materials:Object.keys(groups)};
};
fs.writeFileSync('bridle_LOD0.obj',generate(20).obj);
fs.writeFileSync('bridle_LOD1.obj',generate(8).obj);
console.log(JSON.stringify({LOD0:generate(20).triangles,LOD1:generate(8).triangles}));
