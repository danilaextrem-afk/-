// Original parametric Ratibor belt buckle; Node.js 18+, no dependencies.
const fs=require('node:fs');
function build(n=64){let v=[],uv=[],f=[];function vert(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length}
function tri(a,b,c,m){f.push([a,b,c,m])}
function ring(cx,cy,rx,ry,thick,depth,mat){let layers=[];for(let z of [-depth/2,depth/2])for(let radius of [1,1-thick]){let row=[];for(let i=0;i<n;i++){let a=2*Math.PI*i/n;row.push(vert(cx+rx*radius*Math.cos(a),cy+ry*radius*Math.sin(a),z,i/n,(z/depth+.5)*.5+(1-radius)*.5))}layers.push(row)}
for(let i=0;i<n;i++){let j=(i+1)%n;for(let [a,b,m] of [[0,1,mat],[2,3,mat],[0,2,mat],[1,3,mat]]){tri(layers[a][i],layers[b][i],layers[b][j],m);tri(layers[a][i],layers[b][j],layers[a][j],m)}}}
function rod(x0,y0,x1,y1,r,z,mat){let sides=8,aa=[],bb=[];for(let i=0;i<sides;i++){let a=2*Math.PI*i/sides;aa.push(vert(x0+r*Math.cos(a),y0+r*Math.sin(a),z+r*Math.cos(a),i/sides,0));bb.push(vert(x1+r*Math.cos(a),y1+r*Math.sin(a),z+r*Math.cos(a),i/sides,1))}
for(let i=0;i<sides;i++){let j=(i+1)%sides;tri(aa[i],bb[i],bb[j],mat);tri(aa[i],bb[j],aa[j],mat)}}
ring(0,0,.055,.042,.22,.008,'aged_bronze');rod(-.047,0,.037,0,.004,.008,'iron');rod(.028,0,.028,-.024,.003,.01,'iron');
for(let i=0;i<12;i++){let a=i*Math.PI/6;let x=.045*Math.cos(a),y=.034*Math.sin(a);rod(x,y,x*.90,y*.90,.0015,.008,'silver_inlay')}
let lines=['mtllib materials.mtl','o RatiborBeltBuckle'];v.forEach(p=>lines.push('v '+p.map(x=>x.toFixed(6)).join(' ')));uv.forEach(p=>lines.push('vt '+p.map(x=>x.toFixed(6)).join(' ')));let last='';for(let [a,b,c,m] of f){if(m!==last){lines.push('usemtl '+m);last=m}lines.push('f '+[a,b,c].map(i=>i+'/'+i).join(' '))}
return {obj:lines.join('\n')+'\n',vertices:v.length,triangles:f.length,uv:uv.length}}
for(let [name,n] of [['LOD0',64],['LOD1',24]]){let out=build(n);fs.writeFileSync(name+'.obj',out.obj);console.log(name,out.vertices,out.triangles)}
