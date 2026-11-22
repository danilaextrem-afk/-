// Original procedural geometry for RAZLOM Watcher flail chain. Node.js 18+.
const fs=require('node:fs');
const V=[],UV=[],F=[],groups=[];
function vertex(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length;}
function face(a,b,c){F.push([a,b,c]);}
function torus(name,cx,cy,cz,major=0.085,tube=0.018,axis=0){
 groups.push([name,F.length]);
 const N=24,M=12,idx=[];
 for(let i=0;i<N;i++){const row=[];const a=2*Math.PI*i/N;
 for(let j=0;j<M;j++){const b=2*Math.PI*j/M;
 const r=major+tube*Math.cos(b),p=r*Math.cos(a),q=r*Math.sin(a),t=tube*Math.sin(b);
 const xyz=axis===0?[cx+p,cy+q,cz+t]:[cx+t,cy+p,cz+q];
 row.push(vertex(...xyz,i/N,j/M));}idx.push(row);}
 for(let i=0;i<N;i++)for(let j=0;j<M;j++){const a=idx[i][j],b=idx[(i+1)%N][j],c=idx[(i+1)%N][(j+1)%M],d=idx[i][(j+1)%M];face(a,b,c);face(a,c,d);}
}
function cylinder(name,z0,z1,r,N=24){groups.push([name,F.length]);const a=[],b=[];
 for(let i=0;i<N;i++){const t=2*Math.PI*i/N;a.push(vertex(r*Math.cos(t),r*Math.sin(t),z0,i/N,0));b.push(vertex(r*Math.cos(t),r*Math.sin(t),z1,i/N,1));}
 for(let i=0;i<N;i++){const k=(i+1)%N;face(a[i],a[k],b[k]);face(a[i],b[k],b[i]);}
 const lo=vertex(0,0,z0,.5,.5),hi=vertex(0,0,z1,.5,.5);
 for(let i=0;i<N;i++){const k=(i+1)%N;face(lo,a[k],a[i]);face(hi,b[i],b[k]);}}
cylinder('handle_grip',-0.42,0.02,0.043);
cylinder('handle_pommel',-0.45,-0.42,0.065);
cylinder('handle_guard',0.015,0.055,0.065);
for(let i=0;i<9;i++)torus('chain_link_'+String(i+1).padStart(2,'0'),0,0,0.12+i*0.14,0.084,0.018,i%2);
torus('head_attachment_ring',0,0,1.42,0.073,0.018,1);
let lines=['# RAZLOM original Watcher flail chain and handle','mtllib SM_Watcher_FlailChain_v1.mtl'];
for(const v of V)lines.push('v '+v.map(n=>n.toFixed(6)).join(' '));
for(const uv of UV)lines.push('vt '+uv.map(n=>n.toFixed(6)).join(' '));
let gi=0;for(let i=0;i<F.length;i++){if(gi<groups.length&&groups[gi][1]===i){lines.push('g '+groups[gi][0]);gi++;}
 lines.push('f '+F[i].map(k=>k+'/'+k).join(' '));}
fs.writeFileSync(process.argv[2]||'SM_Watcher_FlailChain_v1.obj',lines.join('\n')+'\n');
console.log(JSON.stringify({vertices:V.length,uv:UV.length,triangles:F.length,groups:groups.length}));