// Original procedural mesh, centimeters, Z up. Run: node generate.mjs
import {writeFileSync} from 'node:fs';
function build(n){
 const v=[],uv=[],faces=[];let mat='';
 function vert(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length}
 function tri(a,b,c,m){faces.push([a,b,c,m])}
 function tube(points,r,seg,m){let rings=[];for(let i=0;i<points.length;i++){let [x,y,z]=points[i],ring=[];for(let j=0;j<seg;j++){let a=2*Math.PI*j/seg;ring.push(vert(x+r*Math.cos(a),y+r*Math.sin(a),z,i/Math.max(1,points.length-1),j/seg))}rings.push(ring)}
 for(let k=0;k<rings.length-1;k++)for(let j=0;j<seg;j++){let a=rings[k][j],b=rings[k][(j+1)%seg],c=rings[k+1][j],d=rings[k+1][(j+1)%seg];tri(a,b,c,m);tri(b,d,c,m)}}
 function box(x0,x1,y0,y1,z0,z1,m){let p=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];for(let [a,b,c,d] of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]]){let ids=[a,b,c,d].map((k,i)=>vert(...p[k],i===1||i===2?1:0,i>=2?1:0));tri(ids[0],ids[1],ids[2],m);tri(ids[0],ids[2],ids[3],m)}}
 tube(Array.from({length:n+1},(_,i)=>[2*Math.cos(2*Math.PI*i/n),2*Math.sin(2*Math.PI*i/n),0]),.32,n>20?8:6,'dark_iron');
 tube(Array.from({length:n>20?27:16},(_,i)=>[0,-2-i*.17,0]),.31,n>20?8:6,'dark_iron');
 for(let b of [[-.32,1.15,-6.5,-6.12,-.26,.26,'polished_edges'],[-.32,.78,-6.12,-5.78,-.26,.26,'dark_iron'],[-.32,1.45,-5.52,-5.15,-.26,.26,'dark_iron'],[-.32,.65,-5.15,-4.85,-.26,.26,'polished_edges']])box(...b);
 if(n>20){for(let y of [-2.12,-2.42,-2.72])box(-.45,.45,y-.065,y+.065,-.36,.36,'polished_edges');for(let i=0;i<12;i++){let a=2*Math.PI*i/12,x=1.64*Math.cos(a),y=1.64*Math.sin(a);box(x-.085,x+.085,y-.085,y+.085,.24,.29,'polished_edges')}}
 let lines=['# Original archive key, cm, Z-up','mtllib key.mtl','o VelegRadArchiveKey'];for(let p of v)lines.push('v '+p.map(x=>x.toFixed(6)).join(' '));for(let p of uv)lines.push('vt '+p.map(x=>x.toFixed(6)).join(' '));let last='';for(let [a,b,c,m] of faces){if(m!==last){lines.push('usemtl '+m);last=m}lines.push('f '+[a,b,c].map(x=>x+'/'+x).join(' '))}
 return {obj:lines.join('\n')+'\n',stats:{vertices:v.length,uv:uv.length,triangles:faces.length,invalid_indices:faces.filter(f=>f.slice(0,3).some(x=>x<1||x>v.length)).length,degenerate_faces:faces.filter(f=>new Set(f.slice(0,3)).size!==3).length}}}
for(let [lod,n] of [['LOD0',48],['LOD1',16]]){let r=build(n);writeFileSync('key_'+lod+'.obj',r.obj);console.log(lod,r.stats)}
