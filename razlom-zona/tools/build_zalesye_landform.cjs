// Original terrain; regenerate with node tools/build_zalesye_landform.cjs
const fs=require("node:fs"),path=require("node:path");
function terrain(step){
  const pos=[],norm=[],uv=[],idx=[];
  const smooth=(a,b,x)=>{let t=Math.min(1,Math.max(0,(x-a)/(b-a)));return t*t*(3-2*t)};
  const height=(x,z)=>{
    let a=Math.abs(x);
    let envelope=smooth(13,24,a)*smooth(-48,-43,z)* (1-smooth(0,5,z));
    // Broad ridgelines, a low hollow behind the houses, not random voxel spikes.
    let contours=0.9+0.52*Math.sin(z*.15+x*.105)+0.33*Math.sin(z*.31-x*.17)+0.18*Math.cos(x*.42+z*.27);
    // Clear the existing solid building at world (-3,-20) and nearby tree trunks.
    const buildingClear=1-(1-smooth(4.5,9,Math.hypot(x+22,z+36)));
    return .025+envelope*Math.max(.18,contours)*buildingClear;
  };
  const grid=(lo,hi)=>{let r=[];for(let x=lo;x<hi;x+=step)r.push(x);r.push(hi);return r;};
  for(const sign of [-1,1]){
    const xs=grid(5,35).map(x=>x*sign).sort((a,b)=>a-b),zs=grid(-48,6);
    const start=pos.length/3;
    for(const z of zs)for(const x of xs){
      const h=height(x,z),dx=(height(x+.025,z)-height(x-.025,z))/.05,dz=(height(x,z+.025)-height(x,z-.025))/.05,n=Math.hypot(dx,1,dz);
      pos.push(x,h,z);norm.push(-dx/n,1/n,-dz/n);uv.push(x*.36,z*.36);
    }
    for(let j=0;j<zs.length-1;j++)for(let i=0;i<xs.length-1;i++){
      const a=start+j*xs.length+i,b=a+1,c=a+xs.length,d=c+1;idx.push(a,c,b,b,c,d);
    }
  }
  const chunks=[],views=[],accessors=[];let off=0;
  function add(values,kind,width,indices=false){
    const bytes=new Uint8Array(values.length*(indices?2:4)),dv=new DataView(bytes.buffer);
    values.forEach((v,i)=>indices?dv.setUint16(i*2,v,true):dv.setFloat32(i*4,v,true));
    const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad;}
    views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:indices?34963:34962});chunks.push(bytes);off+=bytes.length;
    const a={bufferView:views.length-1,componentType:indices?5123:5126,count:values.length/width,type:kind};
    if(values===pos){a.min=[0,1,2].map(k=>Math.min(...values.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...values.filter((_,i)=>i%3===k)));}
    accessors.push(a);return accessors.length-1;
  }
  const attrs={POSITION:add(pos,"VEC3",3),NORMAL:add(norm,"VEC3",3),TEXCOORD_0:add(uv,"VEC2",2)},indices=add(idx,"SCALAR",1,true);
  const bytes=new Uint8Array(off);let offset=0;for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.length}
  const alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let b64="";
  for(let i=0;i<bytes.length;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);b64+=alphabet[(n>>18)&63]+alphabet[(n>>12)&63]+(i+1<bytes.length?alphabet[(n>>6)&63]:"=")+(i+2<bytes.length?alphabet[n&63]:"=");}
  const name=step===2?"zalesye_landform":"zalesye_landform_lod";
  const doc={asset:{version:"2.0",generator:"RAZLOM original terrain source"},scene:0,scenes:[{nodes:[0]}],nodes:[{mesh:0,name}],meshes:[{primitives:[{attributes:attrs,indices,material:0}]}],materials:[{name:"Damp autumn earth",pbrMetallicRoughness:{baseColorTexture:{index:0},metallicFactor:0,roughnessFactor:.94}}],images:[{uri:"../village/leaf_litter.jpg"}],samplers:[{magFilter:9728,minFilter:9984,wrapS:10497,wrapT:10497}],textures:[{sampler:0,source:0}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+b64}],bufferViews:views,accessors};
  return {json:JSON.stringify(doc)+"\n",triangles:idx.length/3,bounds:[accessors[0].min,accessors[0].max]};
}
for(const step of [2,4]){const m=terrain(step);const n=step===2?"zalesye_landform":"zalesye_landform_lod";fs.writeFileSync(path.join(__dirname,"../assets/zalesye",n+".gltf"),m.json);console.log(n,m.triangles,m.bounds)}
