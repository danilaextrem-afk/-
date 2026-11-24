// Original industrial landmarks. node tools/build_industrial_skyline.cjs
const fs=require("node:fs"),path=require("node:path");
function industrial(kind,low=false){
 const P=[],N=[],U=[],I=[];const sub=(a,b)=>a.map((v,i)=>v-b[i]);
 function face(p,t){const a=sub(p[1],p[0]),b=sub(p[2],p[0]),n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],l=Math.hypot(...n);if(l<1e-8)throw Error("degenerate");const start=P.length/3;for(let k=0;k<p.length;k++){P.push(...p[k]);N.push(...n.map(x=>x/l));const [u,v]=[[0,1],[1,1],[1,0],[0,0]][k];U.push((t%8*128+1+126*u)/1024,(Math.floor(t/8)*128+1+126*v)/512);}for(let k=1;k<p.length-1;k++)I.push(start,start+k,start+k+1);}
 function box(x,y,z,w,h,d,t){const a=x-w/2,b=x+w/2,c=y-h/2,e=y+h/2,f=z-d/2,g=z+d/2;const p=[[a,c,f],[b,c,f],[b,e,f],[a,e,f],[a,c,g],[b,c,g],[b,e,g],[a,e,g]];[[3,2,1,0],[4,5,6,7],[0,4,7,3],[5,1,2,6],[7,6,2,3],[0,1,5,4]].forEach(q=>face(q.map(k=>p[k]),t));}
 function facade(w,d,h,front){const z=d/2*front,sg=front;const bays=kind==="admin"?4:3,span=w/bays;for(let j=0;j<bays;j++){const x=-w/2+(j+.5)*span;const bottom=kind==="admin"?.84:1.0,windowH=kind==="admin"?1.52:1.42;const levels=kind==="admin"?[2.08,5.25]:[h-1.75];let y=0;
 for(const center of levels){const a=center-windowH/2,b=center+windowH/2;box(x,(a+y)/2,z,span,a-y,.24,kind==="admin"?1:0);box(x,b-.5*windowH,z-.16*sg,span-.78,windowH,.07,12);for(const edge of [-1,1])box(x+edge*(span-.40)/2,center,z,.40,windowH,.24,0);box(x,a-.06,z+.09*sg,span-.54,.12,.33,8);box(x,b+.06,z+.08*sg,span-.54,.12,.31,8);if(!low){for(const f of [-.25,0,.25])box(x+f*(span-.8),center,z+.035*sg,.045,windowH,.09,6);box(x,center,z+.045*sg,span-.8,.06,.10,6);}y=b;}
 box(x,(y+h)/2,z,span,h-y,.24,kind==="admin"?1:0);
 if(!low)box(x-span/2+.10,h/2,z+.10*sg,.19,h,.32,0);
 }}
 const w=kind==="admin"?11:15,d=kind==="admin"?9:12,h=kind==="admin"?7.65:7.05;
 box(0,.21,0,w+.2,.42,d+.20,8);
 for(const sign of [-1,1])box(sign*(w/2-.12),h/2,0,.24,h,d,0);
 facade(w,d,h,1);
 if(kind==="admin"){facade(w,d,h,-1);box(0,h+.10,0,w+.55,.28,d+.55,8);
 for(const s of [-1,1]){box(s*(w/2-.15),h+.5,0,.23,.78,d+.30,0);box(0,h+.50,s*(d/2-.1),w,.78,.24,0);}
 box(-3.0,h+.78,-1.8,2.2,1.08,2.4,1);box(-3,h+1.35,-1.8,2.48,.17,2.66,6);
 // Entry has a distinct projecting canopy and boarded double door.
 box(0,.95,d/2+.20,1.9,1.90,.10,7);box(0,2.55,d/2+.56,2.72,.24,1.3,8);
 for(const s of [-1,1]){box(s*1.18,1.2,d/2+.9,.18,2.4,.18,0);if(!low)box(s*.48,1.02,d/2+.27,.88,.14,.09,2);}
 if(!low){box(4.7,3.85,-d/2-.13,.10,7.1,.13,6);for(let i=0;i<9;i++)box(4.7,.55+i*.78,-d/2-.20,.31,.035,.17,6);for(const s of [-1,1])for(let j=0;j<4;j++){const z=-3.2+j*2.0;box(s*(w/2+.09),4.6,z,.11,2.15,1.3,12);box(s*(w/2+.16),4.6,z,.07,2.15,.08,6);}}
 }else{
 // A loading facade of heavy shutter doors under a wide canopy.
 const z=-d/2;
 for(let j=0;j<3;j++){const x=-5+j*5;box(x,2.22,z+.06,3.8,4.44,.22,6);
 if(!low)for(let k=0;k<13;k++)box(x,.23+k*.31,z-.09,3.74,.045,.09,5);
 for(const s of [-1,1])box(x+s*2.18,3.5,z,.63,7,.40,0);
 box(x,5.65,z,4.38,2.42,.24,0);}
 box(0,4.62,z-.50,w+.45,.26,1.26,5);
 for(let j=0;j<4;j++)box(-7.1+j*4.73,2.34,z-.91,.18,4.68,.18,6);
 // Sawtooth roof: three long roof bays, metal caps and clerestory strip.
 const ridge=8.6;
 for(let j=0;j<3;j++){const x=-w/2+j*5,xx=x+5;
 face([[x-.14,h,-d/2-.25],[x-.14,h,d/2+.25],[xx-.70,ridge,d/2+.25],[xx-.70,ridge,-d/2-.25]],5);
 face([[xx-.70,ridge,-d/2-.25],[xx-.70,ridge,d/2+.25],[xx+.03,h,d/2+.25],[xx+.03,h,-d/2-.25]],12);
 for(const sg of [-1,1])face([[x,h,sg*d/2],[xx-.70,ridge,sg*d/2],[xx,h,sg*d/2]],0);
 if(!low)for(let k=0;k<6;k++)box(xx-.56,7.78,-5.3+k*2.1,.13,1.43,.10,6);
 }
 box(5.85,8.95,3,.55,2.38,.55,0);box(5.85,10.18,3,.78,.18,.78,8);
 }
 const chunks=[],views=[],acc=[];let off=0;
 function pack(v,k,w,ix=false){const bytes=new Uint8Array(v.length*(ix?2:4)),dv=new DataView(bytes.buffer);v.forEach((x,i)=>ix?dv.setUint16(i*2,x,true):dv.setFloat32(i*4,x,true));const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad;}views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:ix?34963:34962});chunks.push(bytes);off+=bytes.length;const a={bufferView:views.length-1,componentType:ix?5123:5126,count:v.length/w,type:k};if(v===P){a.min=[0,1,2].map(k=>Math.min(...v.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...v.filter((_,i)=>i%3===k)));}acc.push(a);return acc.length-1;}
 const attrs={POSITION:pack(P,"VEC3",3),NORMAL:pack(N,"VEC3",3),TEXCOORD_0:pack(U,"VEC2",2)},indices=pack(I,"SCALAR",1,true);
 const bytes=new Uint8Array(off);let pos=0;for(const c of chunks){bytes.set(c,pos);pos+=c.length}const abc="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let b64="";for(let i=0;i<off;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);b64+=abc[(n>>18)&63]+abc[(n>>12)&63]+(i+1<off?abc[(n>>6)&63]:"=")+(i+2<off?abc[n&63]:"=");}
 const name=(kind==="admin"?"industrial_admin":"industrial_warehouse")+(low?"_lod":"");const doc={asset:{version:"2.0",generator:"RAZLOM industrial skyline workshop"},scene:0,scenes:[{nodes:[0]}],nodes:[{name,mesh:0}],meshes:[{primitives:[{attributes:attrs,indices,material:0}]}],materials:[{name:"Weathered industrial masonry",doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:.94}}],images:[{uri:"../zalesye/district.png"}],textures:[{source:0,sampler:0}],samplers:[{magFilter:9728,minFilter:9984}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+b64}],bufferViews:views,accessors:acc};
 return {name,json:JSON.stringify(doc)+"\n",triangles:I.length/3,bounds:[acc[0].min,acc[0].max]};
}
for(const kind of ["admin","warehouse"])for(const low of [false,true]){const m=industrial(kind,low);fs.writeFileSync(path.join(__dirname,"../assets/base",m.name+".gltf"),m.json);console.log(m.name,m.triangles,m.bounds);}
