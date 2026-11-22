// Ratibor forearm bracer: deterministic source; no third-party assets.
// Run: node generate.cjs (writes two OBJ files)
const fs=require('node:fs');
function generate(lod){
const V=[],F=[],UV=[],M=[];let seg=lod?12:32,rows=lod?5:13;
function vert(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
function face(a,b,c,mat){F.push([a,b,c]);M.push(mat)}
function quad(a,b,c,d,mat){face(a,b,c,mat);face(a,c,d,mat)}
function strip(radius,offset,material,phase=0){
const start=V.length;
for(let j=0;j<=rows;j++){let t=j/rows,y=-.17+t*.34,r=(.091-t*.024)+radius;for(let i=0;i<=seg;i++){let a=(i/seg)*Math.PI*1.86-.93*Math.PI+phase;let ripple=(lod?0:.0015*Math.sin(a*11+y*43));vert((r+ripple)*Math.cos(a),y,(r+ripple)*Math.sin(a)+offset,i/seg,t)}}
for(let j=0;j<rows;j++)for(let i=0;i<seg;i++){let a=start+j*(seg+1)+i+1,b=a+1,c=a+seg+2,d=a+seg+1;quad(a,b,c,d,material)}
}
strip(0,0,"leather");
if(!lod){strip(.003,0,"stitched_leather",.02);}
// iron bands along ends with curved strips
function band(y0,width,mat){let n=lod?12:32,st=V.length;for(let j=0;j<2;j++)for(let i=0;i<=n;i++){let a=i/n*Math.PI*1.86-.93*Math.PI;let r=.093-((y0+.17)/.34)*.024+.005;vert(r*Math.cos(a),y0+j*width,r*Math.sin(a),i/n,j)}for(let i=0;i<n;i++){let a=st+i+1;quad(a,a+1,a+n+2,a+n+1,mat)}}
band(-.15,.018,"dark_iron");band(.128,.018,"dark_iron");
function sphere(cx,cy,cz,r,mat){let n=lod?6:10,k=lod?4:6,st=V.length;for(let j=0;j<=k;j++)for(let i=0;i<=n;i++){let a=i/n*Math.PI*2,b=j/k*Math.PI;vert(cx+r*Math.cos(a)*Math.sin(b),cy+r*Math.cos(b),cz+r*Math.sin(a)*Math.sin(b),i/n,j/k)}for(let j=0;j<k;j++)for(let i=0;i<n;i++){let a=st+j*(n+1)+i+1;quad(a,a+1,a+n+2,a+n+1,mat)}}
for(let y of [-.141,.137])for(let a of [-1.6,-.8,0,.8,1.6]){let r=.092-((y+.17)/.34)*.024+.007;sphere(r*Math.cos(a),y,r*Math.sin(a),.004,"bronze")}
return {V,F,UV,M};
}
function obj(mesh,mtl){let s=`# Original Ratibor leather forearm bracer 1348\nmtllib bracer.mtl\no RatiborBracer_${mtl}\n`;mesh.V.forEach(v=>s+=`v ${v.map(x=>x.toFixed(6)).join(" ")}\n`);mesh.UV.forEach(v=>s+=`vt ${v.map(x=>x.toFixed(6)).join(" ")}\n`);let last="";mesh.F.forEach((f,i)=>{if(mesh.M[i]!==last){last=mesh.M[i];s+=`usemtl ${last}\n`}s+=`f ${f.map(n=>`${n}/${n}`).join(" ")}\n`});return s}
fs.writeFileSync('bracer_LOD0.obj',obj(generate(0),'LOD0'));
fs.writeFileSync('bracer_LOD1.obj',obj(generate(1),'LOD1'));
