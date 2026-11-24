// Forest road embankments. node tools/build_forest_access.cjs
const fs=require("node:fs"),path=require("node:path");
function forest(step){
 const P=[],N=[],U=[],I=[];
 const s=(a,b,x)=>{const t=Math.max(0,Math.min(1,(x-a)/(b-a)));return t*t*(3-2*t)};
 function h(x,z){
   const left=x<=14,edge=left?s(14,10,x):s(31,39,x);
   const along=s(-36,-30,z)*(1-s(-9,-3,z));
   const undulate=1.15+.56*Math.sin(z*.19+x*.28)+.38*Math.cos(z*.39-x*.16);
   const limit=left?1-s(1.4,3.7,Math.hypot(x-12,z+13.5)):0;
   return .018+Math.max(.18,undulate)*edge*along*(1-limit*.93);
 }
 function grid(a,b){let g=[];for(let x=a;x<b;x+=step)g.push(x);g.push(b);return g}
 for(const [a,b] of [[9,14],[31,44]]){
  const xs=grid(a,b),zs=grid(-36,-3),start=P.length/3;
  for(const z of zs)for(const x of xs){const y=h(x,z),dx=(h(x+.03,z)-h(x-.03,z))/.06,dz=(h(x,z+.03)-h(x,z-.03))/.06,n=Math.hypot(dx,1,dz);P.push(x,y,z);N.push(-dx/n,1/n,-dz/n);U.push(x*.4,z*.4)}
  for(let j=0;j<zs.length-1;j++)for(let i=0;i<xs.length-1;i++){const a=start+j*xs.length+i,b=a+1,c=a+xs.length,d=c+1;I.push(a,c,b,b,c,d)}
 }
 const chunks=[],views=[],acc=[];let off=0;
 function pack(v,k,w,ix=false){const bytes=new Uint8Array(v.length*(ix?2:4)),dv=new DataView(bytes.buffer);v.forEach((x,i)=>ix?dv.setUint16(i*2,x,true):dv.setFloat32(i*4,x,true));const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad}views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:ix?34963:34962});chunks.push(bytes);off+=bytes.length;const a={bufferView:views.length-1,componentType:ix?5123:5126,count:v.length/w,type:k};if(v===P){a.min=[0,1,2].map(k=>Math.min(...v.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...v.filter((_,i)=>i%3===k)))}acc.push(a);return acc.length-1}
 const attrs={POSITION:pack(P,"VEC3",3),NORMAL:pack(N,"VEC3",3),TEXCOORD_0:pack(U,"VEC2",2)},indices=pack(I,"SCALAR",1,true);
 const bytes=new Uint8Array(off);let p=0;for(const c of chunks){bytes.set(c,p);p+=c.length}const abc="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let base64="";for(let i=0;i<off;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);base64+=abc[(n>>18)&63]+abc[(n>>12)&63]+(i+1<off?abc[(n>>6)&63]:"=")+(i+2<off?abc[n&63]:"=")}
 const name=step===1?"forest_access_landform":"forest_access_landform_lod";
 const doc={asset:{version:"2.0",generator:"RAZLOM original forest terrain"},scene:0,scenes:[{nodes:[0]}],nodes:[{name,mesh:0}],meshes:[{primitives:[{attributes:attrs,indices,material:0}]}],materials:[{name:"Forest earth",pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:.96}}],images:[{uri:"../village/leaf_litter.jpg"}],textures:[{source:0,sampler:0}],samplers:[{magFilter:9728,minFilter:9984,wrapS:10497,wrapT:10497}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+base64}],bufferViews:views,accessors:acc};
 return {json:JSON.stringify(doc)+"\n",triangles:I.length/3,bounds:[acc[0].min,acc[0].max]};
}
for(const step of [1,3]){const m=forest(step);fs.writeFileSync(path.join(__dirname,"../assets/zalesye",step===1?"forest_access_landform.gltf":"forest_access_landform_lod.gltf"),m.json);console.log(m.triangles,m.bounds);}
