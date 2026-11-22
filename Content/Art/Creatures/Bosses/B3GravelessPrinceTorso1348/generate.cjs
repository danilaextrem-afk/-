// Original parametric B3 torso/armor blockout. Run: node generate.cjs
const fs=require('node:fs');
function build(sides){
 let V=[],UV=[],F=[],parts=[];
 function ringMesh(name, rings, mat){
  const start=V.length; for(let j=0;j<rings.length;j++){const [cx,cy,cz,rx,rz]=rings[j];for(let i=0;i<sides;i++){let a=2*Math.PI*i/sides;V.push([cx+rx*Math.cos(a),cy,cz+rz*Math.sin(a)]);UV.push([i/sides,j/(rings.length-1)]);}}
  const faces=[];for(let j=0;j<rings.length-1;j++)for(let i=0;i<sides;i++){let a=start+j*sides+i,b=start+j*sides+(i+1)%sides,c=a+sides,d=b+sides;faces.push([a,b,c],[b,d,c]);}
  // end caps, wound consistently
  let bot=V.length;V.push([rings[0][0],rings[0][1],rings[0][2]]);UV.push([.5,.5]);let top=V.length;let r=rings[rings.length-1];V.push([r[0],r[1],r[2]]);UV.push([.5,.5]);
  for(let i=0;i<sides;i++){let ni=(i+1)%sides;faces.push([bot,start+ni,start+i]);let e=start+(rings.length-1)*sides;faces.push([top,e+i,e+ni]);}
  parts.push({name,mat,faces});
 }
 ringMesh('cursed_torso',[[0,0,0,.36,.23],[0,.3,0,.48,.28],[0,.72,0,.59,.32],[0,1.12,0,.52,.30],[0,1.42,0,.25,.22]],'iron_black');
 ringMesh('rib_cuirass',[[0,.44,-.06,.50,.31],[0,.65,-.06,.62,.35],[0,.93,-.06,.61,.34],[0,1.18,-.06,.53,.29]],'iron_oxidized');
 for(let sign of [-1,1]){
 ringMesh(sign<0?'left_shoulder':'right_shoulder',[[sign*.52,1.17,0,.20,.22],[sign*.70,1.30,0,.28,.26],[sign*.83,1.45,0,.23,.22]],'iron_oxidized');
 ringMesh(sign<0?'left_upper_arm':'right_upper_arm',[[sign*.88,.46,0,.15,.15],[sign*.86,.78,0,.19,.19],[sign*.81,1.12,0,.20,.20]],'iron_black');
 }
 let out=['# B3 Graveless Prince torso, original procedural geometry','mtllib torso.mtl'];
 for(const v of V)out.push('v '+v.map(n=>n.toFixed(6)).join(' '));
 for(const uv of UV)out.push('vt '+uv.map(n=>n.toFixed(6)).join(' '));
 for(const p of parts){out.push('o '+p.name,'usemtl '+p.mat);for(const f of p.faces)out.push('f '+f.map(i=>(i+1)+'/'+(i+1)).join(' '));}
 return {obj:out.join('\n')+'\n',vertices:V.length,triangles:parts.reduce((n,p)=>n+p.faces.length,0)};
}
for(const [name,sides] of [['LOD0',24],['LOD1',12]])fs.writeFileSync('torso_'+name+'.obj',build(sides).obj);
console.log(build(24).vertices,build(24).triangles);
