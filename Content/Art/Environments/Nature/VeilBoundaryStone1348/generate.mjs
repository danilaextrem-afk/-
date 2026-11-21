// Reproducible generator source: execute with Node.js 18+
import fs from 'node:fs';
const generate=function generate(lod=0){
 const V=[],UV=[],F=[];let seed=1348;const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296};
 function vertex(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
 function tri(a,b,c,mat){F.push([a,b,c,mat])}
 function quad(a,b,c,d,m){tri(a,b,c,m);tri(a,c,d,m)}
 const n=lod?8:20,levels=lod?5:11, rings=[];
 for(let j=0;j<levels;j++){let z=j/(levels-1)*2.45, ring=[];for(let i=0;i<n;i++){let t=i*2*Math.PI/n;let radius=(.46-.11*z/2.45)*(1+.08*Math.sin(i*3.7+j*2.4)+.035*(rand()-.5));ring.push(vertex(Math.cos(t)*radius,Math.sin(t)*radius,z+(.025*rand()),i/n,j/(levels-1)))}rings.push(ring)}
 for(let j=0;j<levels-1;j++)for(let i=0;i<n;i++)quad(rings[j][i],rings[j][(i+1)%n],rings[j+1][(i+1)%n],rings[j+1][i],"stone");
 let centerBottom=vertex(0,0,0,.5,.5),centerTop=vertex(0,0,2.46,.5,.5);
 for(let i=0;i<n;i++){tri(centerBottom,rings[0][(i+1)%n],rings[0][i],"stone");tri(centerTop,rings[levels-1][i],rings[levels-1][(i+1)%n],"stone")}
 // engraved-looking raised angular veil rune on front face (front y=-.48), LOD0 only
 function beam(ax,az,bx,bz,w,depth,mat){let dx=bx-ax,dz=bz-az,len=Math.hypot(dx,dz),px=-dz/len*w/2,pz=dx/len*w/2;let y=-depth;let p=[[ax+px,y,az+pz],[ax-px,y,az-pz],[bx-px,y,bz-pz],[bx+px,y,bz+pz]];let q=p.map(v=>[v[0],v[1]+.045,v[2]]);let a=p.map((v,i)=>vertex(...v,i%2,Math.floor(i/2))),b=q.map((v,i)=>vertex(...v,i%2,Math.floor(i/2)));quad(a[0],a[1],a[2],a[3],mat);for(let k=0;k<4;k++)quad(a[k],b[k],b[(k+1)%4],a[(k+1)%4],mat)}
 if(!lod){for(let k=0;k<7;k++){let a=k*2*Math.PI/7,b=(k+1)*2*Math.PI/7;beam(.21*Math.cos(a),1.4+.21*Math.sin(a),.21*Math.cos(b),1.4+.21*Math.sin(b),.035,.44,"rune")}beam(0,1.11,0,1.69,.05,.45,"rune");beam(-.19,1.41,.19,1.41,.04,.45,"rune")}
 const lines=["# RZ_NATURE_VEIL_BOUNDARY_STONE_1348 v0.1","o VeilBoundaryStone1348",...V.map(v=>"v "+v.map(x=>x.toFixed(6)).join(" ")),...UV.map(v=>"vt "+v.map(x=>x.toFixed(6)).join(" ")),"mtllib BoundaryStone.mtl"];let last="";
 for(const [a,b,c,m] of F){if(m!==last){lines.push("usemtl "+m);last=m}lines.push("f "+[a,b,c].map(i=>`${i}/${i}`).join(" "))}
 return {obj:lines.join("\n")+"\n",vertices:V.length,triangles:F.length,faces:F,uv:UV};
};
const ppm=function ppm(kind){let lines=["P3","32 32","255"];for(let y=0;y<32;y++){let row=[];for(let x=0;x<32;x++){let noise=((x*73+y*151+(x*y*19))%41)-20;let c=kind==="base"?[100+noise,103+noise,98+noise]:kind==="rough"?[190+noise,190+noise,190+noise]:[0,0,0];row.push(c.map(v=>Math.max(0,Math.min(255,v))).join(" "))}lines.push(row.join(" "))}return lines.join("\n")+"\n"};
const hi=generate(0),lo=generate(1);
fs.writeFileSync('BoundaryStone_LOD0.obj',hi.obj);fs.writeFileSync('BoundaryStone_LOD1.obj',lo.obj);
for(const [k,n] of [['base','BaseColor'],['rough','Roughness'],['metal','Metallic']])fs.writeFileSync('Stone_'+n+'.ppm',ppm(k));
console.log(JSON.stringify({lod0:[hi.vertices,hi.triangles],lod1:[lo.vertices,lo.triangles]}));
