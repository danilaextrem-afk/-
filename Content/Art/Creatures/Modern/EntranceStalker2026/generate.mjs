// Original procedural mesh for RZ_CREATURE_MODERN_ENTRANCE_STALKER_001
// Run: node generate.mjs (writes OBJ LOD0/LOD1 and validation.json)
import fs from 'node:fs';
const parts=[
 ['torso',[[0,0,1.05,.23],[0,0,1.45,.31],[0,0,1.9,.25],[0,0,2.25,.19]],'shadow'],
 ['neck',[[0,0,2.18,.11],[0,0,2.36,.095]],'shadow'],
 ['head',[[0,0,2.34,.13],[0,0,2.49,.15],[0,0,2.66,.105]],'shadow'],
 ['armL',[[-.23,0,2.13,.13],[-.42,0,1.75,.10],[-.53,-.06,1.28,.07],[-.60,-.12,.87,.045]],'shadow'],
 ['armR',[[.23,0,2.13,.13],[.42,0,1.75,.10],[.53,-.06,1.28,.07],[.60,-.12,.87,.045]],'shadow'],
 ['legL',[[-.14,0,1.12,.16],[-.16,0,.70,.12],[-.18,0,.27,.085],[-.21,-.08,.06,.075]],'shadow'],
 ['legR',[[.14,0,1.12,.16],[.16,0,.70,.12],[.18,0,.27,.085],[.21,-.08,.06,.075]],'shadow'],
 ['spine',[[0,.19,1.10,.055],[0,.24,1.55,.06],[0,.23,2.04,.045]],'bone'],
 ['jaw',[[0,-.115,2.39,.07],[0,-.17,2.47,.09]],'bone']
];
function build(sides){
 const v=[],uv=[],f=[];let groups=[];
 for(const [name,rings,mat] of parts){
  let start=f.length;
  for(let j=0;j<rings.length;j++){let [x,y,z,r]=rings[j];for(let k=0;k<sides;k++){let a=k*2*Math.PI/sides;v.push([x+r*Math.cos(a),y+r*Math.sin(a),z]);uv.push([k/sides,j/(rings.length-1)]);}}
  let offset=v.length-rings.length*sides;
  for(let j=0;j<rings.length-1;j++)for(let k=0;k<sides;k++){
   let a=offset+j*sides+k+1,b=offset+j*sides+(k+1)%sides+1,c=b+sides,d=a+sides;
   f.push([a,b,c],[a,c,d]);
  }
  groups.push({name,mat,start,end:f.length});
 }
 let obj='# Original mesh: Entrance Stalker / Podjezdnik 2026\nmtllib EntranceStalker.mtl\n';
 v.forEach(p=>obj+='v '+p.map(n=>n.toFixed(6)).join(' ')+'\n');
 uv.forEach(p=>obj+='vt '+p.map(n=>n.toFixed(6)).join(' ')+'\n');
 for(let i=0;i<groups.length;i++){let g=groups[i];obj+='g '+g.name+'\nusemtl '+g.mat+'\n';for(let j=g.start;j<g.end;j++)obj+='f '+f[j].map(k=>k+'/'+k).join(' ')+'\n';}
 const valid=f.every(t=>t.every(k=>k>=1&&k<=v.length))&&uv.length===v.length&&v.every(p=>p.every(Number.isFinite));
 if(!valid)throw Error('Invalid mesh');
 return {obj,vertices:v.length,triangles:f.length,uv:uv.length,valid};
}
const a=build(16),b=build(8);
fs.writeFileSync('EntranceStalker_LOD0.obj',a.obj);fs.writeFileSync('EntranceStalker_LOD1.obj',b.obj);
fs.writeFileSync('validation.json',JSON.stringify({LOD0:{vertices:a.vertices,triangles:a.triangles},LOD1:{vertices:b.vertices,triangles:b.triangles},index_validation:'pass'},null,2));
