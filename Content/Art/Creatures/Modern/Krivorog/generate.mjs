// Original procedural Krivorog v0.1; run: node generate.mjs
import fs from 'node:fs';
const V=[],UV=[],F=[];let material='skin';const groups=[];
function ringTube(name,points,radii,sides=12,mat='skin'){const start=V.length;for(let j=0;j<points.length;j++){const p=points[j];for(let i=0;i<sides;i++){let a=i*2*Math.PI/sides;V.push([p[0]+Math.cos(a)*radii[j],p[1]+Math.sin(a)*radii[j],p[2]]);UV.push([i/sides,j/(points.length-1)]);}}const faces=[];for(let j=0;j<points.length-1;j++)for(let i=0;i<sides;i++){let a=start+j*sides+i,b=start+j*sides+(i+1)%sides,c=b+sides,d=a+sides;faces.push([a,b,c],[a,c,d]);}groups.push({name,mat,faces});}
ringTube('torso',[[0,0,72],[0,0,105],[0,0,140],[0,0,174],[0,0,190]],[20,27,24,17,8],20);
ringTube('neck',[[0,0,182],[0,0,199],[0,0,216]],[9,10,7],16);
ringTube('head',[[0,0,207],[0,0,221],[0,0,237],[0,0,246]],[11,14,11,4],20);
for(const s of [-1,1]){ringTube('arm_'+s,[[s*20,0,174],[s*34,0,154],[s*42,-2,118],[s*49,-7,80]],[11,10,7,5],12);ringTube('leg_'+s,[[s*12,0,78],[s*17,0,49],[s*21,-6,20],[s*25,-15,3]],[13,11,8,7],14);for(let k=0;k<3;k++)ringTube('claw_'+s+'_'+k,[[s*(45+k*4),-7,80],[s*(48+k*4),-10,65],[s*(49+k*4),-14,58]],[3,2,0.35],8,'horn');}
ringTube('horn_left',[[-9,0,240],[-17,0,254],[-20,4,272],[-24,9,290]],[6,5,3,0.25],12,'horn');
ringTube('horn_right',[[9,0,240],[19,0,248],[30,-1,255],[33,-3,263]],[7,6,3,0.25],12,'horn');
ringTube('spine',[[0,12,96],[0,14,146],[0,11,185]],[4,7,3],10,'scar');
let obj='# Krivorog original 3D draft; centimeters Z-up; procedural UV0\nmtllib Krivorog.mtl\n';
for(const v of V)obj+='v '+v.join(' ')+'\n';for(const uv of UV)obj+='vt '+uv.join(' ')+'\n';
for(const g of groups){obj+='g '+g.name+'\nusemtl '+g.mat+'\n';for(const f of g.faces)obj+='f '+f.map(x=>(x+1)+'/'+(x+1)).join(' ')+'\n';}
fs.writeFileSync('Krivorog.obj',obj);
console.log(JSON.stringify({vertices:V.length,triangles:groups.reduce((n,g)=>n+g.faces.length,0),uv:UV.length}));
