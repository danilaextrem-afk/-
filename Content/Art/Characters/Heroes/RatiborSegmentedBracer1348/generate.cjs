// Original procedural mesh: Ratibor segmented forearm bracer, metres, Z-up.
// Run: node generate.cjs > bracer_LOD0.obj
const N=24, rings=12;let lines=['# RZ_CHAR_RATIBOR_BRACER_1348_001','mtllib materials.mtl','o RatiborBracer'];let verts=[],uv=[],faces=[];
function tube(name,z0,z1,r0,r1,material,segments=N){let start=verts.length;for(let k=0;k<2;k++)for(let j=0;j<=segments;j++){let a=2*Math.PI*j/segments,z=k?z1:z0,r=k?r1:r0;verts.push([r*Math.cos(a),r*Math.sin(a),z]);uv.push([j/segments,k]);}for(let j=0;j<segments;j++){let a=start+j+1,b=a+1,c=start+segments+1+j+1,d=c+1;faces.push([a,b,d,material],[a,d,c,material]);}}
tube('leather',0,.33,.078,.11,'Leather');for(let i=0;i<6;i++){let z=.025+i*.052;tube('plate',z,z+.044,.083+i*.0052,.088+i*.0052,'Steel');}tube('rim',0,.015,.086,.088,'Bronze');tube('rim',.315,.33,.116,.118,'Bronze');
lines.push(...verts.map(v=>'v '+v.map(n=>n.toFixed(6)).join(' ')),...uv.map(v=>'vt '+v.map(n=>n.toFixed(6)).join(' ')));
let current='';for(const f of faces){if(f[3]!==current){current=f[3];lines.push('usemtl '+current);}lines.push('f '+f.slice(0,3).map(i=>i+'/'+i).join(' '));}console.log(lines.join('\n'));
