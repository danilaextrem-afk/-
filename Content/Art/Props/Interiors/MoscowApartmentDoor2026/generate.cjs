// Original procedural mesh: Moscow apartment entrance door, meters, Z-up.
// Run: node generate.cjs ; no third-party packages.
const fs=require('fs');
function build(detail){
 const V=[],UV=[],F=[];let mat='steel';
 function box(x,y,z,w,d,h,m){
  const i=V.length+1; const p=[[x,y,z],[x+w,y,z],[x+w,y+d,z],[x,y+d,z],[x,y,z+h],[x+w,y,z+h],[x+w,y+d,z+h],[x,y+d,z+h]];
  V.push(...p);UV.push(...Array(8).fill([0,0]));const faces=[[0,2,1],[0,3,2],[4,5,6],[4,6,7],[0,1,5],[0,5,4],[1,2,6],[1,6,5],[2,3,7],[2,7,6],[3,0,4],[3,4,7]];
  F.push({m,faces:faces.map(a=>a.map(j=>i+j))});
 }
 function ring(cx,cy,cz,rx,ry,depth,segments,m){
  const i=V.length+1;for(let z of [cz,cz+depth])for(let k=0;k<segments;k++){const t=k*2*Math.PI/segments;V.push([cx+rx*Math.cos(t),cy+ry*Math.sin(t),z]);UV.push([k/segments,z===cz?0:1]);}
  const faces=[];for(let k=0;k<segments;k++){const n=(k+1)%segments;faces.push([i+k,i+n,i+segments+n],[i+k,i+segments+n,i+segments+k]);}F.push({m,faces});
 }
 box(-.53,-.08,0,1.06,.12,2.12,'frame');box(-.48,-.105,.045,.96,.07,2.03,'steel');
 box(-.43,-.114,.13,.86,.009,1.85,'paint');box(-.40,-.126,.18,.8,.008,1.73,'paint');
 for(let z of [.27,1.02,1.77]){box(-.51,.035,z,.06,.05,.13,'hinge');}
 box(.24,-.17,.98,.12,.05,.18,'lock');box(.28,-.23,1.05,.23,.045,.045,'steel');
 ring(.0,-.151,1.63,.028,.028,.014,detail?20:8,'brass');
 if(detail){for(let x of [-.43,.43])box(x,-.135,.16,.012,.015,1.78,'trim');for(let z of [.18,1.94])box(-.43,-.135,z,.87,.015,.012,'trim');}
 let out='# RAZLOM original door mesh; meters Z-up\nmtllib door.mtl\n';
 for(const v of V)out+='v '+v.join(' ')+'\n';
 for(let j=0;j<V.length;j++){const v=V[j];out+='vt '+((v[0]+.6)/1.2).toFixed(6)+' '+(v[2]/2.2).toFixed(6)+'\n';}
 for(const group of F){out+='usemtl '+group.m+'\n';for(const f of group.faces)out+='f '+f.map(i=>i+'/'+i).join(' ')+'\n';}
 return {out,vertices:V.length,triangles:F.reduce((s,g)=>s+g.faces.length,0),faces:F.flatMap(g=>g.faces),verticesArray:V};
}
if(require.main===module){for(const [name,detail] of [['door_LOD0.obj',true],['door_LOD1.obj',false]])fs.writeFileSync(name,build(detail).out);}
module.exports={build};
