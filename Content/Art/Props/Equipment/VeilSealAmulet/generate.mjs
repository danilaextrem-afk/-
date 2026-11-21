// Reproducible procedural source of RZ_EQ_VEIL_SEAL_AMULET v0.1
// Run: node generate.mjs > VeilSealAmulet.obj
const generate = function generate(){
 let v=[],vt=[],f=[],groups={},current="bronze";
 function material(m){current=m;groups[m]??=[]}
 function vertex(p,uv){v.push(p);vt.push(uv);return v.length}
 function tri(a,b,c){groups[current].push([a,b,c])}
 function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
 function ring(cx,cy,z,r0,r1,n,seg=12,phase=0){let rows=[];for(let j=0;j<=seg;j++){let rr=r0+(r1-r0)*j/seg, row=[];for(let i=0;i<=n;i++){let t=2*Math.PI*i/n+phase;row.push(vertex([cx+rr*Math.cos(t),cy+rr*Math.sin(t),z],[i/n,j/seg]))}rows.push(row)}for(let j=0;j<seg;j++)for(let i=0;i<n;i++)quad(rows[j][i],rows[j][i+1],rows[j+1][i+1],rows[j+1][i]);}
 function torus(cx,cy,cz,R,r,N=96,M=12){let rows=[];for(let j=0;j<=M;j++){let a=j*2*Math.PI/M,row=[];for(let i=0;i<=N;i++){let t=i*2*Math.PI/N;row.push(vertex([cx+(R+r*Math.cos(a))*Math.cos(t),cy+(R+r*Math.cos(a))*Math.sin(t),cz+r*Math.sin(a)],[i/N,j/M]))}rows.push(row)}for(let j=0;j<M;j++)for(let i=0;i<N;i++)quad(rows[j][i],rows[j][i+1],rows[j+1][i+1],rows[j+1][i]);}
 function box(cx,cy,cz,sx,sy,sz){let p=[[0,0,0],[1,0,0],[1,1,0],[0,1,0],[0,0,1],[1,0,1],[1,1,1],[0,1,1]].map(q=>vertex([cx+(q[0]-.5)*sx,cy+(q[1]-.5)*sy,cz+(q[2]-.5)*sz],[q[0],q[1]]));for(let [a,b,c,d] of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]])quad(p[a],p[b],p[c],p[d])}
 material("aged_bronze");torus(0,0,0,.055,.0065);torus(0,0,.003,.042,.003);ring(0,0,-.002,0,.049,96,12); // medallion backing
 material("black_iron");torus(0,0,.009,.031,.0025);for(let k=0;k<12;k++){let t=2*Math.PI*k/12;box(.045*Math.cos(t),.045*Math.sin(t),.006,.006,.008,.004)}
 material("oxidized_silver");for(let k=0;k<7;k++){let t=2*Math.PI*k/7;torus(.025*Math.cos(t),.025*Math.sin(t),.012,.0045,.0015,24,8)}torus(0,0,.014,.009,.0025);
 material("red_enamel");ring(0,0,.013,0,.008,48,6);
 material("aged_bronze");torus(0,.061,0,.009,.003,48,12);
 material("black_iron");for(let k=0;k<8;k++){let y=.072+k*.010;torus(0,y,0,.008,.002,24,8)}
 material("oxidized_silver");for(let k=0;k<8;k++){let t=2*Math.PI*k/8;box(.037*Math.cos(t),.037*Math.sin(t),.013,.003,.003,.003)}
 let out=["# Original Veil Seal amulet v0.1; meters; Z thickness; XY face","# Procedural UV mesh. No borrowed geometry.","mtllib VeilSealAmulet.mtl","o RZ_EQ_VEIL_SEAL_AMULET"];
 for(const p of v)out.push("v "+p.map(x=>x.toFixed(6)).join(" "));
 for(const uv of vt)out.push("vt "+uv.map(x=>x.toFixed(6)).join(" "));
 for(const [m,faces] of Object.entries(groups)){out.push("g "+m,"usemtl "+m);for(const face of faces)out.push("f "+face.map(i=>i+"/"+i).join(" "))}
 return {obj:out.join("\n")+"\n",vertices:v.length,triangles:Object.values(groups).reduce((s,x)=>s+x.length,0),groups:Object.fromEntries(Object.entries(groups).map(([k,x])=>[k,x.length]))}
};
process.stdout.write(generate().obj);
