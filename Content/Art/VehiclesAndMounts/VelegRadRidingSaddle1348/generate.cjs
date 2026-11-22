// Original procedural saddle generator; Node.js 18+; no dependencies
const fs=require('fs');
const generate=function generate(detail){
 const V=[],UV=[],F=[],M=[];let group="leather";
 function vert(p,uv){V.push(p);UV.push(uv);return V.length}
 function tri(a,b,c){F.push([a,b,c]);M.push(group)}
 function loft(sections,n,mat){group=mat;let rings=[];for(let j=0;j<sections.length;j++){let [x,y,z,ry,rz]=sections[j],ring=[];for(let k=0;k<n;k++){let t=2*Math.PI*k/n;ring.push(vert([x,y+ry*Math.cos(t),z+rz*Math.sin(t)],[j/(sections.length-1),k/n]))}rings.push(ring)}
 for(let j=0;j<rings.length-1;j++)for(let k=0;k<n;k++){let a=rings[j][k],b=rings[j][(k+1)%n],c=rings[j+1][k],d=rings[j+1][(k+1)%n];tri(a,c,b);tri(b,c,d)}
 for(let k=1;k<n-1;k++){tri(rings[0][0],rings[0][k+1],rings[0][k]);let last=rings[rings.length-1];tri(last[0],last[k],last[k+1])}}
 function band(x1,x2,y,z,w,h,mat){loft([[x1,y,z,w,h],[x1+(x2-x1)*.25,y,z+.012,w*1.03,h],[x2-(x2-x1)*.25,y,z+.012,w*1.03,h],[x2,y,z,w,h]],detail?12:8,mat)}
 // x forward, z up, metres; original 1348 saddle seat, pommel and cantle
 loft([[-.43,0,1.21,.18,.075],[-.32,0,1.20,.24,.065],[-.15,0,1.16,.28,.052],[.10,0,1.15,.27,.052],[.30,0,1.19,.23,.07],[.42,0,1.23,.19,.08]],detail?24:12,"saddle_oiled_leather");
 loft([[-.42,0,1.17,.27,.025],[-.22,0,1.16,.34,.03],[.12,0,1.15,.35,.03],[.40,0,1.17,.27,.025]],detail?20:10,"wool_pad");
 loft([[-.42,0,1.25,.20,.09],[-.46,0,1.35,.22,.075],[-.43,0,1.40,.19,.045]],detail?20:10,"cantle");
 loft([[.36,0,1.25,.20,.08],[.42,0,1.33,.18,.07],[.43,0,1.37,.16,.045]],detail?20:10,"pommel");
 for(let side of [-1,1]){
  band(-.23,.24,side*.28,1.04,.045,.018,"strap_dark_leather");
  loft([[-.02,side*.30,1.10,.025,.025],[-.02,side*.37,.93,.025,.025],[-.02,side*.39,.76,.023,.023]],detail?12:8,"stirrup_leather");
  loft([[-.06,side*.39,.75,.012,.013],[-.06,side*.39,.68,.012,.013]],detail?12:8,"iron");
  loft([[.01,side*.39,.75,.012,.013],[.01,side*.39,.68,.012,.013]],detail?12:8,"iron");
  band(-.065,.015,side*.39,.67,.017,.012,"iron");
 }
 return {V,UV,F,M}
};
const obj=function obj(d){let s="# Original saddle mesh; units metres; X forward Z up\n";for(const v of d.V)s+=`v ${v.map(x=>x.toFixed(6)).join(" ")}\n`;for(const uv of d.UV)s+=`vt ${uv.map(x=>x.toFixed(6)).join(" ")}\n`;s+="mtllib saddle.mtl\n";let last="";d.F.forEach((f,i)=>{if(d.M[i]!==last){last=d.M[i];s+=`usemtl ${last}\n`}s+=`f ${f.map(n=>n+"/"+n).join(" ")}\n`});return s};
fs.writeFileSync('saddle_LOD0.obj',obj(generate(true)));
fs.writeFileSync('saddle_LOD1.obj',obj(generate(false)));
