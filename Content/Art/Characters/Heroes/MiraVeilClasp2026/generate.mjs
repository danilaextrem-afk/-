// Original deterministic parametric accessory generator; Node 18+
import {writeFileSync} from 'node:fs';
function makeMesh(segments=64,detail=true){
const V=[],UV=[],F=[];const mats=['aged_silver','oxidized_recess','dark_enamel','cloth_pin'];let material='aged_silver';
function vert(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
function tri(a,b,c){F.push([a,b,c,material])}
function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
function ring(cx,cy,major,minor,z,steps,tube=8,phase=0){const start=V.length+1;for(let i=0;i<=steps;i++){let t=2*Math.PI*i/steps;for(let j=0;j<=tube;j++){let p=2*Math.PI*j/tube;let r=major+minor*Math.cos(p);vert(cx+r*Math.cos(t),cy+r*Math.sin(t),z+minor*Math.sin(p),i/steps,j/tube)}}for(let i=0;i<steps;i++)for(let j=0;j<tube;j++){let a=start+i*(tube+1)+j,b=a+tube+1;quad(a,b,b+1,a+1)}}
function cylinder(cx,cy,r,z0,z1,n){let a=[],b=[];for(let i=0;i<n;i++){let t=i*2*Math.PI/n;a.push(vert(cx+r*Math.cos(t),cy+r*Math.sin(t),z0,(Math.cos(t)+1)/2,(Math.sin(t)+1)/2));b.push(vert(cx+r*Math.cos(t),cy+r*Math.sin(t),z1,(Math.cos(t)+1)/2,(Math.sin(t)+1)/2))}let c0=vert(cx,cy,z0,.5,.5),c1=vert(cx,cy,z1,.5,.5);for(let i=0;i<n;i++){let j=(i+1)%n;quad(a[i],a[j],b[j],b[i]);tri(c0,a[j],a[i]);tri(c1,b[i],b[j])}}
function strip(points,w,z){let l=[],r=[];for(let i=0;i<points.length;i++){let p=points[i],p0=points[Math.max(0,i-1)],p1=points[Math.min(points.length-1,i+1)];let dx=p1[0]-p0[0],dy=p1[1]-p0[1],len=Math.hypot(dx,dy)||1;let nx=-dy/len*w/2,ny=dx/len*w/2;l.push(vert(p[0]+nx,p[1]+ny,z,i/(points.length-1),0));r.push(vert(p[0]-nx,p[1]-ny,z,i/(points.length-1),1))}for(let i=0;i<points.length-1;i++)quad(l[i],r[i],r[i+1],l[i+1])}
material='aged_silver';cylinder(0,0,.047,-.008,.008,segments);ring(0,0,.039,.004,.012,segments,detail?10:6);ring(0,0,.026,.0025,.011,segments,detail?8:5);
material='dark_enamel';cylinder(0,0,.022,.009,.011,segments);
material='aged_silver';let rays=detail?12:8;for(let k=0;k<rays;k++){let a=2*Math.PI*k/rays;let pts=[];for(let j=0;j<5;j++){let r=.024+j*.0034;pts.push([r*Math.cos(a+.045*Math.sin(j)),r*Math.sin(a+.045*Math.sin(j))])}strip(pts,.0019,.014)}
material='oxidized_recess';for(let k=0;k<(detail?24:12);k++){let a=2*Math.PI*k/(detail?24:12);ring(.032*Math.cos(a),.032*Math.sin(a),.0015,.0006,.017,detail?10:6,5)}
material='aged_silver';for(let k=0;k<7;k++){let a=2*Math.PI*k/7;let pts=[];for(let j=0;j<6;j++){let r=.012+j*.0018;pts.push([r*Math.cos(a+.15*Math.sin(j*.7)),r*Math.sin(a+.15*Math.sin(j*.7))])}strip(pts,.0015,.014)}
material='cloth_pin';ring(0,0,.012,.0018,-.013,segments,6);strip([[-.05,-.004],[-.02,-.005],[.018,-.005],[.051,-.004]],.002,-.016);
let obj=['# RAZLOM original mesh; metres; Z up','mtllib MiraVeilClasp.mtl','o MiraVeilClasp'];for(let p of V)obj.push('v '+p.map(x=>x.toFixed(7)).join(' '));for(let p of UV)obj.push('vt '+p.map(x=>x.toFixed(7)).join(' '));let last='';for(let f of F){if(f[3]!==last){last=f[3];obj.push('usemtl '+last)}obj.push('f '+f.slice(0,3).map(i=>i+'/'+i).join(' '))}return {obj:obj.join('\n')+'\n',vertices:V.length,triangles:F.length,uv:UV.length,invalid:F.filter(f=>f.slice(0,3).some(i=>i<1||i>V.length)).length}}

writeFileSync('SM_MiraVeilClasp_LOD0.obj',makeMesh(64,true).obj);
writeFileSync('SM_MiraVeilClasp_LOD1.obj',makeMesh(24,false).obj);
console.log(JSON.stringify({lod0:makeMesh(64,true),lod1:makeMesh(24,false)},(k,v)=>k==='obj'?undefined:v,2));
