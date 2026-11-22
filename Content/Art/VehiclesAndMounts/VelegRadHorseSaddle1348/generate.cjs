// Original procedural saddle geometry; run: node generate.cjs
const fs=require('fs');
function build(detail){
 const V=[],UV=[],F=[],groups=[];let mat='';
 function vert(p,u,v){V.push(p);UV.push([u,v]);return V.length;}
 function tri(a,b,c){F.push([a+1,b+1,c+1]);groups.push(mat);}
 function strip(name,fn,n,m,width){mat=name;let start=V.length;for(let i=0;i<=n;i++){let t=i/n;for(let j=0;j<=m;j++){let s=(j/m-.5)*2;let p=fn(t,s,width);vert(p,t,j/m);}}for(let i=0;i<n;i++)for(let j=0;j<m;j++){let a=start+i*(m+1)+j,b=a+m+1;tri(a,b,a+1);tri(a+1,b,b+1);}}
 function tube(name,fn,n,sides,r){mat=name;let st=V.length;for(let i=0;i<=n;i++){let t=i/n,p=fn(t),q=fn(Math.min(1,t+0.001));let d=[q[0]-p[0],q[1]-p[1],q[2]-p[2]];let len=Math.hypot(...d)||1;d=d.map(x=>x/len);let ref=Math.abs(d[2])<.9?[0,0,1]:[1,0,0],a=[d[1]*ref[2]-d[2]*ref[1],d[2]*ref[0]-d[0]*ref[2],d[0]*ref[1]-d[1]*ref[0]],al=Math.hypot(...a);a=a.map(x=>x/al);let b=[d[1]*a[2]-d[2]*a[1],d[2]*a[0]-d[0]*a[2],d[0]*a[1]-d[1]*a[0]];for(let j=0;j<=sides;j++){let th=2*Math.PI*j/sides;vert(p.map((x,k)=>x+r*(a[k]*Math.cos(th)+b[k]*Math.sin(th))),t,j/sides);}}for(let i=0;i<n;i++)for(let j=0;j<sides;j++){let a=st+i*(sides+1)+j,b=a+sides+1;tri(a,a+1,b);tri(a+1,b+1,b);}}
 // X horse forward, Y lateral, Z upward; saddle pad and hand-shaped leather seat
 strip('WoolPad',(t,s)=>[-.43+.86*t,s*(.31+.045*Math.sin(Math.PI*t)),.12+.09*Math.sin(Math.PI*t)-.095*s*s],detail?32:12,detail?16:6);
 strip('DarkLeather',(t,s)=>[-.37+.74*t,s*(.25+.03*Math.sin(Math.PI*t)),.165+.12*Math.sin(Math.PI*t)-.10*s*s+.035*Math.pow(Math.abs(s),4)],detail?40:14,detail?18:6);
 // Raised cantle and pommel, smooth wood/leather arches
 for(const [x,h] of [[-.34,.31],[.34,.26]])tube('OiledWood',t=>[x,.24*Math.cos(Math.PI*t),h+.09*Math.sin(Math.PI*t)],detail?32:12,detail?10:6,.025);
 // hanging stirrup straps and metal stirrup loops
 for(const side of [-1,1]){
  strip('HarnessLeather',(t,s)=>[-.03+s*.022,side*(.22+.06*t),.17-.40*t],detail?12:6,2);
  tube('ForgedIron',t=>[-.03+.105*Math.cos(2*Math.PI*t),side*.29,-.28+.11*Math.sin(2*Math.PI*t)],detail?40:16,detail?8:5,.011);
  tube('BrassStud',t=>[-.03+.018*Math.cos(2*Math.PI*t),side*.26,.17+.018*Math.sin(2*Math.PI*t)],detail?12:6,detail?7:5,.009);
 }
 // front and rear girth straps
 for(const x of [-.22,.20])strip('HarnessLeather',(t,s)=>[x+s*.025,-.29+.58*t,.07-.27*Math.sin(Math.PI*t)],detail?20:8,2);
 let out='mtllib materials.mtl\no Saddle\n',prev='';for(let i=0;i<V.length;i++){out+='v '+V[i].map(x=>x.toFixed(6)).join(' ')+'\n';out+='vt '+UV[i].map(x=>x.toFixed(6)).join(' ')+'\n';}for(let i=0;i<F.length;i++){if(groups[i]!==prev){prev=groups[i];out+='usemtl '+prev+'\n';}out+='f '+F[i].map(x=>x+'/'+x).join(' ')+'\n';}
 return {out,vertices:V.length,triangles:F.length,uv:UV.length,degenerate:F.filter(f=>{let a=V[f[0]-1],b=V[f[1]-1],c=V[f[2]-1],u=b.map((x,k)=>x-a[k]),v=c.map((x,k)=>x-a[k]);let cr=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];return Math.hypot(...cr)<1e-9;}).length};
}
for(const [lod,high] of [['LOD0',true],['LOD1',false]]){let r=build(high);fs.writeFileSync(lod+'.obj',r.out);console.log(lod,JSON.stringify({vertices:r.vertices,triangles:r.triangles,uv:r.uv,degenerate:r.degenerate}));}
