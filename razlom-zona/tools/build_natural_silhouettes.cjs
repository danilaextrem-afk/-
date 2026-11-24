// Original spruce and rubble: node tools/build_natural_silhouettes.cjs
const fs=require("node:fs"),path=require("node:path");
function build(kind,low=false){
 const groups=[{p:[],n:[],u:[],i:[]},{p:[],n:[],u:[],i:[]}];let state=14791;function rand(){state=(Math.imul(state,1664525)+1013904223)>>>0;return state/4294967296}
 const add=(a,b)=>a.map((x,i)=>x+b[i]),sub=(a,b)=>a.map((x,i)=>x-b[i]),mul=(a,k)=>a.map(x=>x*k),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=a=>mul(a,1/Math.hypot(...a));
 function face(p,t,g=0){const o=groups[g],n=unit(cross(sub(p[1],p[0]),sub(p[2],p[0]))),j=o.p.length/3;for(let k=0;k<p.length;k++){o.p.push(...p[k]);o.n.push(...n);const [u,v]=[[0,1],[1,1],[1,0],[0,0]][k];o.u.push((t%8*128+1+126*u)/1024,(Math.floor(t/8)*128+1+126*v)/512)}for(let k=1;k<p.length-1;k++)o.i.push(j,j+k,j+k+1)}
 function tube(a,b,r,s,t,sides){const d=unit(sub(b,a)),ax=unit(cross(d,Math.abs(d[1])>.9?[1,0,0]:[0,1,0])),ay=cross(d,ax);const ring=Array.from({length:sides},(_,i)=>add(mul(ax,Math.cos(i*2*Math.PI/sides)),mul(ay,Math.sin(i*2*Math.PI/sides))));for(let i=0;i<sides;i++){let j=(i+1)%sides;face([add(a,mul(ring[j],r)),add(a,mul(ring[i],r)),add(b,mul(ring[i],s)),add(b,mul(ring[j],s))],t)}}
 if(kind==="spruce"){
 const height=8.75;
 const trunk=y=>[.085*Math.sin(y*.74),y,.08*Math.sin(y*.91+.5)];
 for(let i=0;i<7;i++)tube(trunk(height*i/7),trunk(height*(i+1)/7),.19*(1-i/7)+.014,.19*(1-(i+1)/7)+.014,9,low?5:7);
 for(let j=0;j<(low?3:5);j++){let a=j*2*Math.PI/5;tube([0,.28,0],[.48*Math.cos(a),.015,.48*Math.sin(a)],.105,.027,9,5)}
 const levels=low?8:13,branches=low?5:7;
 for(let k=0;k<levels;k++){let f=k/levels,y=.90+f*7.6,len=2.10*Math.pow(1-f,.78)+.17;for(let j=0;j<branches;j++){
  let a=k*2.37+j*2*Math.PI/branches+(rand()-.5)*.5,dir=[Math.cos(a),0,Math.sin(a)],side=[-dir[2],0,dir[0]],base=trunk(y+(rand()-.5)*.24),reach=len*(.75+.43*rand()),tip=add(base,add(mul(dir,reach),[0,-.17-.40*(1-f),0])),mid=add(base,add(mul(dir,reach*.57),[0,-.055,0]));
  if(!low&&j%2===0)tube(base,mid,.025*(1-f)+.008,.012,9,5);
  // Flat needle fans overlap at several angles; no bare giant triangular ribs.
  for(let q=0;q<(low?1:3);q++){let from=add(base,add(mul(dir,reach*(q*.17)),[0,.04*q,0])),end=add(tip,add(mul(side,(q-1)*reach*.22),[0,.06*q,0])),width=reach*(low?.40:.34)*(1-.13*q),up=[0,.17+reach*.15,0];
   face([add(from,mul(side,-width*.35)),add(end,mul(side,-width)),add(end,add(mul(side,width),up)),add(from,add(mul(side,width*.35),up))],24,1);
   if(!low&&q!==1)face([add(mid,mul(side,-width*.63)),add(end,mul(side,-width*.30)),add(end,[0,.38*reach,0]),add(mid,[0,.26*reach,0])],24,1);
  }
 }}
 }else{
 // Three overlapping chipped stones, random faceted contours, no square tops.
 for(let i=0;i<(low?2:4);i++){let cx=[0,.30,-.28,.11][i],cz=[0,.11,-.15,-.28][i],r=[.46,.24,.21,.18][i],h=[.31,.20,.17,.13][i],n=low?5:7,ring=[],top=[];for(let k=0;k<n;k++){const a=k*Math.PI*2/n,rr=r*(.8+.30*rand());ring.push([cx+Math.cos(a)*rr,.01,cz+Math.sin(a)*rr*.71]);top.push([cx+Math.cos(a)*rr*.62,h*(.72+.35*rand()),cz+Math.sin(a)*rr*.52])}for(let k=0;k<n;k++){let j=(k+1)%n;face([ring[k],top[k],top[j],ring[j]],8);face([[cx,h*.94,cz],top[j],top[k]],8)}}
 }
 const chunks=[],views=[],acc=[];let off=0;
 function pack(v,k,w,ix=false){const bytes=new Uint8Array(v.length*(ix?2:4)),dv=new DataView(bytes.buffer);v.forEach((x,i)=>ix?dv.setUint16(i*2,x,true):dv.setFloat32(i*4,x,true));const pad=(4-off%4)%4;if(pad){chunks.push(new Uint8Array(pad));off+=pad}views.push({buffer:0,byteOffset:off,byteLength:bytes.length,target:ix?34963:34962});chunks.push(bytes);off+=bytes.length;const a={bufferView:views.length-1,componentType:ix?5123:5126,count:v.length/w,type:k};if(k==="VEC3"){a.min=[0,1,2].map(k=>Math.min(...v.filter((_,i)=>i%3===k)));a.max=[0,1,2].map(k=>Math.max(...v.filter((_,i)=>i%3===k)))}acc.push(a);return acc.length-1}
 const primitives=groups.filter(g=>g.p.length).map((g,i)=>({attributes:{POSITION:pack(g.p,"VEC3",3),NORMAL:pack(g.n,"VEC3",3),TEXCOORD_0:pack(g.u,"VEC2",2)},indices:pack(g.i,"SCALAR",1,true),material:i}));
 const bytes=new Uint8Array(off);let pos=0;for(const c of chunks){bytes.set(c,pos);pos+=c.length}const abc="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";let b64="";for(let i=0;i<off;i+=3){const n=bytes[i]*65536+(bytes[i+1]||0)*256+(bytes[i+2]||0);b64+=abc[(n>>18)&63]+abc[(n>>12)&63]+(i+1<off?abc[(n>>6)&63]:"=")+(i+2<off?abc[n&63]:"=")}
 const name=(kind==="spruce"?"spruce_rework":"broken_stones")+(low?"_lod":"");const doc={asset:{version:"2.0",generator:"RAZLOM natural silhouette workshop"},scene:0,scenes:[{nodes:[0]}],nodes:[{name,mesh:0}],meshes:[{primitives}],materials:[{name:"Bark and rock",doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:1}},{name:"Needle fans",doubleSided:true,alphaMode:"MASK",alphaCutoff:.42,pbrMetallicRoughness:{baseColorTexture:{index:0},roughnessFactor:1}}],images:[{uri:"district.png"}],textures:[{source:0,sampler:0}],samplers:[{magFilter:9728,minFilter:9984}],buffers:[{byteLength:off,uri:"data:application/octet-stream;base64,"+b64}],bufferViews:views,accessors:acc};
 return {name,json:JSON.stringify(doc)+"\n",triangles:groups.reduce((s,g)=>s+g.i.length/3,0)};
}
for(const kind of ["spruce","stones"])for(const low of [false,true]){const m=build(kind,low);fs.writeFileSync(path.join(__dirname,"../assets/zalesye",m.name+".gltf"),m.json);console.log(m.name,m.triangles)}
