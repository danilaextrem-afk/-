// Original procedural mesh, Node.js 18+. Run: node generate.cjs > VeilWardAmulet_LOD0.obj
const generate = function generate(segments=48){
 const V=[],UV=[],F=[];let mat="bronze";
 function vertex(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
 function face(a,b,c,m){F.push([a,b,c,m])}
 function tube(cx,cy,r,thick,depth,n,m){let start=V.length;for(let i=0;i<n;i++){let a=2*Math.PI*i/n;for(let j=0;j<8;j++){let t=2*Math.PI*j/8;vertex(cx+(r+thick*Math.cos(t))*Math.cos(a),cy+(r+thick*Math.cos(t))*Math.sin(a),depth+thick*Math.sin(t),i/n,j/8)}}for(let i=0;i<n;i++)for(let j=0;j<8;j++){let a=start+i*8+j+1,b=start+((i+1)%n)*8+j+1,c=start+((i+1)%n)*8+(j+1)%8+1,d=start+i*8+(j+1)%8+1;face(a,b,c,m);face(a,c,d,m)}}
 function bar(ax,ay,bx,by,width,z,m){let dx=bx-ax,dy=by-ay,L=Math.hypot(dx,dy),nx=-dy/L*width/2,ny=dx/L*width/2;let p=[[ax+nx,ay+ny],[bx+nx,by+ny],[bx-nx,by-ny],[ax-nx,ay-ny]];let base=V.length;for(let zz of [z-.025,z+.025])for(let q of p)vertex(q[0],q[1],zz,(q[0]+1)/2,(q[1]+1)/2);for(let [a,b,c,d] of [[0,1,2,3],[4,7,6,5],[0,4,5,1],[1,5,6,2],[2,6,7,3],[3,7,4,0]]){face(base+a+1,base+b+1,base+c+1,m);face(base+a+1,base+c+1,base+d+1,m)}}
 tube(0,0,.47,.065,0,segments,"aged_bronze");tube(0,0,.34,.022,.005,segments,"dark_silver");tube(0,.56,.09,.035,0,24,"aged_bronze");
 for(let k=0;k<7;k++){let a=k*2*Math.PI/7;bar(.18*Math.cos(a),.18*Math.sin(a),.32*Math.cos(a),.32*Math.sin(a),.065,.02,"dark_silver")}
 for(let k=0;k<7;k++){let a=k*2*Math.PI/7;bar(.39*Math.cos(a),.39*Math.sin(a),.43*Math.cos(a),.43*Math.sin(a),.04,.06,"bone_inlay")}
 bar(-.14,-.16,.14,.16,.055,.055,"bone_inlay");bar(.14,-.16,-.14,.16,.055,.055,"bone_inlay");
 const lines=["mtllib VeilWardAmulet.mtl","o VeilWardAmulet1348_LOD0",...V.map(v=>"v "+v.map(x=>x.toFixed(6)).join(" ")),...UV.map(v=>"vt "+v.map(x=>x.toFixed(6)).join(" "))];let last="";for(let f of F){if(f[3]!==last){last=f[3];lines.push("usemtl "+last)}lines.push("f "+f.slice(0,3).map(i=>i+"/"+i).join(" "))}
 return {obj:lines.join("\n")+"\n",vertices:V.length,triangles:F.length,materials:[...new Set(F.map(f=>f[3]))],valid:F.every(f=>f.slice(0,3).every(i=>i>0&&i<=V.length))};
};
process.stdout.write(generate().obj);
