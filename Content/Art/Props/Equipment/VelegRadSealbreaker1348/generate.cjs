// Original procedural seal-breaker dagger; OBJ Z-up, metres. Node.js >=18.
const fs=require('fs');
function make(low=false){
 let v=[],uv=[],faces=[],groups=[];
 function vert(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length;}
 function tri(a,b,c,mat){faces.push([a,b,c,mat]);}
 function quad(a,b,c,d,m){tri(a,b,c,m);tri(a,c,d,m);}
 function ring(z,rx,ry,n,m,offset=0){let ids=[];for(let i=0;i<n;i++){let t=i*2*Math.PI/n;ids.push(vert(rx*Math.cos(t),ry*Math.sin(t),z,i/n,z/1.2));}return ids;}
 function bridge(a,b,m){for(let i=0;i<a.length;i++){let j=(i+1)%a.length;quad(a[i],a[j],b[j],b[i],m);}}
 let n=low?10:24;
 let profiles=[[0,.018,.018],[.014,.027,.027],[.038,.022,.022],[.055,.014,.014],[.21,.014,.014],[.235,.027,.025],[.253,.03,.028],[.269,.017,.016],[.284,.017,.016]];
 let rings=profiles.map(p=>ring(p[0],p[1],p[2],n));
 for(let i=0;i<rings.length-1;i++)bridge(rings[i],rings[i+1],i===1||i===5?'bronze':'dark_iron');
 // Seal-breaking blade: tapered double-edged lenticular section with faceted central ridge.
 let stations=low?[[.284,.032,.005],[.40,.037,.006],[.56,.024,.004],[.70,.001,.0005]]:[[.284,.032,.005],[.32,.039,.006],[.39,.042,.007],[.47,.035,.006],[.55,.027,.005],[.62,.017,.0035],[.69,.001,.0005]];
 let prev=null;for(let s=0;s<stations.length;s++){let [z,w,t]=stations[s];let ids=[vert(-w,0,z,0,z),vert(0,t,z,.5,z),vert(w,0,z,1,z),vert(0,-t,z,.5,z)];if(prev){for(let k=0;k<4;k++){let q=(k+1)%4;quad(prev[k],prev[q],ids[q],ids[k],'steel');}}prev=ids;}
 // Guard curved horns, symmetrical, with tapered segmented radial cross sections.
 for(let side of [-1,1]){let last=null;let segments=low?4:9;for(let j=0;j<=segments;j++){let t=j/segments,x=side*(.016+.105*t),z=.28+.036*t*t,r=.012*(1-t)+.002;let ids=[];for(let k=0;k<6;k++){let a=k*Math.PI/3;ids.push(vert(x,r*Math.cos(a),z+r*Math.sin(a),t,k/6));}if(last)bridge(last,ids,'bronze');last=ids;}}
 // Inset circular pommel seal, distinct metal.
 let a=ring(-.009,.016,.016,n),b=ring(.002,.022,.022,n);bridge(a,b,'bronze');
 let out=['# Original procedural 3D mesh; metres; Z-up; UV per vertex','mtllib sealbreaker.mtl'];v.forEach(p=>out.push('v '+p.map(x=>x.toFixed(6)).join(' ')));uv.forEach(p=>out.push('vt '+p.map(x=>x.toFixed(6)).join(' ')));let lastMat='';for(let [a,b,c,m] of faces){if(m!==lastMat){out.push('usemtl '+m);lastMat=m;}out.push('f '+[a,b,c].map(i=>i+'/'+i).join(' '));}return {obj:out.join('\n')+'\n',vertices:v.length,triangles:faces.length,degenerate:faces.filter(f=>{let [a,b,c]=f.map(i=>v[i-1]);let u=b.map((x,i)=>x-a[i]),w=c.map((x,i)=>x-a[i]);let cross=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]];return Math.hypot(...cross)<1e-10;}).length};}
for(let [name,low] of [['sealbreaker_LOD0.obj',false],['sealbreaker_LOD1.obj',true]]){let result=make(low);fs.writeFileSync(name,result.obj);console.log(name,result.vertices,result.triangles,result.degenerate);}
