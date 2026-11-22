// Original parametric mesh generator for RAZLOM, deterministic. Run: node generate.cjs
const fs=require('fs');
const generate=function generate(lod=0){
let v=[],vt=[],f=[],groups=[];let current="";
function part(name,mat){current=name;groups.push({name,mat,start:f.length});}
function quad(a,b,c,d){let start=v.length; for(let p of [a,b,c,d]){v.push(p);vt.push([p[0]*0.4+0.5,p[1]*0.4+0.5]);}f.push([start+1,start+2,start+3],[start+1,start+3,start+4]);}
function box(cx,cy,cz,sx,sy,sz){let x=cx,y=cy,z=cz,a=sx/2,b=sy/2,c=sz/2;let p=[[x-a,y-b,z-c],[x+a,y-b,z-c],[x+a,y+b,z-c],[x-a,y+b,z-c],[x-a,y-b,z+c],[x+a,y-b,z+c],[x+a,y+b,z+c],[x-a,y+b,z+c]];for(let q of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]])quad(...q.map(i=>p[i]));}
function rod(a,b,r,n=8){let dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],len=Math.hypot(dx,dy,dz),u=[-dy/Math.hypot(dx,dy)||1,dx/Math.hypot(dx,dy)||0,0],w=[-dz*u[1],dz*u[0],dx*u[1]-dy*u[0]],wl=Math.hypot(...w)||1;w=w.map(q=>q/wl);let ring=(p,i)=>p.map((q,j)=>q+r*(Math.cos(i*2*Math.PI/n)*u[j]+Math.sin(i*2*Math.PI/n)*w[j]));for(let i=0;i<n;i++)quad(ring(a,i),ring(a,(i+1)%n),ring(b,(i+1)%n),ring(b,i));}
part("oak_longitudinal_rails","oak");for(let x of [-0.69,0.69])box(x,0,0.91,0.19,3.25,0.22);
part("oak_crossmembers","oak");for(let y of [-1.27,-0.53,0.24,1.21])box(0,y,0.91,1.63,0.18,0.18);
part("oak_deck","oak");for(let x of [-0.56,-0.28,0,0.28,0.56])box(x,0.02,1.06,0.25,2.76,0.075);
part("oak_side_posts","oak");for(let x of [-0.77,0.77])for(let y of [-1.13,0,1.13])box(x,y,1.37,0.11,0.11,0.67);
part("oak_side_rails","oak");for(let x of [-0.77,0.77])box(x,0,1.67,0.105,2.48,0.11);
part("oak_drawbar","oak");box(0,-2.14,0.9,0.18,2.13,0.18);for(let x of [-0.29,0.29])box(x,-1.9,0.91,0.12,1.35,0.14);
part("iron_axles","iron");for(let y of [-0.94,0.94])rod([-1.16,y,0.78],[1.16,y,0.78],0.085,lod?6:12);
part("iron_straps","iron");for(let y of [-1.27,1.21])for(let x of [-0.69,0.69])box(x,y,1.03,0.23,0.15,0.025);
if(!lod){part("iron_rivets","iron");for(let y of [-1.27,1.21])for(let x of [-0.69,0.69])for(let d of [-0.05,0.05])rod([x+d,y,1.048],[x+d,y,1.065],0.023,8);}
let lines=["# RAZLOM original procedural mesh; meters, Z up","mtllib chassis.mtl"];for(let i=0;i<v.length;i++)lines.push("v "+v[i].map(n=>n.toFixed(5)).join(" "));for(let p of vt)lines.push("vt "+p.map(n=>n.toFixed(5)).join(" "));let gi=0;for(let i=0;i<f.length;i++){if(groups[gi+1]?.start===i)gi++;if(groups[gi].start===i)lines.push("g "+groups[gi].name,"usemtl "+groups[gi].mat);lines.push("f "+f[i].map(k=>k+"/"+k).join(" "));}return {obj:lines.join("\n")+"\n",vertices:v.length,triangles:f.length,uv:vt.length,invalid:f.filter(t=>new Set(t).size!==3||t.some(k=>k<1||k>v.length)).length};
};
for(const [name,lod] of [['chassis_LOD0.obj',0],['chassis_LOD1.obj',1]])fs.writeFileSync(name,generate(lod).obj);
