// RAZLOM: Original authored apartment-9 mailbox bank, 12 real doors, no imported models.
import fs from 'node:fs';
import path from 'node:path';
function mailboxSet(lod=0) {
 const high=lod===0, pi=Math.PI, lines=["# RAZLOM: ninth entrance mailboxes, original authored geometry",
 "mtllib M_NinthEntry_Mailboxes.mtl","o SM_NinthEntry_Mailboxes_2026_LOD"+lod];
 let v=0,tris=0,last="";
 const matCount={};
 const s=(a,b)=>a.map((x,i)=>x-b[i]),mul=(a,x)=>a.map(v=>v*x),
 cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],
 norm=a=>{let n=Math.hypot(...a)||1;return mul(a,1/n);};
 function tri(m,a,b,c){
  if(last!==m){lines.push("g "+m,"usemtl "+m);last=m;}
  const n=norm(cross(s(b,a),s(c,a)));
  for(const [i,p] of [a,b,c].entries()){
    lines.push("v "+p.map(x=>x.toFixed(5)).join(" "));
    lines.push("vt "+[[0,0],[1,0],[0,1]][i].join(" "));
    lines.push("vn "+n.map(x=>x.toFixed(5)).join(" "));
  }
  lines.push("f "+[v+1,v+2,v+3].map(k=>k+"/"+k+"/"+k).join(" "));
  v+=3;tris++;matCount[m]=(matCount[m]||0)+1;
 }
 function q(m,a,b,c,d){tri(m,a,b,c);tri(m,a,c,d);}
 function box(m,xc,yc,zc,w,d,h){
  let x=xc-w/2,X=xc+w/2,y=yc-d/2,Y=yc+d/2,z=zc-h/2,Z=zc+h/2;
  q(m,[x,y,z],[X,y,z],[X,Y,z],[x,Y,z]);
  q(m,[x,y,Z],[x,Y,Z],[X,Y,Z],[X,y,Z]);
  q(m,[x,y,z],[x,y,Z],[X,y,Z],[X,y,z]);
  q(m,[x,Y,z],[X,Y,z],[X,Y,Z],[x,Y,Z]);
  q(m,[x,y,z],[x,Y,z],[x,Y,Z],[x,y,Z]);
  q(m,[X,y,z],[X,y,Z],[X,Y,Z],[X,Y,z]);
 }
 function cyl(m,A,B,r,n=12){
   const axis=norm(s(B,A)),ref=Math.abs(axis[1])>.9?[1,0,0]:[0,1,0],
   u=norm(cross(axis,ref)),w=norm(cross(axis,u)),lo=[],up=[];
   for(let k=0;k<n;k++){let a=2*pi*k/n,dir=u.map((x,i)=>x*Math.cos(a)+w[i]*Math.sin(a));lo.push(A.map((x,i)=>x+dir[i]*r));up.push(B.map((x,i)=>x+dir[i]*r));}
   for(let k=0;k<n;k++){let j=(k+1)%n;q(m,lo[k],lo[j],up[j],up[k]);tri(m,A,lo[j],lo[k]);tri(m,B,up[k],up[j]);}
 }
 // Steel backing structure wall mounts, four assembly rails.
 box("SteelDark",0,.115,1.23,1.16,.23,1.29);
 box("Enamel",0,-.013,1.23,1.14,.025,1.27);
 for(let x of [-.592,.592])box("SteelDark",x,-.08,1.23,.032,.20,1.34);
 for(let z of [.565,1.875])box("SteelDark",0,-.07,z,1.22,.20,.038);
 for(let x of [-.42,.42])for(let z of [.54,1.92])cyl("Iron",[x,.24,z],[x,.285,z],.041,high?10:6);
 // Twelve doors with actual mail insertion gaps, border lips, numbered plates, key hardware.
 const nums=["1","2","3","4","5","6","7","8","9","10","11","12"], cw=.344,ch=.293;
 const digits={
  "0":"abcdef","1":"bc","2":"abdeg","3":"abcdg","4":"bcfg","5":"acdfg","6":"acdefg",
  "7":"abc","8":"abcdefg","9":"abcdfg"
 };
 const segs={a:[-.022,.044,.022,.044],b:[.022,.044,.022,0],c:[.022,0,.022,-.044],
 d:[-.022,-.044,.022,-.044],e:[-.022,0,-.022,-.044],f:[-.022,.044,-.022,0],g:[-.022,0,.022,0]};
 function stroke(x,y,z,aa,bb,cc,dd){
  const dx=cc-aa, dz=dd-bb, cxx=x+(aa+cc)/2,czz=z+(bb+dd)/2;
  if(Math.abs(dx)>Math.abs(dz))box("Number",cxx,y,czz,Math.abs(dx)+.005,.003,.008);
  else box("Number",cxx,y,czz,.008,.003,Math.abs(dz)+.005);
 }
 for(let r=0;r<4;r++)for(let c=0;c<3;c++){
   const num=r*3+c+1, x=-.357+c*.357,z=.777+(3-r)*.298;
   // Folded sheet metal door bottom and side reinforcement.
   box(num%5===0?"EnamelFaded":"Enamel",x,-.169,z,cw,ch*.035,ch*.94);
   box("SteelDark",x,-.187,z,ch*.013,.018,ch*.95); // hinge-adjacent vertical strip
   for(let zz of [z-ch*.44,z+ch*.44])box("SteelDark",x,-.19,zz,cw,.023,.008);
   for(let xx of [x-cw*.48,x+cw*.48])box("SteelDark",xx,-.19,z,.009,.027,ch*.88);
   // True front opening is modeled by strips around horizontal slot.
   const sy=-.191, slz=z+ch*.232;
   for(const offset of [-.037,.037])box("Iron",x,sy,slz+offset,cw*.72,.014,.013);
   for(const offset of [-cw*.355,cw*.355])box("Iron",x+offset,sy,slz,.010,.014,.072);
   box("SlotBlack",x,-.196,slz,cw*.68,.002,.045);
   // Paper name strip under cellophane.
   box("Paper",x-.067,-.196,z-.058,.122,.004,.039);
   box("Glass",x-.067,-.201,z-.058,.133,.003,.05);
   box("SteelDark",x-.067,-.204,z-.083,.142,.004,.006);
   // Physical lock barrel with round face, dark slit and metal surround.
   cyl("Iron",[x+.113,-.195,z-.057],[x+.113,-.223,z-.057],.036,high?16:9);
   cyl("Brass",[x+.113,-.224,z-.057],[x+.113,-.229,z-.057],.028,high?16:9);
   box("SlotBlack",x+.113,-.232,z-.058,.004,.004,.025);
   box("SlotBlack",x+.113,-.232,z-.067,.014,.004,.004);
   // hinges, security screw heads
   for(const zz of [z-.115,z+.115]){
      cyl("Iron",[x-.165,-.182,zz],[x-.165,-.22,zz],.015,high?10:6);
      if(high)box("SteelDark",x-.165,-.222,zz,.006,.002,.014);
   }
   // High fidelity numbers are embossed as seven segment metal lettering.
   if(high){
     let txt=nums[num-1],x0=x-.096-(txt.length-1)*.026;
     for(let j=0;j<txt.length;j++){
       for(let key of digits[txt[j]]){
          const [a,b,c,d]=segs[key];stroke(x0+j*.052,-.212,z-.059,a*.49,b*.64,c*.49,d*.64);
       }
     }
     // Small damage details: peeled paint crescents and mounting screw chips.
     if(num%4===0){
       box("Rust",x+.015,-.205,z+.116,.044,.002,.006);
       box("Rust",x+.033,-.206,z+.110,.020,.002,.004);
     }
   }
 }
 // Aged building information holder at side of rack.
 box("SteelDark",.73,-.080,1.50,.19,.018,.48);
 box("Paper",.73,-.092,1.51,.164,.005,.44);
 for(let i=0;i<(high?11:6);i++)box("PrintedInk",.726,-.097,1.67-i*.029,.125-.012*(i%3),.002,.003);
 return {obj:lines.join("\n")+"\n",vertices:v,triangles:tris,materials:Object.keys(matCount)};
}
function mailboxMtl(){
 const a={SteelDark:[".10 .11 .11",.6,.85],Enamel:[".39 .46 .43",.47,.15],EnamelFaded:[".26 .33 .30",.72,.12],Iron:[".20 .20 .21",.43,.78],
 Brass:[".48 .36 .14",.32,.81],Number:[".78 .77 .62",.48,.44],Glass:[".55 .61 .56",.13,.02],SlotBlack:[".015 .019 .024",.86,0],
 Paper:[".75 .71 .59",.94,0],Rust:[".43 .18 .075",.89,.06],PrintedInk:[".14 .18 .17",.95,0]};
 return "# Draft MTL; final texture maps still required\n"+Object.entries(a).map(([name,[kd,r,m]])=>`newmtl ${name}\nKd ${kd}\nKs 0.10 0.10 0.10\nNs ${Math.round(1000*(1-r))}\nPr ${r}\nPm ${m}\nillum 2\n`).join("\n");
}
const dir=process.argv[2]||'Content/Art/Interiors/Modern/Apartment9Mailboxes';
fs.mkdirSync(dir,{recursive:true});
for(let n of [0,1])fs.writeFileSync(path.join(dir,'SM_NinthEntry_Mailboxes_2026_LOD'+n+'.obj'),mailboxSet(n).obj);
fs.writeFileSync(path.join(dir,'M_NinthEntry_Mailboxes.mtl'),mailboxMtl());
console.log('Generated author-original Ninth Entry mailboxes meshes.');
