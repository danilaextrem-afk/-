// Original bramble foliage, regenerate: node tools/build_zalesye_bramble.cjs
const fs=require("node:fs"),path=require("node:path");
function bramble(low=false){
 const groups=[{p:[],n:[],u:[],i:[]},{p:[],n:[],u:[],i:[]}];
 const add=(a,b)=>a.map((v,i)=>v+b[i]),sub=(a,b)=>a.map((v,i)=>v-b[i]),mul=(a,k)=>a.map(v=>v*k),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=a=>mul(a,1/Math.hypot(...a));
 function face(points,tile,g){
   const o=groups[g],n=unit(cross(sub(points[1],points[0]),sub(points[2],points[0]))),j=o.p.length/3;
   for(let k=0;k<points.length;k++){o.p.push(...points[k]);o.n.push(...n);const [u,v]=[[0,1],[1,1],[1,0],[0,0]][k];o.u.push((tile%8*128+1+126*u)/1024,(Math.floor(tile/8)*128+1+126*v)/512);}
   for(let k=1;k<points.length-1;k++)o.i.push(j,j+k,j+k+1);
 }
 function limb(a,b,r,s){
   const d=unit(sub(b,a)),axis=unit(cross(d,Math.abs(d[1])>.9?[1,0,0]:[0,1,0])),other=cross(d,axis),sides=low?4:6;
   const ring=Array.from({length:sides},(_,i)=>add(mul(axis,Math.cos(i*2*Math.PI/sides)),mul(other,Math.sin(i*2*Math.PI/sides))));
   for(let i=0;i<sides;i++){const k=(i+1)%sides;face([add(a,mul(ring[k],r)),add(a,mul(ring[i],r)),add(b,mul(ring[i],s)),add(b,mul(ring[k],s))],9,0);}
 }
 function foliage(center,w,h,angle,tilt){
   const side=[Math.cos(angle)*w,0,Math.sin(angle)*w],up=[-Math.sin(angle)*tilt,h,Math.cos(angle)*tilt];
   face([sub(sub(center,side),up),sub(add(center,side),up),add(add(center,side),up),add(sub(center,side),up)],25,1);
 }
 const count=low?5:9;
 for(let k=0;k<count;k++){
   const a=k*2.39996,reach=.43+.25*Math.sin(k*1.72+1.2),h=.46+.45*(.5+.5*Math.sin(k*2.19+.8));
   const root=[.06*Math.cos(a),.015,.06*Math.sin(a)],elbow=[Math.cos(a)*reach*.52,h*.52,Math.sin(a)*reach*.52],tip=[Math.cos(a)*reach,h,Math.sin(a)*reach];
   limb(root,elbow,.025,.013);limb(elbow,tip,.013,.004);
   const sprays=low?1:3;
   for(let j=0;j<sprays;j++){
     const aa=a+j*2.15,branchTip=add(tip,[Math.cos(aa)*.15,(j-1)*.06,Math.sin(aa)*.15]);
     if(!low)limb(elbow,branchTip,.009,.003);
     const center=add(branchTip,[0,.08,0]);
     foliage(center,low?.39:.26,low?.31:.23,aa,.12);
     if(!low)foliage(center,.25,.24,aa+1.57,-.12);
   }
 }
 const chunks=[],views=[],acc=[];let off=0;
 function push(values,kind,width,ix=false){
   const bytes=new Uint8Array(values.length*(ix?2:4)),dv=new DataView(bytes.buffer);values.forEach((v,i)=>ix?dv.setUint16(i*2,v,true):dv.setFloat32(i*4,v,true));const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad;}views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:ix?34963:34962});chunks.push(bytes);off+=bytes.length;const o={bufferView:views.length-1,componentType:ix?5123:5126,count:values.length/width,type:kind};if(kind==="VEC3"){o.min=[0,1,2].map(k=>Math.min(...values.filter((_,i)=>i%3===k)));o.max=[0,1,2].map(k=>Math.max(...values.filter((_,i)=>i%3===k)));}acc.push(o);return acc.length-1;
 }
 const primitives=groups.map((o,i)=>({attributes:{POSITION:push(o.p,"VEC3",3),NORMAL:push(o.n,"VEC3",3),TEXCOORD_0:push(o.u,"VEC2",2)},indices:push(o.i,"SCALAR",1,true),material:i}));
 const bytes=new Uint8Array(off);let offset=0;for(const c of chunks){bytes.set(c,offset);offset+=c.length}const abc="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let base64="";for(let i=0;i<off;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);base64+=abc[(n>>18)&63]+abc[(n>>12)&63]+(i+1<off?abc[(n>>6)&63]:"=")+(i+2<off?abc[n&63]:"=");}
 const name=low?"bramble_lod":"bramble";const doc={asset:{version:"2.0",generator:"Zalesye woodland workshop"},scene:0,scenes:[{nodes:[0]}],nodes:[{name,mesh:0}],meshes:[{primitives}],materials:[{name:"twigs",doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:1}},{name:"leaves",doubleSided:true,alphaMode:"MASK",alphaCutoff:.42,pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:1}}],textures:[{source:0,sampler:0}],images:[{uri:"district.png"}],samplers:[{magFilter:9728,minFilter:9984}],accessors:acc,bufferViews:views,buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+base64}]};const all=groups.flatMap(g=>g.p);return {json:JSON.stringify(doc)+"\n",triangles:groups.reduce((n,g)=>n+g.i.length/3,0),bounds:[0,1,2].map(k=>[Math.min(...all.filter((_,i)=>i%3===k)),Math.max(...all.filter((_,i)=>i%3===k))])};
}
for(const low of [false,true]){const m=bramble(low);fs.writeFileSync(path.join(__dirname,"../assets/zalesye",low?"bramble_lod.gltf":"bramble.gltf"),m.json);console.log(m.triangles,m.bounds);}
