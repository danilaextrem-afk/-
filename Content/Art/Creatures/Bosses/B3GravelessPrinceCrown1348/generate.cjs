// Original procedural mesh source. Paste into Node.js and run: node generate.cjs
const fs=require('node:fs');
function generate(segments=48){
 const V=[],UV=[],F=[]; const add=(x,y,z,u,v)=>{V.push([x,y,z]);UV.push([u,v]);return V.length};
 const tri=(a,b,c,mat)=>F.push([a,b,c,mat]);
 function band(r0,r1,z0,z1,n,mat){let rings=[];for(let j=0;j<2;j++){let ring=[];for(let i=0;i<=n;i++){let t=i/n*Math.PI*2,r=j?r1:r0,z=j?z1:z0;ring.push(add(r*Math.cos(t),r*Math.sin(t),z,i/n,j))}rings.push(ring)}for(let i=0;i<n;i++){tri(rings[0][i],rings[0][i+1],rings[1][i+1],mat);tri(rings[0][i],rings[1][i+1],rings[1][i],mat)}}
 // closed crown wall with thickness, UV cylindrical mapping
 const n=segments, rings=[];for(const [r,z] of [[.255,.02],[.285,.02],[.285,.155],[.255,.155]]){let q=[];for(let i=0;i<=n;i++){let t=2*Math.PI*i/n;q.push(add(r*Math.cos(t),r*Math.sin(t),z,i/n,z/.3))}rings.push(q)}
 for(let k=0;k<4;k++)for(let i=0;i<n;i++){let a=rings[k][i],b=rings[k][i+1],c=rings[(k+1)%4][i+1],d=rings[(k+1)%4][i];tri(a,b,c,k===1?"oxidized_iron":"blackened_silver");tri(a,c,d,k===1?"oxidized_iron":"blackened_silver")}
 // eight individually closed tapered prongs with bent profile and pointed caps
 for(let j=0;j<8;j++){let a=j*2*Math.PI/8;let q=[];for(const [z,r,da] of [[.13,.277,0],[.27,.295,.045],[.41,.265,.09]]){let ring=[];for(let k=0;k<4;k++){let t=a+(k-.5)*.085+da;ring.push(add(r*Math.cos(t),r*Math.sin(t),z,k/3,z/.41))}q.push(ring)}
 for(let k=0;k<2;k++)for(let i=0;i<3;i++){tri(q[k][i],q[k][i+1],q[k+1][i+1],"blackened_silver");tri(q[k][i],q[k+1][i+1],q[k+1][i],"blackened_silver")}
 const tip=add(.258*Math.cos(a+.11),.258*Math.sin(a+.11),.52,.5,1);
 for(let i=0;i<3;i++)tri(q[2][i],q[2][i+1],tip,"blackened_silver");
 }
 // rivet bosses as faceted hemispheres on front-facing side
 for(let j=0;j<16;j++){let a=j*2*Math.PI/16;let center=add(.294*Math.cos(a),.294*Math.sin(a),.095,.5,.5);let ring=[];for(let k=0;k<8;k++){let t=k*Math.PI*2/8;let r=.016;ring.push(add((.291+r*Math.cos(t))*Math.cos(a),(.291+r*Math.cos(t))*Math.sin(a),.095+r*Math.sin(t),k/8,0))}for(let k=0;k<8;k++)tri(center,ring[k],ring[(k+1)%8],"aged_bronze")}
 let out="mtllib Crown.mtl\n";for(const v of V)out+=`v ${v.map(x=>x.toFixed(6)).join(" ")}\n`;for(const uv of UV)out+=`vt ${uv.map(x=>x.toFixed(6)).join(" ")}\n`;
 let last="";for(const [a,b,c,m] of F){if(m!==last){out+=`usemtl ${m}\n`;last=m}out+=`f ${a}/${a} ${b}/${b} ${c}/${c}\n`}
 let deg=0;for(const [a,b,c] of F){let p=V[a-1],q=V[b-1],r=V[c-1],u=q.map((x,i)=>x-p[i]),v=r.map((x,i)=>x-p[i]),cross=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];if(Math.hypot(...cross)<1e-9)deg++}
 return {obj:out,vertices:V.length,triangles:F.length,degenerate:deg};
}
for(const [name,n] of [['Crown_LOD0.obj',64],['Crown_LOD1.obj',24]]){const m=generate(n);fs.writeFileSync(name,m.obj);console.log(name,m.vertices,m.triangles,m.degenerate)}
