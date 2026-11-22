// Original procedural shutter generator; Node.js 18+; no external packages.
const fs=require('fs');
const generate=function generate(detail=1){
let v=[],vt=[],f=[],groups=[];const addBox=(name,c,s,mat)=>{let [x,y,z]=c,[a,b,d]=s;let pts=[[x-a/2,y-b/2,z-d/2],[x+a/2,y-b/2,z-d/2],[x+a/2,y+b/2,z-d/2],[x-a/2,y+b/2,z-d/2],[x-a/2,y-b/2,z+d/2],[x+a/2,y-b/2,z+d/2],[x+a/2,y+b/2,z+d/2],[x-a/2,y+b/2,z+d/2]];let faces=[[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]];groups.push(`g ${name}\nusemtl ${mat}`);for(let q of faces){let ix=[];for(let k of q){v.push(pts[k]);vt.push([k===q[0]||k===q[3]?0:1,k===q[0]||k===q[1]?0:1]);ix.push(v.length)}f.push([ix[0],ix[1],ix[2]]);f.push([ix[0],ix[2],ix[3]])}};
 // x width, z height, y depth; pivot at hinge x=0
for(let i=0;i<5;i++){let w=.178;addBox("oak_plank_"+i,[.095+i*.18,0,1.04],[w,.065,1.94],"Oak");if(detail)for(let j=0;j<3;j++)addBox(`grain_${i}_${j}`,[.045+i*.18+j*.045,-.034, .32+j*.59],[.008,.003,.39],"Grain")}
for(let z of [.28,1.76]){addBox("crossbar_"+z,[.46,-.075,z],[.91,.075,.14],"Oak");addBox("iron_strap_"+z,[.44,-.119,z],[.85,.012,.048],"Iron");for(let x of [.12,.46,.79])addBox("rivet_"+z+"_"+x,[x,-.131,z],[.025,.014,.025],"Iron")}
for(let z of [.24,1.81]){addBox("hinge_leaf_"+z,[.035,-.135,z],[.12,.04,.22],"Iron");addBox("hinge_pin_"+z,[0,-.12,z],[.026,.07,.29],"Iron")}
addBox("handle_backplate",[.75,-.131,1.02],[.095,.025,.18],"Iron");addBox("handle_grip",[.75,-.175,1.02],[.025,.07,.12],"Iron");
let obj="# Original VelegRad oak shutter, metres, Z up, hinge at x=0\nmtllib shutter.mtl\n";let idx=0;for(let g of groups){obj+=g+"\n";for(let j=0;j<24;j++){obj+=`v ${v[idx+j].map(n=>n.toFixed(6)).join(" ")}\n`;obj+=`vt ${vt[idx+j].join(" ")}\n`;}for(let j=0;j<12;j++){let t=f[(idx/24)*12+j];obj+=`f ${t.map(n=>n+"/"+n).join(" ")}\n`;}idx+=24}
return {obj,vertices:v.length,triangles:f.length,uv:vt.length,valid:f.every(t=>t.every(i=>i>=1&&i<=v.length))&&vt.every(t=>t.every(n=>n>=0&&n<=1))};
};
for(const [name,d] of [['LOD0',1],['LOD1',0]]) {const r=generate(d); if(!r.valid) throw Error('mesh validation'); fs.writeFileSync('shutter_'+name+'.obj',r.obj); console.log(name,r.vertices,r.triangles);}
