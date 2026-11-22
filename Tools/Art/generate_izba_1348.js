// Procedural original RAZLOM 1348 village izba kit; run with Node 18+.
const fs=require('node:fs'),path=require('node:path');
function mesh(kind,lod){
 const high=lod===0,pi=Math.PI,F=[],mats=new Set();
 const r=n=>+n.toFixed(5);
 function t(a,b,c,m,g,u=[[0,0],[1,0],[0,1]]){const A=b.map((x,i)=>x-a[i]),B=c.map((x,i)=>x-a[i]),N=[A[1]*B[2]-A[2]*B[1],A[2]*B[0]-A[0]*B[2],A[0]*B[1]-A[1]*B[0]],L=Math.hypot(...N);if(L<1e-9)throw Error("Degenerate "+g);F.push({p:[a,b,c].map(x=>x.map(r)),uv:u,n:N.map(x=>r(x/L)),m,g});mats.add(m)}
 function q(a,b,c,d,m,g){t(a,b,c,m,g);t(a,c,d,m,g,[[0,0],[0,1],[1,1]])}
 function box(x,y,z,dx,dy,dz,m,g){const a=[x-dx/2,y-dy/2,z-dz/2],b=[x+dx/2,y+dy/2,z+dz/2];const V=[[a[0],a[1],a[2]],[b[0],a[1],a[2]],[b[0],b[1],a[2]],[a[0],b[1],a[2]],[a[0],a[1],b[2]],[b[0],a[1],b[2]],[b[0],b[1],b[2]],[a[0],b[1],b[2]]];for(const ids of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]])q(...ids.map(i=>V[i]),m,g)}
 function tube(a,b,R,m,g,N=high?12:6){
 const vec=b.map((x,i)=>x-a[i]),l=Math.hypot(...vec),axis=vec.map(x=>x/l);
 const v=Math.abs(axis[2])<.85?[0,0,1]:[0,1,0];
 let U=[axis[1]*v[2]-axis[2]*v[1],axis[2]*v[0]-axis[0]*v[2],axis[0]*v[1]-axis[1]*v[0]],k=Math.hypot(...U);U=U.map(x=>x/k);const W=[axis[1]*U[2]-axis[2]*U[1],axis[2]*U[0]-axis[0]*U[2],axis[0]*U[1]-axis[1]*U[0]],at=(c,theta)=>c.map((x,i)=>x+R*(U[i]*Math.cos(theta)+W[i]*Math.sin(theta)));
 for(let i=0;i<N;i++){let T=2*pi*i/N,V=2*pi*(i+1)/N,p=at(a,T),n=at(a,V),r=at(b,V),s=at(b,T);q(p,n,r,s,m,g);t(a,n,p,m,g);t(b,s,r,m,g)}}
 function log(x0,x1,z,idx){const n=high?12:7,steps=high?3:1,R=.123;
 const pos=(x,a,k)=>[x,R*(1+.015*Math.sin(idx*2.91+k+.4))*Math.cos(a),z+R*(1+.015*Math.cos(idx*1.9+k))*Math.sin(a)];
 for(let k=0;k<steps;k++){let x=x0+(x1-x0)*k/steps,y=x0+(x1-x0)*(k+1)/steps;
 for(let j=0;j<n;j++){let a=2*pi*j/n,b=2*pi*(j+1)/n;q(pos(x,a,k),pos(x,b,k),pos(y,b,k+1),pos(y,a,k+1),"Timber_Log","log_course_"+idx)}}
 for(let j=0;j<n;j++){let a=2*pi*j/n,b=2*pi*(j+1)/n;t([x0,0,z],pos(x0,b,0),pos(x0,a,0),"Timber_Endgrain","sawn_end");t([x1,0,z],pos(x1,a,steps),pos(x1,b,steps),"Timber_Endgrain","sawn_end")}}
 function angled(a,b,w,h,m,g){const D=b.map((x,i)=>x-a[i]),L=Math.hypot(...D),X=D.map(x=>x/L),S=Math.abs(X[2])<.8?[0,0,1]:[0,1,0];let Y=[X[1]*S[2]-X[2]*S[1],X[2]*S[0]-X[0]*S[2],X[0]*S[1]-X[1]*S[0]],size=Math.hypot(...Y);Y=Y.map(x=>x/size);const Z=[X[1]*Y[2]-X[2]*Y[1],X[2]*Y[0]-X[0]*Y[2],X[0]*Y[1]-X[1]*Y[0]],at=(p,s,t)=>p.map((x,k)=>x+s*Y[k]*w/2+t*Z[k]*h/2);
 const v=[at(a,-1,-1),at(a,1,-1),at(a,1,1),at(a,-1,1),at(b,-1,-1),at(b,1,-1),at(b,1,1),at(b,-1,1)];for(const f of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]])q(...f.map(i=>v[i]),m,g)}
 if(["WallSolid","WallWindow","WallDoor"].includes(kind)){
  for(let i=0;i<11;i++){const z=.125+i*.245;let gap=null;
   if(kind==="WallWindow"&&i>=4&&i<=8)gap=[1.23,2.77];
   if(kind==="WallDoor"&&i<=8)gap=[1.42,2.58];
   if(gap){log(0,gap[0]-.036,z,i);log(gap[1]+.036,4,z,i);}else log(0,4,z,i);
   if(i<10){const segments=gap?[[0,gap[0]-.036],[gap[1]+.036,4]]:[[0,4]];for(const [a,b] of segments)box((a+b)/2,-.124,z+.115,b-a,.01,.008,"Chinking_Dark","log_joint_filling")}
   if(high&&!gap&&i%2===0)box(.35+(i%4)*.72,-.13,z,.23,.008,.008,"Chinking_Dark","weather_check");
  }
  if(kind==="WallWindow"){for(const x of [1.175,2.825])box(x,0,1.6,.11,.30,1.27,"Timber_Trim","cutout_window_jamb");box(2,0,.965,1.66,.33,.10,"Timber_Trim","deep_window_sill");box(2,0,2.24,1.66,.33,.115,"Timber_Trim","lintel");}
  if(kind==="WallDoor"){for(const x of [1.37,2.63])box(x,0,1.08,.12,.30,2.16,"Timber_Trim","door_jamb");box(2,0,2.246,1.5,.33,.11,"Timber_Trim","lintel");box(2,0,.035,1.26,.30,.07,"Timber_Trim","stone_wood_threshold")}
 }
 if(kind==="CornerPost"){box(0,0,1.35,.34,.34,2.70,"Timber_Trim","corner_mortised_post");for(const z of [.23,2.39]){box(0,0,z,.36,.36,.09,"Timber_Log","mortise_collar");if(high)tube([0,-.185,z],[0,-.17,z],.022,"Iron_Forged","peg_collar",10)}}
 if(kind==="Floor4m"){const n=high?20:10;for(let i=0;i<n;i++){const w=4/n,mid=(i+.5)*w;box(mid,2,-.045,w-.009,3.98,.09,"Timber_Plank","planed_floorboard");if(high)for(let j=0;j<3;j++)box(mid-.065,.6+j*1.15,.0005,.031,.12,.003,"Chinking_Dark","aged_knot_and_grain")};for(const x of [.22,3.78])box(x,2,-.135,.2,3.9,.16,"Timber_Trim","floor_joist")}
 if(kind==="RoofHalf"){
  const z=y=>.9-y*.9/2.6;
  for(const x of [high?.17:.20,2,high?3.83:3.80])angled([x,0,z(0)-.1],[x,2.6,z(2.6)-.1],.12,.18,"Timber_Trim","sloped_rafter");
  const rows=high?15:8,cols=high?13:7;
  for(let i=0;i<rows;i++)for(let j=0;j<cols;j++){const y0=i*2.6/rows-.019,y1=(i+1)*2.6/rows+.025,x0=j*4/cols+.012,x1=(j+1)*4/cols-.009,delta=high?.008*Math.sin(j*4.25+i*1.3):0;
   const A=[x0,y0,z(y0)+.03+delta],B=[x1,y0,z(y0)+.03+delta],C=[x1,y1,z(y1)+.03+delta],D=[x0,y1,z(y1)+.03+delta],
   E=[x0,y0,A[2]-.027],FF=[x1,y0,B[2]-.027],G=[x1,y1,C[2]-.027],H=[x0,y1,D[2]-.027];
   for(const v of [[A,B,C,D],[E,H,G,FF],[A,E,FF,B],[D,C,G,H],[A,D,H,E],[B,FF,G,C]])q(...v,"Shingle_Wood","overlapping_oak_shingle");
  }
  angled([0,.02,.92],[4,.02,.92],.16,.08,"Timber_Trim","ridge_board");
 }
 if(kind==="DoorLeaf"){const n=high?5:4;for(let j=0;j<n;j++)box((j+.5)/n,-.03,1.025,1/n-.009,.081,2.05,"Timber_Plank","vertical_door_plank");
   for(const z of [.29,1.69]){box(.5,-.082,z,.96,.054,.13,"Timber_Trim","door_batten");box(.5,-.114,z,.95,.012,.04,"Iron_Forged","forged_iron_strap");}
   angled([.07,-.09,.41],[.93,-.09,1.58],.11,.06,"Timber_Trim","angled_door_brace");
   for(const z of [.35,1.68]){box(.035,.04,z,.07,.1,.13,"Iron_Forged","hinge");tube([.016,.075,z-.08],[.016,.075,z+.08],.024,"Iron_Forged","forged_hinge_pin",high?12:6);}
   tube([.87,-.14,.96],[.98,-.14,.96],.011,"Iron_Forged","iron_pull",high?12:6)}
 if(kind==="Table"){const n=high?4:2;for(let j=0;j<n;j++)box(0,(j+.5)*.78/n-.39,.74,1.55,.78/n-.009,.06,"Timber_Plank","hand_planed_table_top");
   for(const x of [-.66,.66])for(const y of [-.28,.28]){box(x,y,.35,.09,.09,.70,"Timber_Trim","tapered_table_leg");if(high)tube([x,y,.77],[x,y,.783],.013,"Timber_Endgrain","wooden_dowel",8)}
   for(const y of [-.28,.28])box(0,y,.56,1.35,.056,.078,"Timber_Trim","table_rails");for(const x of [-.66,.66])box(x,0,.56,.06,.56,.079,"Timber_Trim","table_cross_rail");
   box(0,0,.18,1.34,.07,.059,"Timber_Trim","lower_stretcher")}
 if(kind==="Bench"){const n=high?2:1;for(let j=0;j<n;j++)box(0,(j+.5)*.37/n-.185,.46,1.56,.37/n-.008,.07,"Timber_Plank","bench_board");
    for(const x of [-.58,.58])for(const y of [-.115,.115])box(x,y,.22,.082,.082,.44,"Timber_Trim","bench_leg");
    for(const y of [-.11,.11])box(0,y,.26,1.16,.06,.06,"Timber_Trim","bench_rail");for(const x of [-.58,.58])box(x,0,.41,.07,.33,.055,"Timber_Trim","bench_crossbeam");
    if(high)for(const x of [-.53,.53])for(const y of [-.10,.10])tube([x,y,.495],[x,y,.507],.01,"Iron_Forged","bench_peg",8)}
 if(F.length===0)throw Error("Empty asset "+kind);
 const bounds={min:[Infinity,Infinity,Infinity],max:[-Infinity,-Infinity,-Infinity]};
 for(const f of F)for(const p of f.p)for(let i=0;i<3;i++){bounds.min[i]=Math.min(bounds.min[i],p[i]);bounds.max[i]=Math.max(bounds.max[i],p[i]);}
 return {faces:F,stats:{triangles:F.length,bounds,materials:[...mats].sort()}};
}
function newUV(P,N,group){const log=group.match(/^log_course_(\d+)$/),scale=1.8;if(log){const z=.125+Number(log[1])*.245,vals=P.map(p=>(Math.atan2(p[2]-z,p[1])+Math.PI)/(2*Math.PI));const seam=Math.max(...vals)-Math.min(...vals)>.5;return P.map((p,i)=>[p[0]*1.6,vals[i]+(seam&&vals[i]<.5?1:0)].map(x=>+x.toFixed(5)))}const abs=N.map(Math.abs),j=abs.indexOf(Math.max(...abs));return P.map(p=>(j===0?[p[1]*scale,p[2]*scale]:j===1?[p[0]*scale,p[2]*scale]:[p[0]*scale,p[1]*scale]).map(x=>+x.toFixed(5)))}
function obj(M,k,lod){
 const V=[],U=[],N=[],parts=[],maps=[new Map(),new Map(),new Map()],index=(collection,map,vec)=>{const key=vec.join(",");if(!map.has(key)){collection.push(vec);map.set(key,collection.length)}return map.get(key)};
 let prev="";
 for(const f of M.faces){if(prev!==f.g+f.m){prev=f.g+f.m;parts.push("o "+f.g,"usemtl "+f.m)}parts.push("f "+f.p.map((p,i)=>index(V,maps[0],p)+"/"+index(U,maps[1],newUV(f.p,f.n,f.g)[i])+"/"+index(N,maps[2],f.n)).join(" "))}
 return `# Original RAZLOM Izba ${k} LOD${lod}; OBJ, triangulated, meters Z-up\nmtllib M_IzbaKit_1348.mtl\n`+V.map(a=>"v "+a.join(" ")).join("\n")+"\n"+U.map(a=>"vt "+a.join(" ")).join("\n")+"\n"+N.map(a=>"vn "+a.join(" ")).join("\n")+"\n"+parts.join("\n")+"\n";
}
const names=["WallSolid","WallWindow","WallDoor","CornerPost","Floor4m","RoofHalf","DoorLeaf","Table","Bench"];const dir=path.join(__dirname,'..','..','Content','Art','Environment','Medieval1348','IzbaKit');fs.mkdirSync(dir,{recursive:true});for(const n of names)for(const l of [0,1]){const a=mesh(n,l);fs.writeFileSync(path.join(dir,'SM_Izba_'+n+'_1348_LOD'+l+'.obj'),obj(a,n,l));console.log(n,l,a.stats.triangles)}
