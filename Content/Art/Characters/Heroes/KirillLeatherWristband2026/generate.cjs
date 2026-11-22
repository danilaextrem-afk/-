// Original procedural wristband; Node.js 18+. Run: node generate.cjs
const fs=require('fs');
function build(n){
 let v=[],uv=[],faces=[],groups=[];
 function ring(name,r0,r1,y0,y1,segments){
  let start=v.length;
  for(let j=0;j<4;j++){let r=j===0||j===3?r0:r1, y=j<2?y0:y1;
   for(let i=0;i<=segments;i++){let a=i/segments*Math.PI*2;v.push([r*Math.cos(a),y,r*Math.sin(a)]);uv.push([i/segments,j/3]);}}
  for(let j=0;j<3;j++)for(let i=0;i<segments;i++){let a=start+j*(segments+1)+i,b=a+segments+1;faces.push([a,b,a+1],[a+1,b,b+1]);groups.push(name,name);}
 }
 ring('leather',.038,.043,-.024,.024,n);
 ring('steel_buckle',.042,.047,-.007,.007,Math.max(12,Math.floor(n/2)));
 for(let k=0;k<4;k++){let a=k*Math.PI/2;let x=.044*Math.cos(a),z=.044*Math.sin(a);let s=.004;
 let start=v.length;for(let [dx,dy,dz] of [[-s,-s,-s],[s,-s,-s],[s,s,-s],[-s,s,-s],[-s,-s,s],[s,-s,s],[s,s,s],[-s,s,s]]){v.push([x+dx,dy,z+dz]);uv.push([(dx+s)/(2*s),(dy+s)/(2*s)]);}
 for(let q of [[0,1,2],[0,2,3],[4,6,5],[4,7,6],[0,4,5],[0,5,1],[1,5,6],[1,6,2],[2,6,7],[2,7,3],[3,7,4],[3,4,0]]){faces.push(q.map(t=>t+start));groups.push('rivets');}
 }
 let lines=['# RAZLOM original Kirill wristband','mtllib wristband.mtl'];
 for(let p of v)lines.push('v '+p.map(x=>x.toFixed(6)).join(' '));
 for(let t of uv)lines.push('vt '+t.map(x=>x.toFixed(6)).join(' '));
 let prev='';faces.forEach((f,i)=>{if(groups[i]!==prev){prev=groups[i];lines.push('usemtl '+prev);}lines.push('f '+f.map(j=>(j+1)+'/'+(j+1)).join(' '));});
 return {obj:lines.join('\n')+'\n',vertices:v.length,triangles:faces.length,degenerate:faces.filter(f=>{let a=v[f[0]],b=v[f[1]],c=v[f[2]],u=b.map((x,i)=>x-a[i]),w=c.map((x,i)=>x-a[i]);let cross=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]];return Math.hypot(...cross)<1e-9;}).length};
}
for(let [name,n] of [['LOD0',64],['LOD1',24]]){let a=build(n);fs.writeFileSync('wristband_'+name+'.obj',a.obj);console.log(name,a.vertices,a.triangles,a.degenerate);}
