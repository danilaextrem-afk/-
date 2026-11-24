// Original articulated watchman. node tools/build_watchman_rework.cjs
const fs=require("node:fs"),path=require("node:path");
function buildWatchman(low=false){
 const nodes=[{name:"Watchman",children:[]}],groups=[];
 const add=(a,b)=>a.map((v,i)=>v+b[i]),sub=(a,b)=>a.map((v,i)=>v-b[i]),mul=(a,k)=>a.map(v=>v*k),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=a=>mul(a,1/Math.hypot(...a));
 let g;
 function node(name,at,parent=0){g={p:[],n:[],u:[],i:[]};let ix=nodes.length;nodes.push({name,translation:at,mesh:groups.length,children:[]});groups.push(g);nodes[parent].children.push(ix);return ix}
 function face(points,tile){const n=unit(cross(sub(points[1],points[0]),sub(points[2],points[0]))),j=g.p.length/3;if(n.some(x=>!Number.isFinite(x)))throw Error("degenerate");for(let k=0;k<points.length;k++){g.p.push(...points[k]);g.n.push(...n);const [u,v]=[[0,1],[1,1],[1,0],[0,0]][k];g.u.push((tile%8*128+1+126*u)/1024,(Math.floor(tile/8)*128+1+126*v)/512)}for(let k=1;k<points.length-1;k++)g.i.push(j,j+k,j+k+1)}
 function box(x,y,z,w,h,d,t){const a=x-w/2,b=x+w/2,c=y-h/2,e=y+h/2,f=z-d/2,hz=z+d/2;const p=[[a,c,f],[b,c,f],[b,e,f],[a,e,f],[a,c,hz],[b,c,hz],[b,e,hz],[a,e,hz]];[[3,2,1,0],[4,5,6,7],[0,4,7,3],[5,1,2,6],[7,6,2,3],[0,1,5,4]].forEach(q=>face(q.map(k=>p[k]),t))}
 // Connected, shaped cross-sections with independently modelled creases.
 function form(rows,t,sides=low?6:10){
 const rings=rows.map(([y,w,d,x,z])=>Array.from({length:sides},(_,i)=>[x+w*Math.cos(i*2*Math.PI/sides),y,z+d*Math.sin(i*2*Math.PI/sides)]));
 for(let r=0;r<rings.length-1;r++)for(let i=0;i<sides;i++){const j=(i+1)%sides;face([rings[r][i],rings[r+1][i],rings[r+1][j],rings[r][j]],t)}
 for(const r of [0,rings.length-1]){const [y,w,d,x,z]=rows[r];for(let i=0;i<sides;i++){const j=(i+1)%sides;face(r===0?[[x,y,z],rings[r][i],rings[r][j]]:[[x,y,z],rings[r][j],rings[r][i]],t)}}
 }
 function tube(a,b,r,s,t,sides=low?5:8){
 const d=unit(sub(b,a)),ax=unit(cross(d,Math.abs(d[1])>.9?[1,0,0]:[0,1,0])),ay=cross(d,ax);
 const ring=Array.from({length:sides},(_,i)=>add(mul(ax,Math.cos(i*2*Math.PI/sides)),mul(ay,Math.sin(i*2*Math.PI/sides))));
 for(let i=0;i<sides;i++){let j=(i+1)%sides;face([add(a,mul(ring[j],r)),add(a,mul(ring[i],r)),add(b,mul(ring[i],s)),add(b,mul(ring[j],s))],t);face([b,add(b,mul(ring[j],s)),add(b,mul(ring[i],s))],t)}
 }
 // Front is -Z, preserve existing gameplay pivots and natural adult scale.
 node("Coat",[0,0,0]);form([[.79,.232,.142,0,.025],[.94,.225,.139,0,.012],[1.10,.176,.126,0,0],[1.28,.227,.158,0,-.01],[1.43,.265,.153,0,-.01],[1.52,.247,.132,0,-.012],[1.59,.125,.104,0,-.008]],28);
 form([[.78,.234,.143,0,.025],[.84,.236,.145,0,.025]],29);
 if(!low){
  for(const s of [-1,1]){
   box(s*.127,1.265,-.15,.126,.175,.035,15);box(s*.127,1.345,-.18,.145,.044,.045,29);
   tube([s*.176,1.50,-.132],[s*.105,.91,-.151],.021,.018,29,5);
   box(s*.172,.96,-.141,.123,.105,.064,29);
  }
  for(let i=0;i<6;i++)box(.005,.93+i*.089,-.145,.012,.032,.013,30);
 }
 box(0,1.29,.177,.30,.37,.15,15);box(0,1.42,.263,.27,.075,.04,29);
 if(!low){for(const s of [-1,1])tube([s*.105,1.46,.264],[s*.105,1.13,.266],.015,.013,29,5);tube([.12,1.31,.26],[.14,1.69,.25],.023,.016,6,6);}
 const head=node("Head",[0,1.60,-.012]);
 form([[-.025,.105,.103,0,0],[.07,.138,.128,0,-.01],[.18,.151,.137,0,0],[.29,.125,.113,0,.017],[.345,.064,.068,0,.025]],28);
 form([[.015,.091,.085,0,-.075],[.085,.12,.098,0,-.088],[.165,.121,.086,0,-.074]],29);
 // Two lens housings, dark recessed glass, respirator and hose.
 for(const s of [-1,1]){tube([s*.061,.151,-.14],[s*.061,.151,-.177],.047,.044,31);tube([s*.061,.151,-.177],[s*.061,.151,-.181],.034,.034,12);}
 tube([0,.044,-.135],[0,.016,-.213],.058,.051,31);
 if(!low){for(let i=0;i<4;i++)box(0,-.012+i*.016,-.257,.064,.005,.008,29);tube([.054,.026,-.171],[.11,-.028,-.121],.025,.020,29,6);tube([.11,-.028,-.121],[.124,-.13,-.092],.020,.018,29,6);}
 for(const [s,side] of [[-1,"L"],[1,"R"]]){
  const arm=node("Arm"+side,[s*.251,1.455,-.006]);
  form([[.046,.112,.105,0,0],[-.06,.121,.108,s*.022,.005],[-.19,.101,.092,s*.032,.008],[-.33,.082,.077,s*.028,0]],28);
  if(!low){box(s*.107,-.096,0,.027,.115,.104,15);for(let k=0;k<3;k++)box(s*.035,-.26-k*.023,-.08,.109,.009,.013,30);}
  node("Forearm"+side,[s*.028,-.33,0],arm);
  form([[.018,.084,.078,0,0],[-.08,.08,.072,s*.012,0],[-.23,.064,.059,s*.016,-.022],[-.29,.059,.055,s*.016,-.035]],28);
  form([[-.29,.06,.055,s*.016,-.035],[-.33,.063,.059,s*.016,-.035]],29);
  form([[-.33,.056,.046,s*.016,-.034],[-.395,.048,.043,s*.016,-.048],[-.425,.029,.030,s*.016,-.055]],29);
  if(!low)for(let k=0;k<3;k++)tube([s*.016+(k-1)*.023,-.391,-.062],[s*.016+(k-1)*.023,-.435,-.067],.012,.008,29,5);
  const leg=node("Leg"+side,[s*.116,.83,.014]);
  form([[.048,.117,.125,0,0],[-.13,.112,.113,s*.007,0],[-.29,.093,.092,s*.010,-.015],[-.39,.081,.085,s*.012,-.021]],28);
  if(!low)box(s*.091,-.17,.01,.052,.16,.145,15);
  node("Shin"+side,[s*.012,-.39,-.021],leg);
  form([[.021,.085,.086,0,0],[-.12,.079,.076,0,.006],[-.25,.064,.067,0,.012],[-.34,.071,.076,0,.006]],28);
  form([[-.28,.076,.084,0,.007],[-.345,.084,.091,0,.002],[-.40,.082,.125,0,-.033],[-.43,.086,.145,0,-.054]],29);
  box(0,-.429,-.060,.172,.039,.29,6);
  if(!low)for(let k=0;k<4;k++)box(0,-.31-k*.023,-.085-k*.014,.084,.012,.015,30);
 }
 const chunks=[],views=[],accessors=[];let off=0;
 function pack(values,kind,width,ix=false){const bytes=new Uint8Array(values.length*(ix?2:4)),dv=new DataView(bytes.buffer);values.forEach((v,i)=>ix?dv.setUint16(i*2,v,true):dv.setFloat32(i*4,v,true));const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad;}views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:ix?34963:34962});chunks.push(bytes);off+=bytes.length;const a={bufferView:views.length-1,componentType:ix?5123:5126,count:values.length/width,type:kind};if(kind==="VEC3"){a.min=[0,1,2].map(k=>Math.min(...values.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...values.filter((_,i)=>i%3===k)));}accessors.push(a);return accessors.length-1;}
 const meshes=groups.map(o=>({primitives:[{attributes:{POSITION:pack(o.p,"VEC3",3),NORMAL:pack(o.n,"VEC3",3),TEXCOORD_0:pack(o.u,"VEC2",2)},indices:pack(o.i,"SCALAR",1,true),material:0}]}));
 const bytes=new Uint8Array(off);let pos=0;for(const c of chunks){bytes.set(c,pos);pos+=c.length}const abc="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let b64="";for(let i=0;i<off;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);b64+=abc[(n>>18)&63]+abc[(n>>12)&63]+(i+1<off?abc[(n>>6)&63]:"=")+(i+2<off?abc[n&63]:"=");}
 for(const n of nodes)if(!n.children.length)delete n.children;
 const doc={asset:{version:"2.0",generator:"RAZLOM original watchman character"},scene:0,scenes:[{nodes:[0]}],nodes,meshes,materials:[{name:"Worn field clothing",doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:.91,metallicFactor:0}}],images:[{uri:"../zalesye/district.png"}],textures:[{source:0,sampler:0}],samplers:[{magFilter:9728,minFilter:9984}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+b64}],bufferViews:views,accessors};
 return {json:JSON.stringify(doc)+"\n",triangles:groups.reduce((a,g)=>a+g.i.length/3,0),joints:nodes.map(n=>n.name)}
}
for(const low of [false,true]){const m=buildWatchman(low);fs.writeFileSync(path.join(__dirname,"../assets/transport",low?"watchman_rework_lod.gltf":"watchman_rework.gltf"),m.json);console.log(m.triangles,m.joints);}
