// Original procedural helmet generator; run: node generate.cjs
const fs=require('node:fs');
const build=function build(segments=32,rings=10){
 const v=[],uv=[],faces=[];const add=(x,y,z,u,w)=>{v.push([x,y,z]);uv.push([u,w]);return v.length};
 const face=(a,b,c,m)=>faces.push([a,b,c,m]);
 function dome(){const rows=[];for(let j=0;j<=rings;j++){let t=j/rings,theta=t*Math.PI*.49;let rr=.145*Math.cos(theta),y=.08+.18*Math.sin(theta);let row=[];for(let i=0;i<=segments;i++){let a=i/segments*Math.PI*2;row.push(add(rr*Math.cos(a),y,rr*Math.sin(a),i/segments,t))}rows.push(row)}for(let j=0;j<rings;j++)for(let i=0;i<segments;i++){let a=rows[j][i],b=rows[j][i+1],c=rows[j+1][i],d=rows[j+1][i+1];face(a,b,c,"steel");face(b,d,c,"steel")}}
 function band(y0,y1,r,mat){let lower=[],upper=[];for(let i=0;i<=segments;i++){let a=2*Math.PI*i/segments;lower.push(add(r*Math.cos(a),y0,r*Math.sin(a),i/segments,0));upper.push(add(r*Math.cos(a),y1,r*Math.sin(a),i/segments,1))}for(let i=0;i<segments;i++){face(lower[i],lower[i+1],upper[i],mat);face(lower[i+1],upper[i+1],upper[i],mat)}}
 function box(cx,cy,cz,sx,sy,sz,mat){let p=[[0,0,0],[1,0,0],[1,1,0],[0,1,0],[0,0,1],[1,0,1],[1,1,1],[0,1,1]].map(q=>add(cx+(q[0]-.5)*sx,cy+(q[1]-.5)*sy,cz+(q[2]-.5)*sz,q[0],q[1]));for(let f of [[0,1,2,3],[5,4,7,6],[4,0,3,7],[1,5,6,2],[3,2,6,7],[4,5,1,0]]){face(p[f[0]],p[f[1]],p[f[2]],mat);face(p[f[0]],p[f[2]],p[f[3]],mat)}}
 dome();band(.068,.092,.149,"bronze");band(.018,.071,.145,"steel");band(.014,.022,.151,"bronze");
 box(0,-.036,.147,.029,.137,.018,"steel");box(-.133,-.043,.02,.024,.125,.092,"steel");box(.133,-.043,.02,.024,.125,.092,"steel");
 for(let k=0;k<8;k++){let a=k*Math.PI*2/8;box(.152*Math.cos(a),.078,.152*Math.sin(a),.009,.012,.009,"bronze")}
 let obj="# Ratibor helmet, meters, Y-up, authored procedural geometry\nmtllib materials.mtl\n";
 for(let p of v)obj+="v "+p.map(n=>n.toFixed(6)).join(" ")+"\n";for(let p of uv)obj+="vt "+p.map(n=>n.toFixed(6)).join(" ")+"\n";
 let last="";for(let f of faces){if(f[3]!==last){last=f[3];obj+="usemtl "+last+"\n"}obj+="f "+f.slice(0,3).map(n=>n+"/"+n).join(" ")+"\n"}
 let bad=0;for(let f of faces){let a=v[f[0]-1],b=v[f[1]-1],c=v[f[2]-1];let ab=b.map((x,i)=>x-a[i]),ac=c.map((x,i)=>x-a[i]);let cr=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]];if(Math.hypot(...cr)<1e-9)bad++}
 return {obj,vertices:v.length,triangles:faces.length,degenerate:bad};
};
fs.writeFileSync('helmet_LOD0.obj',build(32,12).obj);
fs.writeFileSync('helmet_LOD1.obj',build(16,6).obj);
