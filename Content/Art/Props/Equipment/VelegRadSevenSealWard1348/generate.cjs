function buildAsset(){
 const V=[],UV=[],F=[],M=[];let mat="bronze";
 function v(x,y,z,u,w){V.push([x,y,z]);UV.push([u,w]);return V.length}
 function tri(a,b,c){F.push([a,b,c]);M.push(mat)}
 function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
 function tube(cx,cy,cz,R,r,seg=48,side=10,material="bronze",offset=0){mat=material;const rows=[];for(let i=0;i<=seg;i++){let t=2*Math.PI*i/seg+offset,row=[];for(let j=0;j<=side;j++){let p=2*Math.PI*j/side;row.push(v(cx+(R+r*Math.cos(p))*Math.cos(t),cy+(R+r*Math.cos(p))*Math.sin(t),cz+r*Math.sin(p),i/seg,j/side))}rows.push(row)}for(let i=0;i<seg;i++)for(let j=0;j<side;j++)quad(rows[i][j],rows[i+1][j],rows[i+1][j+1],rows[i][j+1])}
 function prism(points,depth,material){mat=material;let a=points.map(([x,y])=>v(x,y,-depth/2,(x+0.6)/1.2,(y+0.6)/1.2)),b=points.map(([x,y])=>v(x,y,depth/2,(x+0.6)/1.2,(y+0.6)/1.2));for(let i=1;i<points.length-1;i++){tri(a[0],a[i+1],a[i]);tri(b[0],b[i],b[i+1])}for(let i=0;i<points.length;i++){let j=(i+1)%points.length;quad(a[i],a[j],b[j],b[i])}}
 tube(0,0,0,0.43,0.055,64,12,"bronze");tube(0,0,0,0.315,0.022,64,8,"dark_iron");
 for(let i=0;i<7;i++){let a=2*Math.PI*i/7+Math.PI/2,ux=Math.cos(a),uy=Math.sin(a),vx=-uy,vy=ux;let pts=[[.35,-.095],[.49,-.072],[.64,0],[.49,.072],[.35,.095]].map(([r,t])=>[r*ux+t*vx,r*uy+t*vy]);prism(pts,.07,"bronze");tube(.49*ux,.49*uy,.045,.023,.011,12,6,"dark_iron")}
 prism(Array.from({length:12},(_,i)=>{let a=i*Math.PI/6;return [.19*Math.cos(a),.19*Math.sin(a)]}),.12,"dark_iron");
 prism(Array.from({length:8},(_,i)=>{let a=i*Math.PI/4+Math.PI/8;return [.13*Math.cos(a),.13*Math.sin(a)]}),.16,"garnet");
 for(let i=0;i<7;i++){let a=i*2*Math.PI/7,ux=Math.cos(a),uy=Math.sin(a);let p=[[.225,-.012],[.28,-.012],[.3,0],[.28,.012],[.225,.012]].map(([r,t])=>[r*ux-t*uy,r*uy+t*ux]);prism(p,.145,"silver")}
 tube(0,.72,0,.075,.022,28,8,"dark_iron");
 let out="mtllib ward.mtl\n# meters; XY facing front; Z thickness\n";let last="";V.forEach(p=>out+="v "+p.map(n=>n.toFixed(6)).join(" ")+"\n");UV.forEach(p=>out+="vt "+p.map(n=>n.toFixed(6)).join(" ")+"\n");F.forEach((f,i)=>{if(M[i]!==last){last=M[i];out+="usemtl "+last+"\n"}out+="f "+f.map(j=>j+"/"+j).join(" ")+"\n"});
 let deg=0;for(let f of F){let [a,b,c]=f.map(j=>V[j-1]),ab=b.map((n,i)=>n-a[i]),ac=c.map((n,i)=>n-a[i]);let q=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]];if(Math.hypot(...q)<1e-10)deg++}
 return {obj:out,vertices:V.length,triangles:F.length,degenerate:deg}
}
const asset=buildAsset();if(typeof require!=='undefined'){require('fs').writeFileSync('ward_LOD0.obj',asset.obj);console.log(asset.vertices,asset.triangles,asset.degenerate)}
