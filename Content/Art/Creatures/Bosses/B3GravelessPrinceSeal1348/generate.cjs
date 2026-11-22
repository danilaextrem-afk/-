// Original authored procedural generator. Run: node generate.cjs
const fs=require('node:fs');
function build(lod){
 const N=lod===0?48:20, M=lod===0?12:6, verts=[],faces=[],mats=[],uv=[];
 function vertex(x,y,z,u,v){verts.push([x,y,z]);uv.push([u,v]);return verts.length}
 function tri(a,b,c,mat){faces.push([a,b,c]);mats.push(mat)}
 function tube(points,r,material){const rings=[];for(let i=0;i<points.length;i++){let p=points[i],prev=points[Math.max(0,i-1)],next=points[Math.min(points.length-1,i+1)],dx=next[0]-prev[0],dy=next[1]-prev[1],len=Math.hypot(dx,dy)||1,nx=-dy/len,ny=dx/len;let ring=[];for(let j=0;j<M;j++){let a=2*Math.PI*j/M;ring.push(vertex(p[0]+r*Math.cos(a)*nx,p[1]+r*Math.cos(a)*ny,p[2]+r*Math.sin(a),i/(points.length-1),j/M))}rings.push(ring)}for(let i=0;i<rings.length-1;i++)for(let j=0;j<M;j++){let a=rings[i][j],b=rings[i][(j+1)%M],c=rings[i+1][j],d=rings[i+1][(j+1)%M];tri(a,b,c,material);tri(b,d,c,material)}}
 function ring(R,r,mat){let pts=[];for(let i=0;i<=N;i++){let a=i*2*Math.PI/N,noise=0.006*Math.sin(7*a)+0.003*Math.cos(13*a);pts.push([(R+noise)*Math.cos(a),(R+noise)*Math.sin(a),0.008*Math.sin(5*a)])}tube(pts,r,mat)}
 ring(.42,.055,"iron");ring(.31,.018,"bronze");
 for(let k=0;k<7;k++){let a=k*2*Math.PI/7;let p=[];for(let j=0;j<=8;j++){let t=j/8,R=.315+t*.115;p.push([R*Math.cos(a+.025*Math.sin(t*6+k)),R*Math.sin(a+.025*Math.sin(t*6+k)),.045+.02*Math.sin(t*Math.PI)])}tube(p,.014,"bronze")}
 for(let k=0;k<3;k++){let a=k*2*Math.PI/3+.2,pts=[];for(let j=0;j<=12;j++){let t=j/12,R=.08+.22*t;pts.push([R*Math.cos(a+.13*Math.sin(t*8)),R*Math.sin(a+.13*Math.sin(t*8)),.06+.025*Math.sin(t*9)])}tube(pts,.018,"iron")}
 // central inset black stone: curved concentric rings and a non-planar raised eye
 ring(.095,.035,"obsidian");
 for(let k=0;k<5;k++){let a=k*2*Math.PI/5,pts=[];for(let j=0;j<=5;j++){let t=j/5;pts.push([(.11+.12*t)*Math.cos(a+.12*t),(.11+.12*t)*Math.sin(a+.12*t),.035+.018*Math.sin(t*Math.PI)])}tube(pts,.009,"bronze")}
 let out="# Original procedural boss seal B3 | metres | Z up | UV0\nmtllib B3_Seal.mtl\n";
 for(let v of verts)out+="v "+v.map(x=>x.toFixed(6)).join(" ")+"\n";
 for(let v of uv)out+="vt "+v.map(x=>x.toFixed(6)).join(" ")+"\n";
 let last="";for(let i=0;i<faces.length;i++){if(mats[i]!==last){last=mats[i];out+="usemtl "+last+"\n"}out+="f "+faces[i].map(x=>x+"/"+x).join(" ")+"\n"}
 let deg=faces.filter(f=>{let a=verts[f[0]-1],b=verts[f[1]-1],c=verts[f[2]-1];let u=b.map((x,i)=>x-a[i]),v=c.map((x,i)=>x-a[i]);return Math.hypot(u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0])<1e-10}).length;
 return {obj:out,stats:{vertices:verts.length,triangles:faces.length,degenerate:deg,uv:uv.length,bounds:[Math.min(...verts.map(v=>v[0])),Math.max(...verts.map(v=>v[0]))]}}
}
for(const lod of [0,1]){const {obj,stats}=build(lod);fs.writeFileSync('B3_Seal_LOD'+lod+'.obj',obj);console.log(lod,stats)}
