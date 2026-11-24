// Original base perimeter. node tools/build_base_perimeter.cjs
const fs=require("node:fs"),path=require("node:path");
function fence(low=false,broken=false){
 const P=[],N=[],U=[],I=[];
 const add=(a,b)=>a.map((v,i)=>v+b[i]),sub=(a,b)=>a.map((v,i)=>v-b[i]),mul=(a,k)=>a.map(v=>v*k),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=a=>mul(a,1/Math.hypot(...a));
 function face(p,t){const n=unit(cross(sub(p[1],p[0]),sub(p[2],p[0]))),j=P.length/3;if(n.some(x=>!Number.isFinite(x)))throw Error("degenerate");for(let k=0;k<p.length;k++){P.push(...p[k]);N.push(...n);const [u,v]=[[0,1],[1,1],[1,0],[0,0]][k];U.push((t%8*128+1+126*u)/1024,(Math.floor(t/8)*128+1+126*v)/512)}for(let k=1;k<p.length-1;k++)I.push(j,j+k,j+k+1)}
 function box(x,y,z,w,h,d,t){const a=x-w/2,b=x+w/2,c=y-h/2,e=y+h/2,f=z-d/2,g=z+d/2;const p=[[a,c,f],[b,c,f],[b,e,f],[a,e,f],[a,c,g],[b,c,g],[b,e,g],[a,e,g]];[[3,2,1,0],[4,5,6,7],[0,4,7,3],[5,1,2,6],[7,6,2,3],[0,1,5,4]].forEach(q=>face(q.map(k=>p[k]),t))}
 function bar(a,b,r,t){const d=unit(sub(b,a)),ax=unit(cross(d,Math.abs(d[1])>.9?[1,0,0]:[0,1,0])),ay=cross(d,ax),k=low?4:5;const ring=Array.from({length:k},(_,i)=>add(mul(ax,Math.cos(i*2*Math.PI/k)),mul(ay,Math.sin(i*2*Math.PI/k))));for(let i=0;i<k;i++){const j=(i+1)%k;face([add(a,mul(ring[j],r)),add(a,mul(ring[i],r)),add(b,mul(ring[i],r)),add(b,mul(ring[j],r))],t)}}
 // Chipped cast-concrete posts with tapered caps and bolted steel brackets.
 for(const s of [-1,1]){
  const x=s*1.47;
  box(x,.105,0,.48,.21,.48,8);
  box(x,1.08,0,.29,1.96,.29,1);
  const a=.195,b=.112,y=2.06,y2=2.21;
  for(const z of [-1,1])face([[x-a,y,z*a],[x+a,y,z*a],[x+b,y2,z*b],[x-b,y2,z*b]],8);
  for(const xx of [-1,1])face([[x+xx*a,y,-a],[x+xx*a,y,a],[x+xx*b,y2,b],[x+xx*b,y2,-b]],8);
  box(x,2.212,0,.224,.013,.224,8);
  if(!low)for(const yy of [.53,1.82]){box(x,yy,-.172,.22,.15,.035,6);for(const dx of [-.066,.066])bar([x+dx,yy,-.18],[x+dx,yy,-.205],.021,6);}
 }
 if(!broken){
   box(0,.11,0,2.62,.22,.25,8);
   for(const y of [.35,1.04,1.90])box(0,y,0,2.70,.055,.065,6);
   for(const x of [-1.28,1.28])box(x,1.13,0,.055,1.63,.07,6);
   const step=low?.26:.13;
   for(let x=-1.23;x<1.25;x+=step)bar([x,.38,0],[x,1.87,0],low?.009:.008,6);
   for(let y=.47;y<1.86;y+=step)bar([-1.26,y,-.01],[1.26,y,-.01],low?.009:.008,6);
   // Broken paint, rust braces, and a warning plate supported by actual frame.
   if(!low){bar([-1.25,.38,.045],[1.25,1.88,.045],.02,6);box(.55,1.47,-.052,.42,.29,.023,14);}
 }else{
   // A collapsed, twisted steel section, leaving an intentional sightline.
   for(const x of [-1.13,-.45,.63])box(x,.09,0,.45,.18,.27,8);
   const points=[[-1.28,.35,.02],[-.48,.42,.11],[.34,.27,.36],[1.22,.14,.64]];
   for(let i=0;i<3;i++){bar(points[i],points[i+1],.03,6);const a=add(points[i],[0,.65,.17]),b=add(points[i+1],[0,.65,.17]);bar(a,b,.027,6);bar(points[i],a,.022,6)}
   for(let i=0;i<(low?7:15);i++){const f=i/(low?6:14),x=-1.27+f*2.45,z=.06+.55*f,y=.32-.18*f;bar([x,y,z],[x+.035*Math.sin(i),y+.64,z+.17],.009,6);}
   if(!low){for(let j=0;j<3;j++)bar([-1.22,.48+j*.17,.10+j*.045],[1.20,.30+j*.17,.66+j*.045],.01,6);box(-.69,.13,.41,.43,.04,.32,14)}
 }
 const chunks=[],views=[],acc=[];let off=0;
 function pack(v,k,w,ix=false){const bytes=new Uint8Array(v.length*(ix?2:4)),dv=new DataView(bytes.buffer);v.forEach((x,i)=>ix?dv.setUint16(i*2,x,true):dv.setFloat32(i*4,x,true));const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad}views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:ix?34963:34962});chunks.push(bytes);off+=bytes.length;const a={bufferView:views.length-1,componentType:ix?5123:5126,count:v.length/w,type:k};if(v===P){a.min=[0,1,2].map(k=>Math.min(...v.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...v.filter((_,i)=>i%3===k)))}acc.push(a);return acc.length-1}
 const attrs={POSITION:pack(P,"VEC3",3),NORMAL:pack(N,"VEC3",3),TEXCOORD_0:pack(U,"VEC2",2)},indices=pack(I,"SCALAR",1,true);
 const bytes=new Uint8Array(off);let p=0;for(const c of chunks){bytes.set(c,p);p+=c.length}const abc="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let b64="";for(let i=0;i<off;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);b64+=abc[(n>>18)&63]+abc[(n>>12)&63]+(i+1<off?abc[(n>>6)&63]:"=")+(i+2<off?abc[n&63]:"=");}
 const name="base_fence"+(broken?"_damaged":"")+(low?"_lod":""),doc={asset:{version:"2.0",generator:"RAZLOM base workshop"},scene:0,scenes:[{nodes:[0]}],nodes:[{name,mesh:0}],meshes:[{primitives:[{attributes:attrs,indices,material:0}]}],materials:[{name:"Concrete and weathered steel",doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:.92,metallicFactor:0}}],images:[{uri:"../zalesye/district.png"}],textures:[{source:0,sampler:0}],samplers:[{magFilter:9728,minFilter:9984}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+b64}],bufferViews:views,accessors:acc};
 return {name,json:JSON.stringify(doc)+"\n",triangles:I.length/3,bounds:[acc[0].min,acc[0].max]};
}
for(const damaged of [false,true])for(const low of [false,true]){const m=fence(low,damaged);fs.writeFileSync(path.join(__dirname,"../assets/base",m.name+".gltf"),m.json);console.log(m.name,m.triangles,m.bounds);}
