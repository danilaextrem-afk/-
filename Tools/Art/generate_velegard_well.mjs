// Razlom. Original authored procedural medieval stone well.
// Node.js >=20; OBJ/MTL, UV per face, normals, Z up, 1 unit = 1m.
import fs from 'node:fs';
import path from 'node:path';
function generateWell(level=0){
 const high=level===0, PI=Math.PI, lines=[
 "# RAZLOM original Velegard 1348 well. Meter scale, Z-up, all original geometry",
 "mtllib M_Velegard_Well.mtl","o SM_Velegard_Well_1348_LOD"+level];
 let v=0,t=0,groups={},last="";
 const V=(a,b)=>a.map((x,i)=>x+b[i]), S=(a,b)=>a.map((x,i)=>x-b[i]),
 M=(a,s)=>a.map(x=>x*s), X=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],
 N=a=>{let d=Math.hypot(...a)||1;return M(a,1/d);};
 function tri(mat,a,b,c){
   if(mat!==last){lines.push("g "+mat,"usemtl "+mat);last=mat;}
   const n=N(X(S(b,a),S(c,a))), pts=[a,b,c],uv=[[0,0],[1,0],[0,1]];
   for(let i=0;i<3;i++){
      lines.push("v "+pts[i].map(q=>q.toFixed(5)).join(" "));
      lines.push("vt "+uv[i].join(" "));
      lines.push("vn "+n.map(q=>q.toFixed(6)).join(" "));
   }
   lines.push("f "+[1,2,3].map(k=>{let j=v+k;return j+"/"+j+"/"+j;}).join(" "));
   v+=3;t++;groups[mat]=(groups[mat]||0)+1;
 }
 function quad(mat,a,b,c,d){tri(mat,a,b,c);tri(mat,a,c,d);}
 function cube(mat,C,H){
  const [x,y,z]=C,[a,b,c]=H;
  const A=[x-a,y-b,z-c],B=[x+a,y+b,z+c];
  quad(mat,[A[0],A[1],A[2]],[A[0],B[1],A[2]],[B[0],B[1],A[2]],[B[0],A[1],A[2]]);
  quad(mat,[A[0],A[1],B[2]],[B[0],A[1],B[2]],[B[0],B[1],B[2]],[A[0],B[1],B[2]]);
  quad(mat,[A[0],A[1],A[2]],[B[0],A[1],A[2]],[B[0],A[1],B[2]],[A[0],A[1],B[2]]);
  quad(mat,[A[0],B[1],A[2]],[A[0],B[1],B[2]],[B[0],B[1],B[2]],[B[0],B[1],A[2]]);
  quad(mat,[A[0],A[1],A[2]],[A[0],A[1],B[2]],[A[0],B[1],B[2]],[A[0],B[1],A[2]]);
  quad(mat,[B[0],A[1],A[2]],[B[0],B[1],A[2]],[B[0],B[1],B[2]],[B[0],A[1],B[2]]);
 }
 function cyl(mat,A,B,r,n=12,r2=r){
  let axis=N(S(B,A)),test=Math.abs(axis[2])>.9?[0,1,0]:[0,0,1], u=N(X(axis,test)),w=N(X(axis,u)),a=[],b=[];
  for(let k=0;k<n;k++){let ang=2*PI*k/n,h=V(M(u,Math.cos(ang)),M(w,Math.sin(ang)));a.push(V(A,M(h,r)));b.push(V(B,M(h,r2)));}
  for(let k=0;k<n;k++){let q=(k+1)%n;quad(mat,a[k],a[q],b[q],b[k]);tri(mat,A,a[q],a[k]);tri(mat,B,b[k],b[q]);}
 }
 function wedge(mat,rin,rout,aa,bb,lo,hi){
  const p=(r,a,z)=>[r*Math.cos(a),r*Math.sin(a),z], a=p(rin,aa,lo),b=p(rout,aa,lo),c=p(rin,bb,lo),d=p(rout,bb,lo),
        e=p(rin,aa,hi),f=p(rout,aa,hi),g=p(rin,bb,hi),h=p(rout,bb,hi);
  quad(mat,b,d,h,f);quad(mat,c,a,e,g);quad(mat,e,f,h,g);quad(mat,a,c,d,b);quad(mat,a,b,f,e);quad(mat,d,c,g,h);
 }
 function route(mat,pts,r=0.018,n=7){for(let i=1;i<pts.length;i++)cyl(mat,pts[i-1],pts[i],r,n);}
 // Interlocking stone courses, irregular faces and chipped gaps.
 const stones=high?20:12,courses=high?3:2;
 for(let j=0;j<courses;j++)for(let k=0;k<stones;k++){
  let aa=2*PI*(k+(j%2)/2)/stones, bb=2*PI*(k+1+(j%2)/2)/stones-0.018,crack=0.012*Math.sin(k*8.6+j*5.1);
  wedge((k+j)%7===0?"StoneDark":"Stone",0.54,0.83+crack,aa,bb,j*.89/courses+0.008,(j+1)*.89/courses-0.01);
  if(high&&(k+j*3)%9===0)wedge("Moss",0.816,0.852,aa+0.03,aa+0.11,j*.89/courses+0.06,j*.89/courses+0.10);
 }
 const rim=high?16:9;
 for(let i=0;i<rim;i++)wedge("Stone",.52,.93,2*PI*i/rim,2*PI*(i+1)/rim-.015,.89,1.0);
 // Dark water disc, open centre of well.
 const circle=high?40:20;
 for(let i=0;i<circle;i++)tri("Water",[0,0,.3],[.54*Math.cos(2*PI*i/circle),.54*Math.sin(2*PI*i/circle),.3],
 [.54*Math.cos(2*PI*(i+1)/circle),.54*Math.sin(2*PI*(i+1)/circle),.3]);
 // Four wood posts, foot iron and cross braces.
 for(let x of [-.93,.93])for(let y of [-.76,.76]){
  cyl("Wood",[x,y,0.03],[x,y,2.15],.105,high?10:7);
  cyl("Iron",[x,y,0.05],[x,y,.28],.117,high?10:7);
  cyl("Brass",[x,y,.265],[x,y,.29],.127,high?10:7);
  route("WoodDark",[[x,y,1.30],[x-x*.22,y,1.78],[x-x*.25,y,2.12]],.068,8);
 }
 for(let y of [-.76,.76])route("WoodDark",[[-1.06,y,2.15],[0,y,2.92],[1.06,y,2.15]],.087,10);
 route("WoodDark",[[0,-1.1,2.92],[0,1.1,2.92]],.098,12);
 for(let x of [-1.065,1.065])route("WoodDark",[[x,-1.1,2.15],[x,1.1,2.15]],.079,9);
 // Two sloped roofs composed of individual solid timber shingles.
 const rows=high?8:4,cols=high?11:6;
 for(let sign of [-1,1])for(let i=0;i<rows;i++)for(let j=0;j<cols;j++){
  let x0=sign*(i/rows)*1.15,x1=sign*((i+1)/rows)*1.15,
      ya=-1.09+j*2.18/cols,yb=-1.09+(j+1)*2.18/cols-.010,
      z0=2.91-Math.abs(x0)*.69,z1=2.91-Math.abs(x1)*.69,
      wobble=.008*Math.sin((j+2)*3.21+(i+1)*4.9);
  const mat=(i+j)%5===0?"WoodDark":"Shingle";
  quad(mat,[x0,ya,z0+wobble],[x1,ya,z1+wobble],[x1,yb,z1+wobble],[x0,yb,z0+wobble]);
  quad(mat,[x0,ya,z0-.045],[x0,yb,z0-.045],[x1,yb,z1-.045],[x1,ya,z1-.045]);
  quad(mat,[x0,ya,z0+wobble],[x0,ya,z0-.045],[x1,ya,z1-.045],[x1,ya,z1+wobble]);
  quad(mat,[x0,yb,z0+wobble],[x1,yb,z1+wobble],[x1,yb,z1-.045],[x0,yb,z0-.045]);
  quad(mat,[x1,ya,z1+wobble],[x1,ya,z1-.045],[x1,yb,z1-.045],[x1,yb,z1+wobble]);
  if(high && i%2===0 && j%4===0)cyl("Iron",[x1,ya+.04,z1+.007],[x1,ya+.04,z1+.019],.019,6);
 }
 // Seven bells relief on roof front.
 cube("WoodDark",[0,-.835,2.59],[.21,.025,.19]);
 for(let i=0;i<7;i++){
  let x=(i-3)*.05, z=2.56+.051*Math.cos((i-3)/3*PI/2);
  cyl("Brass",[x,-.88,z-.031],[x,-.88,z+.025],.022,high?9:6,.012);
  cyl("Iron",[x,-.886,z-.035],[x,-.886,z-.046],.007,6);
 }
 // Turnable wood winch with iron collars, crank handle and spiral hemp rope.
 cyl("WoodDark",[0,-1.05,1.68],[0,1.14,1.68],.09,high?16:10);
 cyl("Wood",[0,-.43,1.68],[0,.43,1.68],.19,high?20:10);
 for(let y of [-.43,.43])cyl("Iron",[0,y-.025,1.68],[0,y+.025,1.68],.204,high?16:9);
 cyl("Iron",[0,1.11,1.68],[.35,1.11,1.68],.044,12);
 cyl("WoodDark",[.35,1.11,1.68],[.35,1.11,1.99],.054,12);
 cyl("Brass",[.35,1.11,1.965],[.35,1.11,1.997],.065,10);
 const spiral=[],seg=(high?8:4)*(high?12:8),turns=high?8:4;
 for(let i=0;i<=seg;i++){let angle=2*PI*turns*i/seg;spiral.push([.212*Math.cos(angle),-.32+.64*i/seg,1.68+.212*Math.sin(angle)]);}
 route("Rope",spiral,.018,high?7:5);
 route("Rope",[[.22,0,1.68],[.22,0,1.42],[.22,0,1.08],[.22,0,.86]],.02,6);
 // Hollow hoop-bound bucket and arched iron handle, 18 assembled wooden staves.
 const bs=high?18:10,bx=.22,by=0,zbase=.47;
 const bp=(r,a,z)=>[bx+r*Math.cos(a),by+r*Math.sin(a),z];
 for(let i=0;i<bs;i++){
  let a=2*PI*i/bs,b=2*PI*(i+1)/bs-.015;
  quad("Wood",bp(.16,a,zbase),bp(.16,b,zbase),bp(.225,b,zbase+.35),bp(.225,a,zbase+.35));
  quad("WoodDark",bp(.15,b,zbase),bp(.15,a,zbase),bp(.208,a,zbase+.35),bp(.208,b,zbase+.35));
 }
 for(let z of [.54,.74]){
  let ring=[],pts=high?40:20,r=.164+(z-zbase)*.18;
  for(let i=0;i<=pts;i++)ring.push(bp(r,2*PI*i/pts,z));
  route("Iron",ring,.015,7);
 }
 let handle=[],hn=high?28:14;
 for(let i=0;i<=hn;i++){let a=PI*i/hn;handle.push([bx+.23*Math.cos(a),by,zbase+.35+.21*Math.sin(a)]);}
 route("Iron",handle,.017,7);
 for(let i=0;i<bs;i++)tri("WoodDark",[bx,by,zbase+.018],bp(.15,2*PI*i/bs,zbase+.018),bp(.15,2*PI*(i+1)/bs,zbase+.018));
 return {obj:lines.join("\n")+"\n", vertices:v, triangles:t, materials:groups, lod:level};
}
function makeMtl(){
 const d={Stone:[".35 .34 .31",900,0],StoneDark:[".23 .23 .22",920,0],Moss:[".10 .16 .06",940,0],
 Wood:[".30 .17 .09",750,0],WoodDark:[".15 .09 .05",810,0],Shingle:[".20 .12 .075",790,0],
 Iron:[".16 .18 .19",400,.88],Brass:[".48 .31 .13",300,.78],Rope:[".45 .34 .20",850,0],Water:[".05 .15 .20",190,.03]};
 let s="# Draft PBR-inspired metal/roughness factors; no baked textures yet.\n";
 for(let [n,[c,rough,metal]] of Object.entries(d)){
  s+=`newmtl ${n}\nKd ${c}\nKa 0 0 0\nKs 0.14 0.14 0.14\nNs ${Math.round(1000-rough)}\nPr ${rough/1000}\nPm ${metal}\nillum 2\n\n`;
 }
 return s;
}
const out=process.argv[2]||'Content/Art/Environments/MedievalArchitecture/VelegardWell';
fs.mkdirSync(out,{recursive:true});
for(const lod of [0,1])fs.writeFileSync(path.join(out,'SM_Velegard_Well_1348_LOD'+lod+'.obj'),generateWell(lod).obj);
fs.writeFileSync(path.join(out,'M_Velegard_Well.mtl'),makeMtl());
for(const lod of [0,1]){const m=generateWell(lod);console.log('LOD'+lod,m.triangles+' triangles',m.vertices+' vertices');}
