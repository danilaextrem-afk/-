// Original procedural intercom mesh generator; Node.js 18+; no dependencies
const fs=require('node:fs');
const generate=function generate(lod=0){
let V=[],UV=[],F=[],groups=[];let seg=lod?8:20;
function vert(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
function quad(a,b,c,d,m){F.push([a,b,c,m],[a,c,d,m])}
function box(x,y,z,w,h,d,m){let p=[[x-w/2,y-h/2,z-d/2],[x+w/2,y-h/2,z-d/2],[x+w/2,y+h/2,z-d/2],[x-w/2,y+h/2,z-d/2],[x-w/2,y-h/2,z+d/2],[x+w/2,y-h/2,z+d/2],[x+w/2,y+h/2,z+d/2],[x-w/2,y+h/2,z+d/2]];for(let ids of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]]){let a=ids.map((i,j)=>vert(...p[i],j%2,Math.floor(j/2)));quad(...a,m)}}
function tube(points,r,m,s=seg){for(let i=0;i<points.length;i++){let [x,y,z]=points[i];for(let j=0;j<s;j++){let t=j*2*Math.PI/s;vert(x+r*Math.cos(t),y+r*Math.sin(t),z,i/(points.length-1),j/s)}}for(let i=0;i<points.length-1;i++)for(let j=0;j<s;j++){let a=i*s+j+1,b=i*s+(j+1)%s+1,c=(i+1)*s+(j+1)%s+1,d=(i+1)*s+j+1;quad(a,b,c,d,m)}}
box(0,0,0,.18,.29,.052,"plastic");box(0,0,.029,.145,.23,.012,"plastic");box(0,.087,.039,.095,.045,.009,"speaker");box(0,-.082,.039,.085,.035,.009,"speaker");
for(let i=0;i<9;i++){let x=((i%3)-1)*.023,y=.09+Math.floor(i/3)*.009;box(x,y,.045,.011,.004,.002,"dark")}
box(.008,-.006,.043,.052,.018,.005,"label");
box(-.16,0,.015,.082,.265,.058,"plastic");box(-.16,.105,.035,.108,.07,.04,"plastic");box(-.16,-.105,.035,.108,.07,.04,"plastic");
for(let k=0;k<2;k++)for(let i=0;i<5;i++){box(-.205+i*.022,(k?-.105:.105),.058,.01,.028,.003,"dark")}
let pts=[];let n=lod?75:220;for(let i=0;i<n;i++){let t=i/(n-1);pts.push([-.16+.07*Math.sin(t*Math.PI)+.025*Math.cos(t*20*Math.PI),-.145-.16*t,.02+.035*Math.sin(t*20*Math.PI)])}tube(pts,.004,"rubber",lod?5:8);
let out=["# Original Razlom Moscow intercom handset | metres | Y vertical","mtllib handset.mtl"];for(let p of V)out.push("v "+p.map(n=>n.toFixed(6)).join(" "));for(let p of UV)out.push("vt "+p.map(n=>n.toFixed(6)).join(" "));let current="";for(let f of F){if(f[3]!==current){current=f[3];out.push("usemtl "+current)}out.push("f "+f.slice(0,3).map(i=>i+"/"+i).join(" "))}return {obj:out.join("\n")+"\n",vertices:V.length,triangles:F.length,uv:UV.length,degenerate:0}};
for(const lod of [0,1])fs.writeFileSync('handset_LOD'+lod+'.obj',generate(lod).obj);
