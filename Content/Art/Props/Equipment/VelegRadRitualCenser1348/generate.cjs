function generate(detail=1){
 const V=[],UV=[],F=[];let mat="blackened_bronze";
 function vert(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
 function quad(a,b,c,d){F.push([a,b,c],[a,c,d])}
 function tube(path,r,sides=8,m="blackened_bronze"){mat=m;let prev=null;for(let j=0;j<path.length;j++){let [x,y,z]=path[j],ring=[];for(let k=0;k<sides;k++){let a=2*Math.PI*k/sides;ring.push(vert(x+r*Math.cos(a),y+r*Math.sin(a),z,k/sides,j/(path.length-1)))}if(prev)for(let k=0;k<sides;k++)quad(prev[k],prev[(k+1)%sides],ring[(k+1)%sides],ring[k]);prev=ring}}
 function torus(cx,cy,cz,R,r,segments=24,sides=8,axis="z"){let rings=[];for(let j=0;j<=segments;j++){let t=j*2*Math.PI/segments,ring=[];for(let k=0;k<=sides;k++){let a=k*2*Math.PI/sides,rad=R+r*Math.cos(a),p=[rad*Math.cos(t),rad*Math.sin(t),r*Math.sin(a)];if(axis==="y")p=[p[0],p[2],p[1]];ring.push(vert(cx+p[0],cy+p[1],cz+p[2],j/segments,k/sides))}rings.push(ring)}for(let j=0;j<segments;j++)for(let k=0;k<sides;k++)quad(rings[j][k],rings[j+1][k],rings[j+1][k+1],rings[j][k+1])}
 const n=detail?32:16,s=detail?8:5;
 // open cage ribs: curved meridians around an ember cavity, not a solid ball
 for(let rib=0;rib<8;rib++){let theta=rib*Math.PI/4,pts=[];for(let j=0;j<=n;j++){let phi=.14+(Math.PI-.28)*j/n,rad=.23*Math.sin(phi);pts.push([rad*Math.cos(theta),rad*Math.sin(theta),.42+.23*Math.cos(phi)])}tube(pts,.012,s)}
 for(let z of [.22,.42,.62])torus(0,0,z,z===.42?.23:.085,.014,detail?32:16,s);
 // hinged lid crown, central finial, base and ash cup
 torus(0,0,.19,.11,.018,detail?32:16,s);torus(0,0,.67,.055,.016,detail?32:16,s);
 tube([[0,0,.65],[0,0,.72],[0,0,.78]],.024,s);
 // hanging chain triplet, visible physical links
 for(let a=0;a<3;a++){let th=a*2*Math.PI/3;let x=.095*Math.cos(th),y=.095*Math.sin(th);for(let j=0;j<5;j++)torus(x,y,.74+j*.095,.036,.007,detail?16:10,s,"y")}
 torus(0,0,1.25,.11,.013,detail?32:16,s,"y");
 // ember cup inside openwork, dark central rim
 torus(0,0,.34,.13,.015,detail?32:16,s);
 let out="# Original VelegRad ritual censer; metres; Z-up; UV per vertex\nmtllib censer.mtl\n";
 V.forEach(p=>out+="v "+p.map(x=>x.toFixed(6)).join(" ")+"\n");UV.forEach(p=>out+="vt "+p.map(x=>x.toFixed(6)).join(" ")+"\n");
 out+="usemtl blackened_bronze\n";F.forEach(f=>out+="f "+f.map(i=>i+"/"+i).join(" ")+"\n");
 return {obj:out,vertices:V.length,triangles:F.length,uv:UV.length,degenerate:F.filter(f=>{let a=V[f[0]-1],b=V[f[1]-1],c=V[f[2]-1];let u=b.map((v,i)=>v-a[i]),w=c.map((v,i)=>v-a[i]);let cr=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]];return Math.hypot(...cr)<1e-10}).length}
}
const fs=require('fs');for(const [name,d] of [['censer_LOD0.obj',1],['censer_LOD1.obj',0]])fs.writeFileSync(name,generate(d).obj);
