// Original procedural crown mesh, B3 Grave-less Prince. Node 18+.
const fs=require('fs'),path=require('path');
function build(n,out){
 const v=[],uv=[],f=[],m=[];const V=(x,y,z,u,w)=>{v.push([x,y,z]);uv.push([u,w]);return v.length};
 const tri=(a,b,c,mat)=>{f.push([a,b,c]);m.push(mat)};
 const quad=(a,b,c,d,mat)=>{tri(a,b,c,mat);tri(a,c,d,mat)};
 for(const [r,z0,z1,mat] of [[.31,0,.12,'iron'],[.32,.12,.18,'bronze'],[.30,.18,.23,'iron']]){
 const rows=[];for(const z of [z0,z1])rows.push(Array.from({length:n+1},(_,i)=>{let t=2*Math.PI*i/n;return V(r*Math.cos(t),r*Math.sin(t),z,i/n,z)}));
 for(let i=0;i<n;i++)quad(rows[0][i],rows[0][i+1],rows[1][i+1],rows[1][i],mat);
 }
 for(let k=0;k<7;k++){let a=2*Math.PI*k/7,lo=a-.16,hi=a+.16,h=.4+.08*Math.sin(k*2.1);
 let p=V(.3*Math.cos(lo),.3*Math.sin(lo),.18,0,0),q=V(.3*Math.cos(hi),.3*Math.sin(hi),.18,1,0),tip=V(.29*Math.cos(a),.29*Math.sin(a),h,.5,1);
 tri(p,q,tip,'iron');tri(q,p,tip,'iron');
 }
 for(let k=0;k<14;k++){let a=2*Math.PI*k/14,cx=.325*Math.cos(a),cy=.325*Math.sin(a),cz=.15;
 let c=V(cx,cy,cz,.5,.5),ring=[];for(let j=0;j<8;j++){let t=2*Math.PI*j/8;ring.push(V(cx+.012*Math.cos(t)*Math.cos(a),cy+.012*Math.cos(t)*Math.sin(a),cz+.012*Math.sin(t),.5+.4*Math.cos(t),.5+.4*Math.sin(t)))}for(let j=0;j<8;j++)tri(c,ring[j],ring[(j+1)%8],'bronze');
 }
 let lines=['mtllib crown.mtl','o RZ_BOSS_B3_GRAVELESS_PRINCE_CROWN',...v.map(p=>'v '+p.join(' ')),...uv.map(p=>'vt '+p.join(' '))],prev='';
 f.forEach((a,i)=>{if(m[i]!==prev){prev=m[i];lines.push('usemtl '+prev)}lines.push('f '+a.map(j=>j+'/'+j).join(' '))});
 fs.writeFileSync(path.join(__dirname,out),lines.join('\n')+'\n');return {vertices:v.length,triangles:f.length,uv:uv.length};
}
if(require.main===module){console.log(JSON.stringify({lod0:build(96,'crown_LOD0.obj'),lod1:build(32,'crown_LOD1.obj')}))}
module.exports={build};
