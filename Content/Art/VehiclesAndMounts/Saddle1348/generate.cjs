// Original procedural medieval riding saddle; Node.js >=18, no dependencies.
const fs=require('fs');const path=require('path');
const out=__dirname;
function build(level){const n=level?12:32,verts=[],uv=[],faces=[];let mat='leather';
function ring(y,rx,rz,z,offset=0){const ids=[];for(let i=0;i<n;i++){let a=2*Math.PI*i/n;verts.push([Math.cos(a)*rx,y,Math.sin(a)*rz+z+offset*Math.cos(a*2)]);uv.push([i/n,(y+0.6)/1.2]);ids.push(verts.length)}return ids}
function bridge(a,b,m){for(let i=0;i<n;i++){let j=(i+1)%n;faces.push([a[i],b[i],b[j],m],[a[i],b[j],a[j],m])}}
const layers=[ring(-.32,.36,.19,.19),ring(-.17,.43,.23,.26),ring(.05,.45,.22,.24),ring(.29,.36,.19,.31)];for(let i=0;i<3;i++)bridge(layers[i],layers[i+1],'leather');
function tube(points,r,m){for(let k=0;k<points.length-1;k++){const [x,y,z]=points[k], [X,Y,Z]=points[k+1];let a=[],b=[];for(let j=0;j<8;j++){let t=j*Math.PI/4;verts.push([x+r*Math.cos(t),y+r*Math.sin(t),z]);uv.push([j/8,k/(points.length-1)]);a.push(verts.length);verts.push([X+r*Math.cos(t),Y+r*Math.sin(t),Z]);uv.push([j/8,(k+1)/(points.length-1)]);b.push(verts.length)}for(let j=0;j<8;j++){let q=(j+1)%8;faces.push([a[j],b[j],b[q],m],[a[j],b[q],a[q],m])}}}
tube([[-.42,-.19,.29],[-.53,-.19,.04],[-.51,-.19,-.34]],.022,'strap');tube([[.42,-.19,.29],[.53,-.19,.04],[.51,-.19,-.34]],.022,'strap');
tube([[-.35,.26,.36],[0,.32,.43],[.35,.26,.36]],.035,'wood');tube([[-.35,-.30,.36],[0,-.37,.44],[.35,-.30,.36]],.033,'wood');
tube([[-.53,-.19,-.32],[-.63,-.18,-.38],[-.65,-.18,-.18]],.019,'iron');tube([[.53,-.19,-.32],[.63,-.18,-.38],[.65,-.18,-.18]],.019,'iron');
let s='# Original RAZLOM saddle, metres, Z-up; UV per vertex\\n';for(let v of verts)s+='v '+v.join(' ')+'\\n';for(let u of uv)s+='vt '+u.join(' ')+'\\n';let last='';for(let f of faces){if(f[3]!==last){s+='usemtl '+f[3]+'\\n';last=f[3]}s+='f '+f.slice(0,3).map(i=>i+'/'+i).join(' ')+'\\n'}fs.writeFileSync(path.join(out,'SM_Saddle1348_LOD'+level+'.obj'),s);return {vertices:verts.length,triangles:faces.length,uv:uv.length,invalidFaces:faces.filter(f=>f.slice(0,3).some(i=>i<1||i>verts.length)).length}}
const lod0=build(0),lod1=build(1);fs.writeFileSync(path.join(out,'qa_generated.json'),JSON.stringify({lod0,lod1},null,2));console.log(lod0,lod1);
