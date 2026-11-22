// Original RAZLOM Watcher articulated gauntlets. Run: node Tools/Art/generate_watcher_gauntlets.js
// Source coordinates in metres, Z up. Deterministic, no dependencies.
const fs = require('fs');
const out = process.argv[2] || 'Content/Art/Characters/Watcher/SM_Watcher_Gauntlets_v1.obj';
let lines=['# RAZLOM original procedural articulated Watcher gauntlets, v1','# units: metres; Z up; symmetric X hands','# separate named components for future rig split'];let vertices=0,faces=0;
function ringTube(name,centers,radii,sides=12){
 lines.push('o '+name);let start=vertices+1;
 for(let j=0;j<centers.length;j++){
  const c=centers[j],r=radii[j];
  for(let i=0;i<sides;i++){let a=2*Math.PI*i/sides;
   lines.push('v '+(c[0]+r[0]*Math.cos(a)).toFixed(6)+' '+(c[1]+r[1]*Math.sin(a)).toFixed(6)+' '+c[2].toFixed(6));vertices++;
  }
 }
 for(let j=0;j<centers.length-1;j++)for(let i=0;i<sides;i++){
  let a=start+j*sides+i,b=start+j*sides+(i+1)%sides,c=b+sides,d=a+sides;
  lines.push('f '+a+' '+b+' '+d);lines.push('f '+b+' '+c+' '+d);faces+=2;
 }
 // cap
 for(let i=1;i<sides-1;i++){lines.push('f '+start+' '+(start+i+1)+' '+(start+i));faces++;}
 let last=start+(centers.length-1)*sides;
 for(let i=1;i<sides-1;i++){lines.push('f '+last+' '+(last+i)+' '+(last+i+1));faces++;}
}
for(const side of [-1,1]){
 let tag=side<0?'L':'R',x=side*.52;
 // wrist-to-elbow, tapered forged vambrace with ridge
 ringTube(tag+'_forearm_vambrace',[[x,0,1.18],[x,0,1.25],[x,0,1.40],[x,0,1.53]],[[.085,.068],[.102,.078],[.123,.095],[.14,.11]],16);
 for(let k=0;k<4;k++){
  let z=1.23+k*.075;
  ringTube(tag+'_raised_steel_band_'+k,[[x,0,z],[x,0,z+.013]],[[.109+k*.008,.085+k*.007],[.109+k*.008,.085+k*.007]],16);
 }
 // articulated hand palm with beveled segmented metal shell
 ringTube(tag+'_palm',[[x,0,1.155],[x,0,1.08],[x,0,1.025]],[[.080,.056],[.086,.064],[.079,.055]],12);
 for(let f=0;f<4;f++){
  let dx=(f-1.5)*.039,base=x+dx;
  let len=[.090,.108,.104,.087][f],tip=1.022-len;
  ringTube(tag+'_finger_'+f+'_proximal',[[base,0,1.025],[base,0,1.025-len*.52]],[[.019,.020],[.018,.019]],10);
  ringTube(tag+'_finger_'+f+'_distal',[[base,0,1.025-len*.55],[base,0,tip]],[[.017,.018],[.010,.013]],10);
  // knuckle dome, independent future joint
  ringTube(tag+'_knuckle_'+f,[[base,0,1.035],[base,0,1.019]],[[.024,.025],[.024,.025]],10);
 }
 // opposable thumb, distinct from fingers
 let tx=x+side*.086;
 ringTube(tag+'_thumb_proximal',[[x+side*.061,-.020,1.114],[tx,-.043,1.078]],[[.026,.025],[.022,.022]],12);
 ringTube(tag+'_thumb_distal',[[tx,-.043,1.078],[tx+side*.019,-.057,1.043]],[[.020,.021],[.012,.014]],12);
 // three ornamental claw fins at outer edge
 for(let k=0;k<3;k++){
  let z=1.30+k*.082,fx=x+side*(.110+k*.006);
  ringTube(tag+'_outer_spur_'+k,[[fx,0,z],[fx+side*.027,0,z+.016],[fx+side*.042,0,z+.034]],[[.024,.022],[.015,.015],[.003,.004]],8);
 }
}
fs.mkdirSync(require('path').dirname(out),{recursive:true});
fs.writeFileSync(out,lines.join('\n')+'\n');
console.log(JSON.stringify({output:out,vertices,triangles:faces}));
