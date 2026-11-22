// Original procedural wattle fence, meters, Z up. Node >=18.
const fs=require('fs'); const path=require('path');
function make(lod){let v=[],uv=[],f=[],groups=[];let N=lod?6:12;
function box(cx,cy,cz,sx,sy,sz,mat){let start=v.length;let p=[[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]];
for(let q of p){v.push([cx+q[0]*sx/2,cy+q[1]*sy/2,cz+q[2]*sz/2]);uv.push([(q[0]+1)/2,(q[2]+1)/2]);}
let quads=[[0,1,2,3],[4,7,6,5],[0,4,5,1],[1,5,6,2],[2,6,7,3],[3,7,4,0]];
for(let q of quads){f.push([start+q[0]+1,start+q[1]+1,start+q[2]+1,mat]);f.push([start+q[0]+1,start+q[2]+1,start+q[3]+1,mat]);}}
for(let i=0;i<5;i++){let x=-1.8+i*.9;box(x,0,.72,.11,.14,1.7,'wood_post');box(x,0,1.59,.17,.18,.13,'wood_post');}
for(let j=0;j<N;j++){let z=.27+j*(1.18/(N-1));let y=(j%2?-.055:.055);for(let i=0;i<4;i++){let x=-1.8+i*.9+.45;box(x,y,z,.92,.065,lod?.095:.065,'wood_wattle');}}
return {v,uv,f};}
function obj(mesh){let a=['# RAZLOM original authored mesh; Z up; meters'];for(let p of mesh.v)a.push('v '+p.map(x=>x.toFixed(5)).join(' '));for(let p of mesh.uv)a.push('vt '+p.map(x=>x.toFixed(5)).join(' '));let last='';for(let q of mesh.f){if(q[3]!==last){last=q[3];a.push('usemtl '+last);}a.push('f '+q.slice(0,3).map(n=>n+'/'+n).join(' '));}return a.join('\n')+'\n';}
for(let lod=0;lod<=1;lod++)fs.writeFileSync(path.join(__dirname,'LOD'+lod+'.obj'),obj(make(lod)));
module.exports={make,obj};