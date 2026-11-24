// Original wall equipment. node tools/build_g4_service.cjs
const fs=require("node:fs"),path=require("node:path");
function service(kind,low=false){
 const P=[],N=[],U=[],I=[];const sub=(a,b)=>a.map((v,i)=>v-b[i]);
 function face(p,t){const a=sub(p[1],p[0]),b=sub(p[2],p[0]),n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],l=Math.hypot(...n);if(l<1e-8)throw Error("degenerate");const start=P.length/3;for(let k=0;k<p.length;k++){P.push(...p[k]);N.push(...n.map(x=>x/l));const [u,v]=[[0,1],[1,1],[1,0],[0,0]][k];U.push((t%8*128+1+126*u)/1024,(Math.floor(t/8)*128+1+126*v)/512);}for(let k=1;k<p.length-1;k++)I.push(start,start+k,start+k+1);}
 function box(x,y,z,w,h,d,t){const a=x-w/2,b=x+w/2,c=y-h/2,e=y+h/2,f=z-d/2,g=z+d/2;const p=[[a,c,f],[b,c,f],[b,e,f],[a,e,f],[a,c,g],[b,c,g],[b,e,g],[a,e,g]];[[3,2,1,0],[4,5,6,7],[0,4,7,3],[5,1,2,6],[7,6,2,3],[0,1,5,4]].forEach(q=>face(q.map(k=>p[k]),t));}
 const add=(a,b)=>a.map((v,i)=>v+b[i]),mul=(a,k)=>a.map(v=>v*k),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=a=>mul(a,1/Math.hypot(...a));
 function pipe(a,b,r,t,sides=low?6:10){const d=unit(sub(b,a)),ax=unit(cross(d,Math.abs(d[1])>.9?[1,0,0]:[0,1,0])),ay=cross(d,ax);const ring=Array.from({length:sides},(_,i)=>add(mul(ax,Math.cos(i*2*Math.PI/sides)*r),mul(ay,Math.sin(i*2*Math.PI/sides)*r)));for(let i=0;i<sides;i++){let j=(i+1)%sides;face([add(a,ring[j]),add(a,ring[i]),add(b,ring[i]),add(b,ring[j])],t);face([b,add(b,ring[j]),add(b,ring[i])],t)}}
 if(kind==="switchgear"){
  box(0,0,.035,1.22,1.22,.07,6);
  for(const x of [-.30,.30]){
   box(x,0,.12,.55,1.10,.17,31);box(x,0,.223,.49,1.02,.055,4);
   box(x,.29,.258,.35,.18,.018,7);
   box(x-.13,.29,.271,.055,.055,.013,14);box(x+.05,.29,.271,.055,.055,.013,11);
   box(x+.15,-.02,.267,.05,.21,.035,6);
   if(!low){for(let j=0;j<6;j++)box(x,-.24-j*.041,.258,.33,.018,.025,6);for(const y of [-.44,.44])box(x-.24,y,.25,.06,.12,.06,6);}
   pipe([x,-.55,.105],[x,-.77,.105],.038,6);
   pipe([x,.55,.105],[x,.82,.105],.036,6);
  }
 }else if(kind==="valve"){
  box(0,0,.03,1.35,.91,.06,8);
  for(const y of [-.26,.26]){
   pipe([-.68,y,.13],[.68,y,.13],.072,6);
   for(const x of [-.47,.47])pipe([x-.045,y,.13],[x+.045,y,.13],.106,31);
  }
  pipe([0,-.26,.13],[0,.26,.13],.095,31);
  pipe([0,0,.13],[0,0,.27],.048,6);
  const count=low?8:14;
  for(let i=0;i<count;i++){const a=i*2*Math.PI/count,b=(i+1)*2*Math.PI/count;pipe([.19*Math.cos(a),.19*Math.sin(a),.28],[.19*Math.cos(b),.19*Math.sin(b),.28],.021,14,low?4:6)}
  for(let i=0;i<4;i++){let a=i*Math.PI/2;pipe([0,0,.28],[.19*Math.cos(a),.19*Math.sin(a),.28],.017,14,5)}
  if(!low){pipe([.45,.26,.13],[.45,.48,.13],.034,6);pipe([.45,.48,.13],[.45,.48,.20],.115,31);pipe([.45,.48,.20],[.45,.48,.218],.09,13);box(.45,.48,.231,.012,.13,.012,7);}
 }else{
  box(0,0,.05,1.24,.92,.10,8);box(0,0,.12,1.11,.80,.11,6);
  box(0,0,.19,1.04,.73,.07,7);
  for(let j=0;j<(low?5:11);j++){const y=-.32+j*(low?.15:.062);box(0,y,.245,1.03,.048,.105,31)}
  for(const x of [-.59,.59])box(x,0,.18,.08,.88,.18,6);
  if(!low){for(const x of [-.56,.56])for(const y of [-.38,.38])box(x,y,.285,.045,.045,.03,11);pipe([.52,.46,.10],[.52,.65,.10],.027,6)}
 }
 const chunks=[],views=[],acc=[];let off=0;
 function pack(v,k,w,ix=false){const bytes=new Uint8Array(v.length*(ix?2:4)),dv=new DataView(bytes.buffer);v.forEach((x,i)=>ix?dv.setUint16(i*2,x,true):dv.setFloat32(i*4,x,true));const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad;}views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:ix?34963:34962});chunks.push(bytes);off+=bytes.length;const a={bufferView:views.length-1,componentType:ix?5123:5126,count:v.length/w,type:k};if(v===P){a.min=[0,1,2].map(k=>Math.min(...v.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...v.filter((_,i)=>i%3===k)));}acc.push(a);return acc.length-1;}
 const attrs={POSITION:pack(P,"VEC3",3),NORMAL:pack(N,"VEC3",3),TEXCOORD_0:pack(U,"VEC2",2)},indices=pack(I,"SCALAR",1,true);
 const bytes=new Uint8Array(off);let pos=0;for(const c of chunks){bytes.set(c,pos);pos+=c.length}const abc="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let b64="";for(let i=0;i<off;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);b64+=abc[(n>>18)&63]+abc[(n>>12)&63]+(i+1<off?abc[(n>>6)&63]:"=")+(i+2<off?abc[n&63]:"=")}
 const name="g4_"+kind+(low?"_lod":"");const doc={asset:{version:"2.0",generator:"RAZLOM service gallery workshop"},scene:0,scenes:[{nodes:[0]}],nodes:[{name,mesh:0}],meshes:[{primitives:[{attributes:attrs,indices,material:0}]}],materials:[{name:"Weathered service equipment",doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:.84,metallicFactor:.12}}],images:[{uri:"../zalesye/district.png"}],textures:[{source:0,sampler:0}],samplers:[{magFilter:9728,minFilter:9984}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+b64}],bufferViews:views,accessors:acc};
 return {name,json:JSON.stringify(doc)+"\n",triangles:I.length/3,bounds:[acc[0].min,acc[0].max]};
}
for(const kind of ["switchgear","valve","vent"])for(const low of [false,true]){const m=service(kind,low);fs.writeFileSync(path.join(__dirname,"../assets/bunker",m.name+".gltf"),m.json);console.log(m.name,m.triangles,m.bounds);}
