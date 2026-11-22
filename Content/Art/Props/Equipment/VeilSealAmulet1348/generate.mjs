// Original parametric 1348 Veil Seal amulet. Node.js >=18; node generate.mjs
import {writeFileSync} from 'node:fs';
function build(segments){
 const V=[],UV=[],F=[];let groups=[];
 function vert(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length;}
 function face(a,b,c,mat){F.push([a,b,c,mat]);}
 function torus(R,r,z,material,steps=segments,tube=8){
  let ids=[];for(let i=0;i<=steps;i++){let row=[];let a=i*2*Math.PI/steps;
   for(let j=0;j<=tube;j++){let b=j*2*Math.PI/tube;row.push(vert((R+r*Math.cos(b))*Math.cos(a),(R+r*Math.cos(b))*Math.sin(a),z+r*Math.sin(b),i/steps,j/tube));}ids.push(row);}
  for(let i=0;i<steps;i++)for(let j=0;j<tube;j++){let a=ids[i][j],b=ids[i+1][j],c=ids[i+1][j+1],d=ids[i][j+1];face(a,b,c,material);face(a,c,d,material);}
 }
 function disc(R,z0,z1,mat,steps=segments){let top=[],bottom=[];for(let i=0;i<steps;i++){let a=2*Math.PI*i/steps;top.push(vert(R*Math.cos(a),R*Math.sin(a),z1,(Math.cos(a)+1)/2,(Math.sin(a)+1)/2));bottom.push(vert(R*Math.cos(a),R*Math.sin(a),z0,(Math.cos(a)+1)/2,(Math.sin(a)+1)/2));}let ct=vert(0,0,z1,.5,.5),cb=vert(0,0,z0,.5,.5);for(let i=0;i<steps;i++){let j=(i+1)%steps;face(ct,top[i],top[j],mat);face(cb,bottom[j],bottom[i],mat);face(bottom[i],bottom[j],top[j],mat);face(bottom[i],top[j],top[i],mat);}}
 function stroke(points,width,z,mat){for(let i=0;i<points.length-1;i++){let [x,y]=points[i],[X,Y]=points[i+1],dx=X-x,dy=Y-y,L=Math.hypot(dx,dy),nx=-dy/L*width/2,ny=dx/L*width/2;let q=[vert(x+nx,y+ny,z,0,0),vert(x-nx,y-ny,z,1,0),vert(X-nx,Y-ny,z,1,1),vert(X+nx,Y+ny,z,0,1)];face(q[0],q[1],q[2],mat);face(q[0],q[2],q[3],mat);}}
 disc(.038,-.003,.003,'dark_iron');torus(.038,.003,0,'aged_bronze');torus(.029,.0016,.004,'silver_inlay');
 // Seven angular strokes: seven bells / Veil order, asymmetrical cut into central motif.
 for(let i=0;i<7;i++){let a=2*Math.PI*i/7+.13;stroke([[.012*Math.cos(a),.012*Math.sin(a)],[.024*Math.cos(a+.09),.024*Math.sin(a+.09)]],.0018,.0042,'silver_inlay');}
 stroke([[-.013,-.007],[0,.013],[.013,-.007]],.0025,.0045,'silver_inlay');
 torus(.006,.0017,.044,'aged_bronze',Math.max(12,Math.floor(segments/2)),8);
 let out=['# Original Veil Seal Amulet 1348; metres; Z up','mtllib amulet.mtl','o VeilSealAmulet1348'];for(const v of V)out.push('v '+v.join(' '));for(const uv of UV)out.push('vt '+uv.join(' '));let mat='';for(const f of F){if(f[3]!==mat){mat=f[3];out.push('usemtl '+mat)}out.push('f '+f.slice(0,3).map(n=>n+'/'+n).join(' '));}
 return {obj:out.join('\n')+'\n',stats:{vertices:V.length,triangles:F.length,uv:UV.length,invalidFaces:F.filter(f=>f.slice(0,3).some(i=>i<1||i>V.length)).length}};
}
for(const [name,n] of [['LOD0',96],['LOD1',32]]){const x=build(n);writeFileSync(name+'.obj',x.obj);console.log(name,x.stats);}
