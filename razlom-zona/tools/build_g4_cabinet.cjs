// Original G-4 cabinet. Run: node tools/build_g4_cabinet.cjs
const fs = require("node:fs");
const path = require("node:path");
function buildCabinet(low=false){
  const P=[],N=[],U=[],I=[];
  function quad(a,b,c,d,t){
    const u=b.map((v,i)=>v-a[i]),v=c.map((v,i)=>v-a[i]),n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]],l=Math.hypot(...n),j=P.length/3;
    if(l<1e-9)throw Error("degenerate");
    [a,b,c,d].forEach((p,i)=>{P.push(...p);N.push(...n.map(x=>x/l));const uv=[[0,1],[1,1],[1,0],[0,0]][i];U.push((t%4*32+.5+31*uv[0])/128,(Math.floor(t/4)*32+.5+31*uv[1])/64)});
    I.push(j,j+1,j+2,j,j+2,j+3);
  }
  function box(x,y,z,w,h,d,t){
    const a=x-w/2,b=x+w/2,c=y-h/2,e=y+h/2,f=z-d/2,g=z+d/2;
    const p=[[a,c,f],[b,c,f],[b,e,f],[a,e,f],[a,c,g],[b,c,g],[b,e,g],[a,e,g]];
    [[3,2,1,0],[4,5,6,7],[0,4,7,3],[5,1,2,6],[7,6,2,3],[0,1,5,4]].forEach(q=>quad(...q.map(k=>p[k]),t));
  }
  function knob(x,y,z,r,d,t,sides=12){
    const ring=Array.from({length:sides},(_,i)=>[x+r*Math.cos(i*2*Math.PI/sides),y+r*Math.sin(i*2*Math.PI/sides)]);
    for(let i=0;i<sides;i++){
      const a=ring[i],b=ring[(i+1)%sides];quad([...a,z-d],[...b,z-d],[...b,z],[...a,z],t);
    }
    // fan expressed as nondegenerate quads across pairs of opposite sectors.
    for(let i=1;i<sides-1;i+=2){
      if(i+2>=sides)break;
      quad([...ring[0],z],[...ring[i],z],[...ring[i+1],z],[...ring[i+2],z],t);
    }
  }
  // Wide raised plinth, worn steel case, overlapping front access panels.
  box(0,.055,-.035,.78,.11,.41,4);
  box(0,.48,-.035,.72,.75,.36,1);
  box(0,.885,-.035,.80,.07,.43,2);
  box(0,.48,.157,.65,.70,.035,4);
  box(0,.235,.182,.58,.22,.018,1);
  box(0,.565,.182,.58,.36,.024,1);
  box(0,.765,.188,.48,.07,.024,7);
  if(!low){
    // Recessed gauge, raised bezel, range knob, speaker louvers and fasteners.
    box(-.125,.625,.205,.22,.14,.032,4);
    box(-.125,.625,.224,.175,.093,.008,5);
    for(let i=0;i<5;i++)box(-.19+i*.032,.633,.230,.005,.039,.004,6);
    box(-.11,.621,.234,.008,.051,.006,2);
    knob(.16,.625,.251,.064,.045,4);
    knob(.16,.625,.265,.043,.014,2);
    box(.16,.65,.273,.008,.026,.009,6);
    for(let i=0;i<6;i++)box(.01,.38+i*.025,.207,.39,.011,.018,4);
    for(const x of [-.29,.29])for(const y of [.15,.32,.43,.72])box(x,y,.204,.019,.019,.015,6);
    for(const y of [.20,.65])box(-.343,y,.175,.043,.09,.045,2);
    box(.235,.235,.224,.08,.025,.06,2);
    for(const x of [-.32,.32])box(x,.47,-.225,.047,.68,.035,2);
  }
  const chunks=[],views=[],accessors=[];let off=0;
  function add(values,kind,width,indices=false){
    const bytes=new Uint8Array(values.length*(indices?2:4)),dv=new DataView(bytes.buffer);
    values.forEach((v,i)=>indices?dv.setUint16(i*2,v,true):dv.setFloat32(i*4,v,true));
    const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad;}
    views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:indices?34963:34962});chunks.push(bytes);off+=bytes.length;
    const a={bufferView:views.length-1,componentType:indices?5123:5126,count:values.length/width,type:kind};
    if(values===P){a.min=[0,1,2].map(k=>Math.min(...values.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...values.filter((_,i)=>i%3===k)));}
    accessors.push(a);return accessors.length-1;
  }
  const attrs={POSITION:add(P,"VEC3",3),NORMAL:add(N,"VEC3",3),TEXCOORD_0:add(U,"VEC2",2)},index=add(I,"SCALAR",1,true);
  const bytes=new Uint8Array(off);let pos=0;for(const chunk of chunks){bytes.set(chunk,pos);pos+=chunk.length}
  const alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let b64="";
  for(let i=0;i<bytes.length;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);b64+=alphabet[(n>>18)&63]+alphabet[(n>>12)&63]+(i+1<bytes.length?alphabet[(n>>6)&63]:"=")+(i+2<bytes.length?alphabet[n&63]:"=");}
  const name=low?"g4_radio_cabinet_lod":"g4_radio_cabinet";
  const doc={asset:{version:"2.0",generator:"RAZLOM ZONA original cabinet generator"},scene:0,scenes:[{nodes:[0]}],nodes:[{name,mesh:0}],meshes:[{primitives:[{attributes:attrs,indices:index,material:0}]}],materials:[{name:"Aged service enamel",pbrMetallicRoughness:{baseColorTexture:{index:0},metallicFactor:.13,roughnessFactor:.83}}],images:[{uri:"gallery_atlas.png"}],textures:[{sampler:0,source:0}],samplers:[{magFilter:9728,minFilter:9984}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+b64}],bufferViews:views,accessors};
  return {json:JSON.stringify(doc)+"\n",triangles:I.length/3,bounds:[accessors[0].min,accessors[0].max]};
}
for (const low of [false,true]) {
  const m=buildCabinet(low);
  const name=low?"g4_radio_cabinet_lod":"g4_radio_cabinet";
  fs.writeFileSync(path.join(__dirname,"../assets/bunker",name+".gltf"),m.json);
  console.log(name,m.triangles,m.bounds);
}
