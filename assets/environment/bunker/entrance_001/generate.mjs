// Original procedural asset: RZ_ZONA_ENV_BUNKER_ENTRANCE_001 v0.1.0
// Coordinate convention: Y up, X width, Z depth, meters.
// Reproduce: node generate.mjs > bunker_entrance.gltf
const parts=[["left",[-1.35,1.5,0],[0.95,3,0.55],0],["right",[1.35,1.5,0],[0.95,3,0.55],0],["lintel",[0,2.65,0],[1.75,0.7,0.55],0],["threshold",[0,0.1,0],[1.75,0.2,0.7],1],["door",[-0.38,1.2,-0.12],[0.72,2.15,0.09],1],["hingeA",[-0.72,1.85,-0.2],[0.18,0.18,0.17],2],["hingeB",[-0.72,0.55,-0.2],[0.18,0.18,0.17],2],["handle",[-0.1,1.05,-0.25],[0.09,0.25,0.12],3],["postL",[-1.82,1.5,0.1],[0.12,3,0.7],1],["postR",[1.82,1.5,0.1],[0.12,3,0.7],1],["lamp",[0,2.87,-0.38],[0.27,0.12,0.12],3]];
const colors=[[.38,.41,.40,1],[.22,.27,.28,1],[.51,.26,.20,1],[.70,.12,.16,1]];
let pos=[],uv=[],idx=[],groups=[[],[],[],[]];
for(const [name,c,s,mat] of parts){
 const x=c[0],y=c[1],z=c[2],a=s[0]/2,b=s[1]/2,d=s[2]/2;
 const corners=[[-a,-b,-d],[a,-b,-d],[a,b,-d],[-a,b,-d],[-a,-b,d],[a,-b,d],[a,b,d],[-a,b,d]];
 const faces=[[0,1,2,3],[5,4,7,6],[4,0,3,7],[1,5,6,2],[3,2,6,7],[4,5,1,0]];
 for(const f of faces){let start=pos.length/3;for(let i=0;i<4;i++){let p=corners[f[i]];pos.push(x+p[0],y+p[1],z+p[2]);uv.push(i===1||i===2?1:0,i>=2?1:0)}groups[mat].push(start,start+1,start+2,start,start+2,start+3)}
}
const encode=(bytes)=>{let chars='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/',out='';for(let i=0;i<bytes.length;i+=3){let n=(bytes[i]<<16)|((bytes[i+1]||0)<<8)|(bytes[i+2]||0);out+=chars[(n>>>18)&63]+chars[(n>>>12)&63]+(i+1<bytes.length?chars[(n>>>6)&63]:'=')+(i+2<bytes.length?chars[n&63]:'=')}return out};
let buffers=[],views=[],accessors=[],offset=0;
function add(array,componentType,type,count,target){let bytes=new Uint8Array(array.buffer);while(offset%4){buffers.push(0);offset++}let at=offset;buffers.push(...bytes);offset+=bytes.length;let vi=views.push({buffer:0,byteOffset:at,byteLength:bytes.length,target})-1;let ai=accessors.push({bufferView:vi,componentType,count,type})-1;return ai}
let pa=add(new Float32Array(pos),5126,'VEC3',pos.length/3,34962),ta=add(new Float32Array(uv),5126,'VEC2',uv.length/2,34962);
let primitives=groups.map((g,i)=>({attributes:{POSITION:pa,TEXCOORD_0:ta},indices:add(new Uint16Array(g),5123,'SCALAR',g.length,34963),material:i,mode:4})).filter(p=>accessors[p.indices].count);
let gltf={asset:{version:'2.0',generator:'Original RAZLOM ZONA procedural kit'},scene:0,scenes:[{nodes:[0]}],nodes:[{mesh:0,name:'BunkerEntrance_4m'}],meshes:[{primitives}],materials:colors.map((c,i)=>({name:['concrete','steel','rust','warning_red'][i],pbrMetallicRoughness:{baseColorFactor:c,metallicFactor:i===0?0:0.65,roughnessFactor:.85},doubleSided:false})),buffers:[{byteLength:buffers.length,uri:'data:application/octet-stream;base64,'+encode(buffers)}],bufferViews:views,accessors};
process.stdout.write(JSON.stringify(gltf));
