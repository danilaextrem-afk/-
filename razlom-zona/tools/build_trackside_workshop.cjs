// Original repair workshop; node tools/build_trackside_workshop.cjs
const fs=require("node:fs"),path=require("node:path");
function workshop(low=false){
 const P=[],N=[],U=[],I=[];
 const sub=(a,b)=>a.map((v,i)=>v-b[i]);
 function face(p,t){const a=sub(p[1],p[0]),b=sub(p[2],p[0]),n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],l=Math.hypot(...n);if(l<1e-8)throw Error("degenerate");const start=P.length/3;for(let k=0;k<p.length;k++){P.push(...p[k]);N.push(...n.map(x=>x/l));const [u,v]=[[0,1],[1,1],[1,0],[0,0]][k];U.push((t%8*128+1+126*u)/1024,(Math.floor(t/8)*128+1+126*v)/512);}for(let k=1;k<p.length-1;k++)I.push(start,start+k,start+k+1);}
 function box(x,y,z,w,h,d,t){const a=x-w/2,b=x+w/2,c=y-h/2,e=y+h/2,f=z-d/2,g=z+d/2;const p=[[a,c,f],[b,c,f],[b,e,f],[a,e,f],[a,c,g],[b,c,g],[b,e,g],[a,e,g]];[[3,2,1,0],[4,5,6,7],[0,4,7,3],[5,1,2,6],[7,6,2,3],[0,1,5,4]].forEach(q=>face(q.map(k=>p[k]),t));}
 const w=5.8,d=4.45,z=d/2,e=2.95;
 // Low masonry repair workshop, high asymmetric monitor roof, not a wooden cottage.
 box(0,.14,0,w+.12,.28,d+.12,8);
 box(0,1.62,-z,w,2.72,.18,0);
 for(const s of [-1,1])box(s*(w/2-.10),1.62,0,.20,2.72,d,0);
 // Central wide boarded workshop door and two recessed high industrial windows.
 for(const [x,ww] of [[-2.67,.46],[-1.16,.49],[1.10,.54],[2.65,.50]])box(x,1.62,z,ww,2.72,.22,0);
 for(const x of [-1.93,1.89]){
   box(x,.65,z,1.18,1.0,.20,0);box(x,2.65,z,1.18,.65,.20,0);
   box(x,1.73,z-.115,1.13,1.14,.045,12);
   box(x,1.12,z+.09,1.37,.13,.34,8);box(x,2.37,z+.07,1.35,.15,.32,8);
   for(const s of [-1,1])box(x+s*.58,1.75,z+.045,.10,1.20,.13,4);
   if(!low){for(const dx of [-.28,0,.28])box(x+dx,1.74,z+.063,.036,1.11,.067,6);box(x,1.75,z+.063,1.13,.042,.07,6);}
 }
 box(-.02,2.68,z,1.78,.60,.24,0);
 box(-.02,1.18,z-.03,1.75,2.36,.18,7);
 if(!low){
  for(let k=0;k<9;k++)box(-.83+k*.18,1.18,z+.095,.17,2.29,.07,2);
  for(const y of [.30,1.15,2.13])box(0,y,z+.145,1.79,.13,.09,6);
  box(.58,1.07,z+.222,.15,.28,.095,6);
  for(const x of [-.80,.80])box(x,1.2,z+.14,.08,2.32,.09,6);
 }
 box(0,2.96,0,w+.18,.18,d+.18,8);
 // Sawtooth-like asymmetrical raised roof profile and clear gables.
 const profile=[[-3.14,3.07],[-.85,4.20],[.22,4.20],[.22,3.64],[3.14,3.07]];
 for(let j=0;j<profile.length-1;j++){
  const a=profile[j],b=profile[j+1];face([[a[0],a[1],-2.48],[a[0],a[1],2.48],[b[0],b[1],2.48],[b[0],b[1],-2.48]],j===2?12:5);
  for(const s of [-1,1])if(Math.abs(a[0]-b[0])>.001)face([[a[0],3.02,s*2.27],[b[0],3.02,s*2.27],[b[0],b[1],s*2.27],[a[0],a[1],s*2.27]],0);
 }
 // Monitor frame and ridge caps, ventilation stack, visibly different skyline.
 for(const y of [3.68,4.16])box(.22,y,0,.11,.08,4.98,6);
 if(!low)for(let i=0;i<9;i++)box(.23,3.92,-2.15+i*.53,.13,.52,.06,6);
 box(-1.96,3.93,-1.03,.44,1.75,.48,0);
 box(-1.96,4.83,-1.03,.64,.15,.65,8);
 box(-1.96,4.94,-1.03,.37,.14,.38,7);
 if(!low){
   for(const x of [-2.76,2.76])for(let i=0;i<7;i++)box(x,.40+i*.35,z+.13,.24,.16,.15,8);
   for(const s of [-1,1]){box(s*2.92,1.88,-.80,.08,.83,1.22,12);for(const dz of [-.58,0,.58])box(s*2.98,1.88,-.8+dz,.07,.92,.07,6);}
   for(let i=0;i<5;i++){box(-1.16+i*.25,.09,z+.2,.22,.18,.28,8);}
 }
 const chunks=[],views=[],accessors=[];let off=0;
 function add(values,kind,width,ix=false){const bytes=new Uint8Array(values.length*(ix?2:4)),dv=new DataView(bytes.buffer);values.forEach((v,i)=>ix?dv.setUint16(i*2,v,true):dv.setFloat32(i*4,v,true));const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad}views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:ix?34963:34962});chunks.push(bytes);off+=bytes.length;const a={bufferView:views.length-1,componentType:ix?5123:5126,count:values.length/width,type:kind};if(values===P){a.min=[0,1,2].map(k=>Math.min(...values.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...values.filter((_,i)=>i%3===k)));}accessors.push(a);return accessors.length-1;}
 const attrs={POSITION:add(P,"VEC3",3),NORMAL:add(N,"VEC3",3),TEXCOORD_0:add(U,"VEC2",2)},indices=add(I,"SCALAR",1,true);
 const bytes=new Uint8Array(off);let offset=0;for(const c of chunks){bytes.set(c,offset);offset+=c.length}const abc="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let b64="";for(let i=0;i<off;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);b64+=abc[(n>>18)&63]+abc[(n>>12)&63]+(i+1<off?abc[(n>>6)&63]:"=")+(i+2<off?abc[n&63]:"=");}
 const name=low?"trackside_workshop_lod":"trackside_workshop",doc={asset:{version:"2.0",generator:"Zalesye architecture workshop"},scene:0,scenes:[{nodes:[0]}],nodes:[{name,mesh:0}],meshes:[{primitives:[{attributes:attrs,indices,material:0}]}],materials:[{name:"Weathered district masonry",doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},metallicFactor:0,roughnessFactor:.92}}],images:[{uri:"district.png"}],textures:[{source:0,sampler:0}],samplers:[{magFilter:9728,minFilter:9984}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+b64}],bufferViews:views,accessors};
 return {json:JSON.stringify(doc)+"\n",triangles:I.length/3,bounds:[accessors[0].min,accessors[0].max]};
}
for(const low of [false,true]){const m=workshop(low);fs.writeFileSync(path.join(__dirname,"../assets/zalesye",low?"trackside_workshop_lod.gltf":"trackside_workshop.gltf"),m.json);console.log(m.triangles,m.bounds);}
