'use strict';
const fs=require('node:fs');
function build(){
 const v=[],uv=[],f=[],groups=[];
 const add=(name,verts,faces)=>{const start=v.length;for(const p of verts){v.push(p);uv.push([Math.atan2(p[1],p[0])/(2*Math.PI)+.5,Math.max(0,Math.min(1,(p[2]+.1)/.3))]);}groups.push({name,faces:faces.map(t=>t.map(i=>i+start+1))});};
 const ring=(name,R,r,z,radial=40,tube=12)=>{const vs=[],fs=[];for(let i=0;i<radial;i++)for(let j=0;j<tube;j++){const a=2*Math.PI*i/radial,b=2*Math.PI*j/tube;vs.push([(R+r*Math.cos(b))*Math.cos(a),(R+r*Math.cos(b))*Math.sin(a),z+r*Math.sin(b)]);}for(let i=0;i<radial;i++)for(let j=0;j<tube;j++){const a=i*tube+j,b=((i+1)%radial)*tube+j,c=((i+1)%radial)*tube+(j+1)%tube,d=i*tube+(j+1)%tube;fs.push([a,b,c],[a,c,d]);}add(name,vs,fs);};
 const band=(name,r1,r2,z1,z2,N=48)=>{const vs=[],fs=[];for(const z of [z1,z2])for(const r of [r1,r2])for(let i=0;i<N;i++){const a=2*Math.PI*i/N;vs.push([r*Math.cos(a),r*Math.sin(a),z]);}for(let layer=0;layer<4;layer++)for(let i=0;i<N;i++){const ni=(i+1)%N;const a=layer*N+i,b=layer*N+ni,c=((layer+1)%4)*N+ni,d=((layer+1)%4)*N+i;fs.push([a,b,c],[a,c,d]);}add(name,vs,fs);};
 const bolt=(name,x,y,z)=>{const vs=[],fs=[],N=12;for(let k=0;k<2;k++)for(let i=0;i<N;i++){const a=i*2*Math.PI/N;vs.push([x+.012*Math.cos(a),y+.012*Math.sin(a),z+k*.009]);}for(let i=0;i<N;i++){const j=(i+1)%N;fs.push([i,j,N+j],[i,N+j,N+i]);}add(name,vs,fs);};
 band('forged_cuff_body',.060,.072,-.055,.055);ring('upper_beaded_rim',.072,.008,.055);ring('lower_beaded_rim',.072,.008,-.055);ring('socket_eye',.093,.016,.104,36,12);
 for(let i=0;i<8;i++){const a=2*Math.PI*i/8;bolt('eight_iron_rivets_'+i,.077*Math.cos(a),.077*Math.sin(a),.028);}
 for(let i=0;i<8;i++){const a=2*Math.PI*i/8;bolt('lower_rivets_'+i,.077*Math.cos(a),.077*Math.sin(a),-.027);}
 ring('center_ridge',.073,.005,0);
 const lines=['# RAZLOM Watcher hand grip forged socket cuff v1 - original geometry, metres, Z up','mtllib SM_Watcher_Flail_GripSocket_v1.mtl'];
 for(const p of v)lines.push('v '+p.map(x=>x.toFixed(6)).join(' '));for(const t of uv)lines.push('vt '+t.map(x=>x.toFixed(6)).join(' '));for(const g of groups){lines.push('g '+g.name,'usemtl ForgedIron');for(const t of g.faces)lines.push('f '+t.map(i=>i+'/'+i).join(' '));}
 return {obj:lines.join('\n')+'\n',vertices:v.length,triangles:groups.reduce((a,g)=>a+g.faces.length,0),groups:groups.length};
}
if(require.main===module){const x=build();fs.writeFileSync(process.argv[2]||'SM_Watcher_Flail_GripSocket_v1.obj',x.obj);console.log(x.vertices,x.triangles,x.groups);}
module.exports={build};
