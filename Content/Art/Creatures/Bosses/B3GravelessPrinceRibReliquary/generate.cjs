// Original procedural boss B3 chest reliquary, no external assets. Node >=18.
const fs=require('fs');
function build(lod=0){
 const v=[],vt=[],f=[];let mat='';const segments=lod?10:24;
 function vert(x,y,z,u,w){v.push([x,y,z]);vt.push([u,w]);return v.length;}
 function face(a,b,c,m){f.push([a,b,c,m]);}
 function tube(points,r,m,sides=lod?5:9){
  let rings=[];for(let i=0;i<points.length;i++){let p=points[i],q=points[Math.min(points.length-1,i+1)],prev=points[Math.max(0,i-1)];let d=[q[0]-prev[0],q[1]-prev[1],q[2]-prev[2]],dl=Math.hypot(...d)||1;d=d.map(x=>x/dl);let a=[-d[1],d[0],0],al=Math.hypot(...a);if(al<.01)a=[1,0,0];else a=a.map(x=>x/al);let b=[d[1]*a[2]-d[2]*a[1],d[2]*a[0]-d[0]*a[2],d[0]*a[1]-d[1]*a[0]];rings.push(Array.from({length:sides},(_,j)=>{let t=2*Math.PI*j/sides;return vert(p[0]+r*(a[0]*Math.cos(t)+b[0]*Math.sin(t)),p[1]+r*(a[1]*Math.cos(t)+b[1]*Math.sin(t)),p[2]+r*(a[2]*Math.cos(t)+b[2]*Math.sin(t)),j/sides,i/(points.length-1));}));}
  for(let i=0;i<rings.length-1;i++)for(let j=0;j<sides;j++){let k=(j+1)%sides,a=rings[i][j],b=rings[i][k],c=rings[i+1][j],d=rings[i+1][k];face(a,c,b,m);face(b,c,d,m);}
 }
 function box(cx,cy,cz,sx,sy,sz,m){let p=[[0,0,1],[0,0,-1],[0,1,0],[0,-1,0],[1,0,0],[-1,0,0]];for(let n of p){let a=n[0]!==0?1:0,b=n[1]!==0?1:0,c=n[2]!==0?1:0;let pts=[];for(let [u,w] of [[0,0],[1,0],[1,1],[0,1]]){let x=cx+(a?n[0]*sx/2:(u-.5)*sx),y=cy+(b?n[1]*sy/2:(a?(u-.5)*sy:(w-.5)*sy)),z=cz+(c?n[2]*sz/2:(w-.5)*sz);if(b){z=cz+(w-.5)*sz;}pts.push(vert(x,y,z,u,w));}face(pts[0],pts[1],pts[2],m);face(pts[0],pts[2],pts[3],m);}}
 // X lateral, Y depth, Z up, 1.8m scale.
 // central funerary sternum, split armor plates
 box(0,-.19,1.20,.20,.09,.65,'oxidized_iron');
 for(let side of [-1,1]){
  for(let i=0;i<5;i++){let z=1.46-i*.115,pts=[];let count=lod?7:16;for(let j=0;j<=count;j++){let t=j/count,theta=t*Math.PI*.84;pts.push([side*(.10+.44*Math.sin(theta)), -.13-.16*Math.sin(theta),z-.14*t]);}tube(pts,.023+(4-i)*.002,'bone',lod?5:8);}
  // shoulder plates
  box(side*.44,-.105,1.57,.35,.15,.13,'oxidized_iron');
  // hooked shoulder spikes
  tube([[side*.47,-.12,1.60],[side*.64,-.13,1.69],[side*.77,-.15,1.91]],.033,'bone',lod?5:8);
 }
 // relic heart frame: toroidal ring with fractured prongs
 let ring=[];for(let i=0;i<=segments;i++){let a=i/segments*Math.PI*2;ring.push([.17*Math.cos(a),-.28,1.20+.21*Math.sin(a)]);}tube(ring,.031,'silver',lod?5:8);
 for(let k=0;k<7;k++){let a=k*2*Math.PI/7;let x=.17*Math.cos(a),z=1.2+.21*Math.sin(a);tube([[x,-.30,z],[x*1.18,-.33,1.2+(z-1.2)*1.2]],.014,'silver',5);}
 box(0,-.315,1.2,.105,.025,.14,'seal_black');
 // suspension and ragged lower armor segments
 for(let k=-2;k<=2;k++){box(k*.13,-.13,.84,.105,.11,.23-(Math.abs(k)*.025),'oxidized_iron');}
 let lines=['# original mesh B3 boss armor, meters, Z-up','mtllib materials.mtl'];let prev='';
 for(let x of v)lines.push('v '+x.map(a=>a.toFixed(6)).join(' '));for(let x of vt)lines.push('vt '+x.map(a=>a.toFixed(6)).join(' '));
 for(let x of f){if(x[3]!==prev){lines.push('usemtl '+x[3]);prev=x[3];}lines.push('f '+x.slice(0,3).map(i=>i+'/'+i).join(' '));}
 return {obj:lines.join('\n')+'\n',stats:{vertices:v.length,triangles:f.length,uv:v.length,materials:[...new Set(f.map(x=>x[3]))],invalidFaces:f.filter(x=>x.slice(0,3).some(i=>i<1||i>v.length)).length}};
}
if(require.main===module){for(let lod of [0,1]){let a=build(lod);fs.writeFileSync('LOD'+lod+'.obj',a.obj);console.log('LOD'+lod,a.stats);}}
module.exports={build};
