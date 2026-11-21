// Original procedural 3D asset. Run with Node.js: node generate.js > StairwellCageLamp.obj
const build = function build(){
 let v=[],uv=[],f=[],mtl="";const N=32;
 const add=(p,t)=>{v.push(p);uv.push(t);return v.length};
 const tri=(a,b,c,mat)=>f.push({a,b,c,mat});
 const quad=(a,b,c,d,mat)=>{tri(a,b,c,mat);tri(a,c,d,mat)};
 function ring(z,r,mat,phase=0,cx=0,cy=0){let ids=[];for(let i=0;i<N;i++){let t=2*Math.PI*i/N+phase;ids.push(add([cx+r*Math.cos(t),cy+r*Math.sin(t),z],[i/N,z/3]));}return ids}
 function bridge(a,b,mat){for(let i=0;i<N;i++)quad(a[i],a[(i+1)%N],b[(i+1)%N],b[i],mat)}
 function lathe(profile,mat){let last=null;for(const [z,r] of profile){let next=ring(z,r);if(last)bridge(last,next,mat);last=next}}
 function rod(a,b,r,mat,segments=10){let dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],l=Math.hypot(dx,dy,dz),ux=-dy/(Math.hypot(dx,dy)||1),uy=dx/(Math.hypot(dx,dy)||1),uz=0,vx=dy*uz-dz*uy/l,vy=dz*ux/l-dx*uz,vz=dx*uy/l-dy*ux/l;let aa=[],bb=[];for(let i=0;i<segments;i++){let t=i*2*Math.PI/segments;let off=[r*(ux*Math.cos(t)+vx*Math.sin(t)),r*(uy*Math.cos(t)+vy*Math.sin(t)),r*(uz*Math.cos(t)+vz*Math.sin(t))];aa.push(add(a.map((q,j)=>q+off[j]),[i/segments,0]));bb.push(add(b.map((q,j)=>q+off[j]),[i/segments,1]))}for(let i=0;i<segments;i++)quad(aa[i],aa[(i+1)%segments],bb[(i+1)%segments],bb[i],mat)}
 // ceiling mount, porcelain socket, spun enamel shade with thick rim
 lathe([[2.62,.055],[2.64,.105],[2.68,.105],[2.70,.075]],"black_metal");
 lathe([[2.38,.055],[2.44,.065],[2.52,.065],[2.54,.050]],"ceramic");
 rod([0,0,2.53],[0,0,2.64],.018,"black_metal");
 lathe([[2.37,.095],[2.39,.115],[2.40,.16],[2.38,.22],[2.34,.30],[2.28,.42],[2.24,.50],[2.23,.51]],"enamel");
 lathe([[2.225,.51],[2.228,.505],[2.245,.49],[2.29,.41],[2.35,.285],[2.39,.17]],"enamel_inside");
 lathe([[2.225,.51],[2.225,.525],[2.242,.525],[2.245,.49]],"black_metal");
 // exposed frosted globe and tungsten filament
 lathe([[2.08,.025],[2.095,.11],[2.13,.155],[2.18,.165],[2.225,.145],[2.27,.09],[2.29,.045]],"glass");
 rod([-.018,0,2.18],[.018,0,2.18],.004,"filament",8);
 // steel protective cage: circular guards and vertical ribs
 for(let z of [2.06,2.17,2.235]){for(let i=0;i<16;i++){let a=i*2*Math.PI/16,b=(i+1)*2*Math.PI/16;rod([.35*Math.cos(a),.35*Math.sin(a),z],[.35*Math.cos(b),.35*Math.sin(b),z],.009,"black_metal",6)}}
 for(let i=0;i<12;i++){let a=i*2*Math.PI/12;rod([.35*Math.cos(a),.35*Math.sin(a),2.06],[.35*Math.cos(a),.35*Math.sin(a),2.235],.011,"black_metal",7)}
 // corroded fasteners on rim
 for(let i=0;i<8;i++){let a=i*2*Math.PI/8;rod([.507*Math.cos(a),.507*Math.sin(a),2.232],[.512*Math.cos(a),.512*Math.sin(a),2.245],.018,"rust",8)}
 let out=["# Original procedural mesh: RZ_PROP_STAIRWELL_CAGE_LAMP_2026_v01","# Units meters; Z up; triangulated faces; UV per vertex","mtllib StairwellCageLamp.mtl"];
 v.forEach(p=>out.push("v "+p.map(x=>x.toFixed(6)).join(" ")));uv.forEach(p=>out.push("vt "+p.map(x=>x.toFixed(6)).join(" ")));
 let prev="";for(let q of f){if(q.mat!==prev){out.push("usemtl "+q.mat);prev=q.mat}out.push("f "+[q.a,q.b,q.c].map(i=>`${i}/${i}`).join(" "))}
 return {obj:out.join("\n")+"\n",vertices:v.length,triangles:f.length,materials:[...new Set(f.map(x=>x.mat))],bounds:{height_m:2.7,diameter_m:1.05}};
};
process.stdout.write(build().obj);
