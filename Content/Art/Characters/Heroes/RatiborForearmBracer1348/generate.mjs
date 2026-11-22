// Original procedural asset generator. Run: node generate.mjs
import {writeFileSync} from 'node:fs';
const generate=function generate(detail=1){
 const V=[],UV=[],F=[],groups=[];let mat="leather";
 function vert(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
 function tri(a,b,c){F.push([a,b,c,mat])}
 function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
 function ring(y,rx,rz,n,phase=0){let out=[];for(let i=0;i<n;i++){let t=2*Math.PI*i/n+phase;out.push(vert(rx*Math.cos(t),y,rz*Math.sin(t),i/n,(y+.18)/.34))}return out}
 const n=detail?48:20,ys=detail?12:5;let prev=null;
 for(let j=0;j<=ys;j++){let y=-.16+.32*j/ys,r=.063+.024*(j/ys),r0=ring(y,r,r*.88,n);if(prev)for(let i=0;i<n;i++)quad(prev[i],prev[(i+1)%n],r0[(i+1)%n],r0[i]);prev=r0}
 mat="leather_edge";for(const y of [-.16,.16]){const a=ring(y,.063+(y>0?.024:0),(.063+(y>0?.024:0))*.88,n),b=ring(y+(y>0?.006:-.006),.066+(y>0?.024:0),(.066+(y>0?.024:0))*.88,n);for(let i=0;i<n;i++)quad(a[i],a[(i+1)%n],b[(i+1)%n],b[i])}
 mat="iron";for(const y of [-.105,.09]){const a=ring(y,.069+(y+.16)/.32*.024,.066+(y+.16)/.32*.018,n),b=ring(y+.015,.071+(y+.16)/.32*.024,.068+(y+.16)/.32*.018,n);for(let i=0;i<n;i++)quad(a[i],a[(i+1)%n],b[(i+1)%n],b[i])}
 mat="bronze";const rivN=detail?12:6;for(let k=0;k<rivN;k++){const y=-.115+.23*(k%6)/5,angle=(k<rivN/2?.6:2.55);const r=.07+.024*(y+.16)/.32;let cx=r*Math.cos(angle),cz=r*.88*Math.sin(angle),rr=.004;let top=vert(cx+rr*Math.cos(angle),y,cz+rr*Math.sin(angle),.5,1);let base=[];for(let i=0;i<8;i++){let t=i*Math.PI/4;base.push(vert(cx+rr*.2*Math.cos(angle)+rr*Math.cos(t)*(-Math.sin(angle)),y+rr*Math.sin(t),cz+rr*.2*Math.sin(angle)+rr*Math.cos(t)*Math.cos(angle),i/8,0))}for(let i=0;i<8;i++)tri(base[i],base[(i+1)%8],top)}
 let s="# Original Ratibor forearm bracer; meters; Y forearm axis\nmtllib materials.mtl\n";let last="";
 for(let i=0;i<V.length;i++)s+=`v ${V[i].map(v=>v.toFixed(6)).join(" ")}\n`;
 for(const uv of UV)s+=`vt ${uv.map(v=>v.toFixed(6)).join(" ")}\n`;
 for(const [a,b,c,m] of F){if(m!==last){s+=`usemtl ${m}\n`;last=m}s+=`f ${a}/${a} ${b}/${b} ${c}/${c}\n`}
 return {obj:s,vertices:V.length,triangles:F.length,uv:UV.length,valid:F.every(f=>f.slice(0,3).every(i=>i>0&&i<=V.length))};
};
writeFileSync('LOD0.obj',generate(1).obj);writeFileSync('LOD1.obj',generate(0).obj);
