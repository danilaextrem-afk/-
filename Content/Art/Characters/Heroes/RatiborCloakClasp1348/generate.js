// Original procedural source. Run: node generate.js
const fs=require('fs');
function generate(detail=1){
 const V=[],UV=[],F=[];let mat="iron";
 function vertex(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
 function tri(a,b,c){F.push([a,b,c,mat])}
 function torus(cx,cy,cz,R,r,N,M,m){mat=m;let ids=[];for(let i=0;i<=N;i++){let a=2*Math.PI*i/N,row=[];for(let j=0;j<=M;j++){let b=2*Math.PI*j/M;row.push(vertex(cx+(R+r*Math.cos(b))*Math.cos(a),cy+(R+r*Math.cos(b))*Math.sin(a),cz+r*Math.sin(b),i/N,j/M))}ids.push(row)}for(let i=0;i<N;i++)for(let j=0;j<M;j++){let a=ids[i][j],b=ids[i+1][j],c=ids[i+1][j+1],d=ids[i][j+1];tri(a,b,c);tri(a,c,d)}}
 function cylinder(x,y,z,r,len,N,m){mat=m;let rings=[];for(let k=0;k<2;k++){let row=[];for(let i=0;i<=N;i++){let a=i*2*Math.PI/N;row.push(vertex(x+r*Math.cos(a),y+r*Math.sin(a),z+k*len,i/N,k))}rings.push(row)}for(let i=0;i<N;i++){tri(rings[0][i],rings[1][i],rings[1][i+1]);tri(rings[0][i],rings[1][i+1],rings[0][i+1])}}
 const N=detail?64:24,M=detail?12:6;
 torus(0,0,0,0.095,0.011,N,M,"bronze");
 torus(0,0,0.002,0.074,0.003,N,detail?8:5,"iron");
 for(let k=0;k<8;k++){let a=2*Math.PI*k/8; cylinder(.095*Math.cos(a),.095*Math.sin(a),.009,.008,.006,detail?12:6,"bronze")}
 // Crossbar needle, slender polygonal prism pointing left
 mat="iron";let sections=[[-.132,.008],[-.105,.012],[.094,.011],[.118,.003]];
 for(let k=0;k<sections.length-1;k++){let [x,r]=sections[k],[x2,r2]=sections[k+1];let a=[],b=[];for(let j=0;j<=8;j++){let t=j*Math.PI*2/8;a.push(vertex(x,r*Math.cos(t),.017+r*Math.sin(t),j/8,k/3));b.push(vertex(x2,r2*Math.cos(t),.017+r2*Math.sin(t),j/8,(k+1)/3))}for(let j=0;j<8;j++){tri(a[j],b[j],b[j+1]);tri(a[j],b[j+1],a[j+1])}}
 const obj=["# Original Ratibor cloak clasp 1348; meters","o RatiborCloakClasp",...V.map(p=>"v "+p.map(n=>n.toFixed(6)).join(" ")),...UV.map(p=>"vt "+p.map(n=>n.toFixed(6)).join(" "))];let prev="";for(const [a,b,c,m] of F){if(m!==prev){obj.push("usemtl "+m);prev=m}obj.push("f "+[a,b,c].map(i=>i+"/"+i).join(" "))}return {obj:obj.join("\n")+"\n",vertices:V.length,triangles:F.length,valid:F.every(t=>t.slice(0,3).every(i=>i>=1&&i<=V.length))&&UV.length===V.length,uv:UV.length}
}
for(const [d,name] of [[1,'clasp_LOD0.obj'],[0,'clasp_LOD1.obj']]) fs.writeFileSync(name,generate(d).obj);
