// Original procedural wheel. Node.js 18+; run: node generate.cjs
const fs=require('fs');
const make=(N)=>{
let V=[],UV=[],F=[],M=[];const add=(x,y,z,u,v)=>{V.push([x,y,z]);UV.push([u,v]);return V.length};const tri=(a,b,c,m)=>{F.push([a,b,c]);M.push(m)};
function tube(points,r,mat){let rings=[];for(let j=0;j<points.length;j++){let p=points[j],q=points[Math.min(j+1,points.length-1)],t=[q[0]-p[0],q[1]-p[1],q[2]-p[2]];if(j===points.length-1)t=[p[0]-points[j-1][0],p[1]-points[j-1][1],p[2]-points[j-1][2]];let len=Math.hypot(...t)||1;t=t.map(x=>x/len);let a=[-t[1],t[0],0],al=Math.hypot(...a)||1;a=a.map(x=>x/al);let b=[t[1]*a[2]-t[2]*a[1],t[2]*a[0]-t[0]*a[2],t[0]*a[1]-t[1]*a[0]];let ring=[];for(let k=0;k<8;k++){let ang=2*Math.PI*k/8;ring.push(add(p[0]+r*(a[0]*Math.cos(ang)+b[0]*Math.sin(ang)),p[1]+r*(a[1]*Math.cos(ang)+b[1]*Math.sin(ang)),p[2]+r*(a[2]*Math.cos(ang)+b[2]*Math.sin(ang)),k/8,j/(points.length-1)))}rings.push(ring)}
for(let j=0;j<rings.length-1;j++)for(let k=0;k<8;k++){let a=rings[j][k],b=rings[j][(k+1)%8],c=rings[j+1][k],d=rings[j+1][(k+1)%8];tri(a,b,c,mat);tri(b,d,c,mat)}for(let k=1;k<7;k++){tri(rings[0][0],rings[0][k+1],rings[0][k],mat);let last=rings[rings.length-1];tri(last[0],last[k],last[k+1],mat)}}
let pts=[];for(let i=0;i<=N;i++){let a=i*2*Math.PI/N;pts.push([0,0.62*Math.cos(a),0.62*Math.sin(a)])}tube(pts,0.085,"IronRim");
for(let i=0;i<10;i++){let a=i*2*Math.PI/10;tube([[0,0.0,0.0],[0,0.27*Math.cos(a),0.27*Math.sin(a)],[0,0.56*Math.cos(a),0.56*Math.sin(a)]],0.044,"OakSpokes")}
tube([[-0.17,0,0],[0.17,0,0]],0.125,"OakHub");
let obj="# Original procedural VelegRad wagon wheel; meters, X axle\nmtllib materials.mtl\n";for(const v of V)obj+="v "+v.map(x=>x.toFixed(6)).join(" ")+"\n";for(const uv of UV)obj+="vt "+uv.map(x=>x.toFixed(6)).join(" ")+"\n";let last="";for(let i=0;i<F.length;i++){if(M[i]!==last){last=M[i];obj+="usemtl "+last+"\n"}obj+="f "+F[i].map(x=>x+"/"+x).join(" ")+"\n"}let deg=0;for(let f of F){let [a,b,c]=f.map(i=>V[i-1]);let x=b.map((v,i)=>v-a[i]),y=c.map((v,i)=>v-a[i]);let cr=[x[1]*y[2]-x[2]*y[1],x[2]*y[0]-x[0]*y[2],x[0]*y[1]-x[1]*y[0]];if(Math.hypot(...cr)<1e-9)deg++}return {obj,stats:{vertices:V.length,triangles:F.length,degenerate:deg,uv:UV.length}}};
fs.writeFileSync('LOD0.obj',make(32).obj);fs.writeFileSync('LOD1.obj',make(16).obj);
console.log(JSON.stringify([make(32).stats,make(16).stats]));
