// Original procedural ritual dagger, units meters; node generate.cjs
const fs=require('fs');
const parts=[], verts=[],uvs=[],faces=[]; let current='iron';
function addRing(y,rx,rz,n,material,twist=0){const start=verts.length;for(let i=0;i<n;i++){let a=2*Math.PI*i/n+twist;verts.push([rx*Math.cos(a),y,rz*Math.sin(a)]);uvs.push([i/n,y/0.55]);}return start;}
function loft(sections,n,material){const starts=sections.map(s=>addRing(s[0],s[1],s[2],n,material,s[3]||0));for(let j=0;j<starts.length-1;j++)for(let i=0;i<n;i++){let a=starts[j]+i+1,b=starts[j]+(i+1)%n+1,c=starts[j+1]+i+1,d=starts[j+1]+(i+1)%n+1;faces.push([a,b,c,material],[b,d,c,material]);}for(let i=1;i<n-1;i++){faces.push([starts[0]+1,starts[0]+i+2,starts[0]+i+1,material]);let z=starts.at(-1)+1;faces.push([z,z+i+1,z+i+2,material]);}}
function make(detail){const n=detail?24:12; // blade diamond cross-section with tapered distal end
loft([[0.01,.020,.004],[.03,.028,.006],[.12,.035,.006],[.22,.028,.005],[.34,.015,.003],[.41,.0005,.0005]],n,'iron');
loft([[-.018,.075,.017],[0,.073,.017],[.014,.045,.013]],n,'bronze');
loft([[-.175,.017,.017],[-.165,.019,.019],[-.045,.018,.018],[-.02,.017,.017]],n,'leather');
loft([[-.205,.032,.024],[-.185,.035,.027],[-.17,.024,.022]],n,'bronze');
for(let k=0;k<(detail?15:7);k++){let y=-.158+k*.0075;loft([[y,.0195,.0195],[y+.0018,.021,.021],[y+.003,.0195,.0195]],detail?12:8,'leather');}
for(let side of [-1,1])for(let k=0;k<3;k++){let y=-.005+k*.008;let x=side*(.052+k*.003);let st=verts.length;for(let i=0;i<8;i++){let a=i*Math.PI/4;verts.push([x+.003*Math.cos(a),y,.018*Math.sin(a)]);uvs.push([i/8,k/3]);}for(let i=1;i<7;i++)faces.push([st+1,st+i+1,st+i+2,'silver']);}
}
function obj(){let out=['# RAZLOM original procedural dagger, meters','mtllib dagger.mtl','o VeilRitualDagger'];for(let v of verts)out.push('v '+v.map(x=>x.toFixed(6)).join(' '));for(let uv of uvs)out.push('vt '+uv.map(x=>x.toFixed(6)).join(' '));let m='';for(let f of faces){if(f[3]!==m){m=f[3];out.push('usemtl '+m);}out.push('f '+f.slice(0,3).map(i=>i+'/'+i).join(' '));}return out.join('\n')+'\n';}
function build(detail){verts.length=0;uvs.length=0;faces.length=0;make(detail);let output=obj();let stats={vertices:verts.length,triangles:faces.length,uvs:uvs.length};return {output,stats};}
if(require.main===module){for(let d of [true,false]){let r=build(d);fs.writeFileSync(d?'LOD0.obj':'LOD1.obj',r.output);console.log(d?'LOD0':'LOD1',r.stats);}}
module.exports={build};
