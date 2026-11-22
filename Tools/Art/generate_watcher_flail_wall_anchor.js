// Original procedural asset, metres, Z-up. Run: node generate_watcher_flail_wall_anchor.js
const fs = require('fs');
function build(){
let v=[],uv=[],f=[],groups=[],current="";
function vertex(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length}
function tri(a,b,c){f.push([a,b,c,current])}
function group(s){current=s;groups.push(s)}
function box(cx,cy,cz,sx,sy,sz){let x=[cx-sx/2,cx+sx/2],y=[cy-sy/2,cy+sy/2],z=[cz-sz/2,cz+sz/2];for(let face of [[0,1,2,3],[4,7,6,5],[0,4,5,1],[3,2,6,7],[0,3,7,4],[1,5,6,2]]){let pts=[[x[0],y[0],z[0]],[x[1],y[0],z[0]],[x[1],y[1],z[0]],[x[0],y[1],z[0]],[x[0],y[0],z[1]],[x[1],y[0],z[1]],[x[1],y[1],z[1]],[x[0],y[1],z[1]]];let ids=face.map((j,k)=>vertex(...pts[j],k%2,Math.floor(k/2)));tri(ids[0],ids[1],ids[2]);tri(ids[0],ids[2],ids[3])}}
function tube(points,r,segments=12){let rings=[];for(let i=0;i<points.length;i++){let p=points[i],prev=points[Math.max(0,i-1)],next=points[Math.min(points.length-1,i+1)];let d=[next[0]-prev[0],next[1]-prev[1],next[2]-prev[2]],l=Math.hypot(...d);d=d.map(q=>q/l);let a=[-d[1],d[0],0],al=Math.hypot(...a);if(al<0.001)a=[1,0,0];else a=a.map(q=>q/al);let b=[d[1]*a[2]-d[2]*a[1],d[2]*a[0]-d[0]*a[2],d[0]*a[1]-d[1]*a[0]];let ring=[];for(let j=0;j<segments;j++){let t=j*2*Math.PI/segments;ring.push(vertex(p[0]+r*(a[0]*Math.cos(t)+b[0]*Math.sin(t)),p[1]+r*(a[1]*Math.cos(t)+b[1]*Math.sin(t)),p[2]+r*(a[2]*Math.cos(t)+b[2]*Math.sin(t)),j/segments,i/(points.length-1)))}rings.push(ring)}for(let i=0;i<rings.length-1;i++)for(let j=0;j<segments;j++){let k=(j+1)%segments;tri(rings[i][j],rings[i+1][j],rings[i+1][k]);tri(rings[i][j],rings[i+1][k],rings[i][k])}}
group("iron_backplate");box(0,0,0,0.24,0.035,0.38);
group("reinforcement_rib");box(0,-0.029,0,0.09,0.025,0.32);
group("hook_forged");let pts=[];for(let i=0;i<=28;i++){let t=i/28;pts.push([0,-0.05-0.23*t+0.11*t*t,-0.11+0.35*t-0.10*t*t])}tube(pts,0.024,16);
group("retaining_eye");let ring=[];for(let i=0;i<=48;i++){let t=2*Math.PI*i/48;ring.push([0.085+0.046*Math.cos(t),-0.095,0.085+0.046*Math.sin(t)])}tube(ring,0.009,10);
for(let i=0;i<4;i++){let x=i%2?0.087:-0.087,z=i<2?0.145:-0.145;group("rivet_"+i);let p=[];for(let j=0;j<=8;j++){let t=j/8;p.push([x,-0.047-0.011*Math.sin(Math.PI*t),z-0.009+0.018*t])}tube(p,0.009,10)}
let lines=["# RAZLOM original geometry: Watcher flail wall anchor, metres, Z up","mtllib SM_Watcher_FlailWallAnchor_v1.mtl"];for(let p of v)lines.push("v "+p.map(x=>x.toFixed(6)).join(" "));for(let p of uv)lines.push("vt "+p.map(x=>x.toFixed(6)).join(" "));let last="";for(let [a,b,c,g] of f){if(g!==last){lines.push("g "+g);lines.push("usemtl forged_iron");last=g}lines.push(`f ${a}/${a} ${b}/${b} ${c}/${c}`)}return {obj:lines.join("\n")+"\n",vertices:v.length,triangles:f.length,groups:groups.length,valid:f.every(q=>q.slice(0,3).every(n=>n>=1&&n<=v.length))}}
const out=build();if(!out.valid)throw new Error('invalid face indices');
fs.writeFileSync('SM_Watcher_FlailWallAnchor_v1.obj',out.obj);console.log(JSON.stringify({vertices:out.vertices,triangles:out.triangles,groups:out.groups}));
