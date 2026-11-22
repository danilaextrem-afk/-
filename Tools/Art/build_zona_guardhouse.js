// Original procedural source geometry for RAZLOM: ZONA Guardhouse 04
// Run: node Tools/Art/build_zona_guardhouse.js [output_dir]
// Output: SM_Guardhouse04_v1.gltf and source-geometry preview SVG
const fs=require('node:fs'),path=require('node:path');
const buildAll=function buildAll(){
 const palette={
   concrete:"#666a69", concreteDark:"#42484a", plaster:"#898479", plasterAlt:"#77766c",
   brick:"#5b5752", brickDark:"#3e4345", metal:"#3e4b4c", metalHi:"#707877",
   rust:"#92563c", rustDark:"#593b33", wood:"#655244", woodDark:"#3e332e",
   glass:"#263e48", void:"#141f25", cyan:"#50dccd", cyanDark:"#327e79",
   grime:"#394438", yellow:"#b69b55", black:"#22282a", paper:"#b9aa83"
 };
 const faces=[];let elements=0;
 const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
 function quad(name,mat,v){
   let a=v[0],b=v[1],c=v[2],u=[b[0]-a[0],b[1]-a[1],b[2]-a[2]],w=[c[0]-a[0],c[1]-a[1],c[2]-a[2]];
   let n=cross(u,w),len=Math.hypot(...n)||1;n=n.map(t=>t/len);
   faces.push({name,mat,v,n});
 }
 function box(name,x,y,z,sx,sy,sz,mat,rx=0,rz=0){
   elements++;
   const hx=sx/2,hy=sy/2,hz=sz/2;
   const c=[[-hx,-hy,-hz],[hx,-hy,-hz],[hx,hy,-hz],[-hx,hy,-hz],[-hx,-hy,hz],[hx,-hy,hz],[hx,hy,hz],[-hx,hy,hz]];
   const cx=Math.cos(rx),ix=Math.sin(rx),cz=Math.cos(rz),iz=Math.sin(rz);
   let verts=c.map(a=>{let xx=a[0], yy=a[1]*cx-a[2]*ix, zz=a[1]*ix+a[2]*cx;return [x+xx*cz-yy*iz,y+xx*iz+yy*cz,z+zz]});
   for(const f of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[1,2,6,5],[0,4,7,3]])quad(name,mat,f.map(i=>verts[i]));
 }
 // 6.8 x 5.4 m Soviet perimeter checkpoint, detailed original mobile-friendly assembled kit
 box("collision_foundation",0,0,0.13,6.85,5.28,.26,"concreteDark");
 for(let i=0;i<9;i++)box("slab_joint_"+i,-3.0+i*.75,-2.38,.268,.03,.24,.008,"black");
 // Wall masses divided into door and window apertures; real openings, not decals
 box("back_wall",0,2.27,1.64,5.95,.28,2.78,"plaster");
 box("left_wall",-2.98,0,1.64,.28,4.55,2.78,"plasterAlt");
 box("right_wall",2.98,0,1.64,.28,4.55,2.78,"plasterAlt");
 box("front_left",-2.33,-2.27,1.63,1.29,.28,2.78,"plasterAlt");
 box("front_door_window_pier",.34,-2.27,1.63,.61,.28,2.78,"plaster");
 box("front_right",2.66,-2.27,1.63,.6,.28,2.78,"plasterAlt");
 box("front_top",.53,-2.27,2.72,4.34,.28,.60,"plaster");
 box("front_under_window",1.62,-2.27,.57,1.57,.28,.64,"plaster");
 box("front_window_top",1.62,-2.27,2.27,1.57,.28,.37,"plasterAlt");
 // door free opening x [-1.69,-.0?] from z 0.3 to 2.35; real panel is separate node
 box("door_dark_reveal",-1.10,-2.28,1.25,1.64,.10,1.99,"void");
 for (const xx of [-1.96,-.22])box("door_frame_vertical_"+xx,xx,-2.48,1.27,.12,.19,2.25,"rustDark");
 box("door_frame_header",-1.09,-2.49,2.38,1.88,.20,.13,"metal");
 box("door_leaf",-1.14,-2.56,1.22,1.56,.13,1.94,"metal");
 for(let i=0;i<9;i++)box("door_vertical_corrosion_"+i,-1.82+i*.165,-2.644,1.25,.025,.012,1.76,i%3===0?"rust":"rustDark");
 for(let i=0;i<5;i++)box("door_lower_rivet_strip_"+i,-1.74+i*.31,-2.655,.37,.028,.028,.065,"metalHi");
 box("door_horizontal_brace",-1.14,-2.67,1.85,1.49,.07,.105,"rustDark");
 box("door_handle_plate",-.49,-2.67,1.21,.11,.05,.30,"metalHi");
 box("door_handle",-.42,-2.71,1.15,.18,.07,.055,"yellow");
 box("door_bottom_threshold",-1.14,-2.69,.286,1.7,.24,.055,"rust");
 // upper transom vent cutout is actual dark patch with many bars
 box("door_grill_void",-1.15,-2.667,1.94,1.05,.012,.24,"void");
 for(let i=0;i<10;i++)box("door_grill_"+i,-1.64+i*.11,-2.691,1.94,.024,.028,.26,"metalHi");
 // window: inset, thick trim, two panes, rust mesh, plywood cover
 box("window_recess",1.62,-2.28,1.49,1.62,.10,1.43,"void");
 box("window_glazing",1.62,-2.36,1.48,1.45,.045,1.27,"glass");
 for(let x of [.86,2.38])box("window_frame_side_"+x,x,-2.48,1.49,.13,.17,1.50,"rustDark");
 for(let z of [.72,2.25])box("window_frame_topbottom_"+z,1.62,-2.48,z,1.68,.18,.105,"metal");
 box("window_mid_cross",1.60,-2.49,1.49,.09,.17,1.35,"metalHi");
 for(let j=0;j<5;j++)box("window_rebar_x_"+j,1.00+j*.31,-2.56,1.47,.035,.055,1.37,"rust");
 for(let j=0;j<4;j++)box("window_rebar_z_"+j,1.62,-2.559,.94+j*.34,1.48,.045,.035,"rustDark");
 for(let i=0;i<3;i++)box("window_board_"+i,1.68,-2.648,1.34+i*.38,1.47,.11,.21,"wood",0,i%2===0?.13:-.11);
 for(let i=0;i<10;i++)box("window_nail_"+i,1.0+(i%5)*.34,-2.713,1.28+Math.floor(i/5)*.76,.041,.023,.042,"metalHi");
 box("window_sill",1.62,-2.63,.688,1.91,.39,.16,"concreteDark");
 // roof 17 corrugated plates with ridges, correct single slope and exposed timber trim
 const slope=-.077;
 for(let i=0;i<17;i++){
  const x=-3.15+i*.395;
  box("roof_sheet_"+i,x,0,3.41,.392,5.50,.074,i%4===0?"rustDark":(i%5===0?"rust":"metal"),slope);
  box("roof_ridge_"+i,x+.135,0,3.467,.044,5.48,.042,i%4===0?"rust":"metalHi",slope);
  if(i%3===0)box("roof_corrosion_patch_"+i,x+.05,-.65,3.51,.155,.48,.015,"rust",slope);
 }
 for(let x of [-2.9,0,2.9])box("roof_under_beam_"+x,x,-2.18,3.10,.18,.16,.38,"woodDark",slope);
 for(let y of [-2.65,2.65])box("roof_fascia_"+y,0,y,3.41+(y*slope),6.8,.15,.23,"woodDark");
 // rough plaster chipped faces and corner brick repairs, individually recessed tiles
 for(let side of [-1,1]){
  for(let row=0;row<7;row++)for(let col=0;col<3;col++){
   const z=.48+row*.34,x=side*(2.50+col*.125);
   box("corner_brick_front_"+side+"_"+row+"_"+col,x,-2.425,z,.12,.031,.13,(row+col)%4===0?"rust":"brickDark");
  }
 }
 for(let i=0;i<28;i++){
  const zz=.37+(i%14)*.16, y=-2.00+Math.floor(i/14)*.17;
  box("wall_dirt_left_"+i,-2.89,y,zz,.07,.065,.09,i%3?"grime":"brickDark");
 }
 // exposed conduits and power box to the right of front window
 box("electrical_box",2.67,-2.56,1.43,.48,.37,.68,"metal");
 box("electrical_door",2.67,-2.768,1.43,.43,.025,.61,"metalHi");
 for(let i=0;i<3;i++)box("electrical_hazard_"+i,2.50+i*.16,-2.791,1.39,.11,.023,.11,"yellow",0,-.12);
 box("electrical_lock",2.84,-2.814,1.38,.038,.032,.085,"black");
 box("power_conduit_vertical",2.76,-2.38,2.36,.074,.082,1.38,"metal");
 box("power_conduit_bend",2.65,-2.38,3.07,.38,.09,.074,"metalHi");
 // wall signage and physical stencil "04" in front
 box("sign_base",-2.30,-2.48,2.50,.71,.064,.40,"metal");
 function seg(d,x,y,z,w=.10,h=.042){
  const segments={
   "0":[[0,.12,.10,.034],[0,-.12,.10,.034],[-.052,0,.034,.27],[.052,0,.034,.27]],
   "4":[[-.052,.064,.034,.14],[.052,0,.034,.27],[0,0,.10,.034]]
  };
  for(let [dx,dz,ww,hh] of segments[d])box("number_"+d+"_"+elements,x+dx,y,z+dz,ww,.022,hh,"paper");
 }
 seg("0",-2.48,-2.52,2.49);seg("4",-2.23,-2.52,2.49);
 // downspout side
 for(let j=0;j<5;j++)box("drain_pipe_"+j,3.12,1.70,.45+j*.62,.072,.072,.59,j===2?"rust":"metal");
 box("drain_elbow",3.12,1.48,.28,.072,.50,.072,"metalHi");
 for(let i=0;i<4;i++)box("drain_clamp_"+i,3.12,1.70,.8+i*.65,.18,.13,.09,"rustDark");
 // sideboard utility shutter and ventilation slats
 box("side_vent_plate",3.135,.0,1.76,.061,1.40,.94,"metal");
 box("side_vent_inset",3.176,0,1.76,.042,1.24,.70,"void");
 for(let j=0;j<9;j++)box("side_vent_slats_"+j,3.217,0,1.46+j*.075,.035,1.13,.034,j%3===0?"rust":"metalHi");
 // foundation stairs, cracked concrete
 for(let i=0;i<3;i++)box("entrance_step_"+i,-1.14,-2.87-i*.35,.245-i*.065,1.90,.56,.12,"concrete");
 for(let i=0;i<7;i++)box("crack_stairs_"+i,-1.94+i*.25,-3.16,.315,.025,.16,.008,"black",0,i*.1);
 // outdoor spotlight, protective grille, impact brackets
 box("spotlight_arm",-.10,-2.80,3.18,.13,.78,.13,"rustDark");
 box("spotlight_housing",-.10,-3.22,3.18,.44,.35,.24,"metal");
 box("spotlight_lens",-.10,-3.42,3.18,.29,.045,.15,"cyan");
 for(let x of [-.28,.08])box("spotlight_grate_"+x,x,-3.47,3.18,.021,.03,.17,"black");
 // roof antenna stub and cable brackets, no oversized high poly parts
 box("antenna_base",2.65,1.44,3.58,.28,.28,.18,"rustDark");
 box("antenna_pole",2.65,1.44,4.08,.055,.055,.98,"metalHi");
 box("antenna_cross",2.65,1.44,4.43,.64,.045,.045,"metal");
 for(let i=0;i<6;i++)box("antenna_wire_notches_"+i,2.36+i*.115,1.44,4.45,.028,.048,.07,"rust");
 // flecks of geometry wear to evoke pixel-clustered broken rendering
 for(let i=0;i<55;i++){
   let x=-2.95+((i*47)%53)*.111;
   let z=.55+((i*19)%31)*.075;
   if(x>-1.85&&x<2.5)continue;
   box("weathered_plaster_chip_"+i,x,-2.426,z,.075,.023,.06,(i%3===0?"brickDark":"grime"));
 }
 // glTF 2.0 Y-up, unlit-ish metallic rough color; groups allow door to animate independently
 const allMats=Object.keys(palette);const keyGroups=new Map();
 for(const f of faces){const key=f.name.startsWith("door_")?"DoorLeaf|"+f.mat:(f.name.startsWith("window_")?"Window|"+f.mat:"Guardhouse|"+f.mat);if(!keyGroups.has(key))keyGroups.set(key,[]);keyGroups.get(key).push(f);}
 let bytes=[];const bufferViews=[],accessors=[],meshes=[],nodes=[];
 function appendFloats(arr,numComponents,minmax=false){
  while(bytes.length%4)bytes.push(0);
  let off=bytes.length, min=Array(numComponents).fill(Infinity),max=Array(numComponents).fill(-Infinity);
  for(let k=0;k<arr.length;k++){let val=arr[k],j=k%numComponents;min[j]=Math.min(min[j],val);max[j]=Math.max(max[j],val);
   const ab=new ArrayBuffer(4);new DataView(ab).setFloat32(0,val,true);bytes.push(...new Uint8Array(ab));
  }
  let bi=bufferViews.length;bufferViews.push({buffer:0,byteOffset:off,byteLength:bytes.length-off,target:34962});
  let ac=accessors.length;const a={bufferView:bi,componentType:5126,count:arr.length/numComponents,type:numComponents===3?"VEC3":"VEC2"};
  if(minmax){a.min=min.map(x=>+x.toFixed(5));a.max=max.map(x=>+x.toFixed(5));}
  accessors.push(a);return ac;
 }
 for(const [name, fs] of keyGroups){
  let pos=[],nor=[],uv=[];
  for(const f of fs)for(let i of [0,1,2,0,2,3]){
   const v=f.v[i],n=f.n;
   pos.push(v[0],v[2],-v[1]);nor.push(n[0],n[2],-n[1]);
   const uv4=[[0,0],[1,0],[1,1],[0,1]];uv.push(...uv4[i]);
  }
  const matIndex=allMats.indexOf(name.split("|")[1]);
  const primitive={attributes:{POSITION:appendFloats(pos,3,true),NORMAL:appendFloats(nor,3),TEXCOORD_0:appendFloats(uv,2)},material:matIndex,mode:4};
  const mi=meshes.length;meshes.push({name,primitives:[primitive]});nodes.push({name,mesh:mi});
 }
 // Hand-authored procedural 64x64 pixel-grunge albedo pattern, embedded PNG; reproducible, no external pack
 function texturePNG(){
  const w=64,h=64,rgba=[];
  for(let y=0;y<h;y++){
   rgba.push(0);
   for(let x=0;x<w;x++){
    let xx=x>>1,yy=y>>1;
    let v=((xx*741103597)^(yy*1597334677)^(xx*yy*1376312589))>>>0;
    let g=232+(v%24);
    if(v%29===0)g=173;
    if((xx+yy*3)%43===0)g=191;
    rgba.push(g,g,g,255);
   }
  }
  const len=rgba.length,be=n=>[(n>>>24)&255,(n>>>16)&255,(n>>>8)&255,n&255];
  function crc(arr){let c=0xffffffff;for(let b of arr){c^=b;for(let j=0;j<8;j++)c=(c>>>1)^((c&1)?0xedb88320:0);}return (c^0xffffffff)>>>0;}
  function chunk(tag,data){let tagBytes=tag.split("").map(c=>c.charCodeAt(0)),all=[...tagBytes,...data];return [...be(data.length),...all,...be(crc(all))];}
  let a=1,b=0;for(let v of rgba){a=(a+v)%65521;b=(b+a)%65521;}
  const zlib=[0x78,0x01,0x01,len&255,(len>>>8)&255,(~len)&255,((~len)>>>8)&255,...rgba,...be(((b<<16)|a)>>>0)];
  return [137,80,78,71,13,10,26,10,...chunk("IHDR",[...be(w),...be(h),8,6,0,0,0]),...chunk("IDAT",zlib),...chunk("IEND",[])];
 }
 const materials=allMats.map(k=>{const v=palette[k],rgb=[1,3,5].map(i=>parseInt(v.slice(i,i+2),16)/255);
  return {name:k,pbrMetallicRoughness:{baseColorFactor:[...rgb,1],baseColorTexture:{index:0},metallicFactor:k.includes("metal")||k.includes("rust")?.55:0,roughnessFactor:k==="glass"?.22:.86},emissiveFactor:k==="cyan"?[.05,.55,.51]:[0,0,0],doubleSided:false};
 });
 function b64(arr){let out="",ch="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  for(let i=0;i<arr.length;i+=3){let a=arr[i],b=arr[i+1],c=arr[i+2],v=(a<<16)|((b||0)<<8)|(c||0);
   out+=ch[(v>>>18)&63]+ch[(v>>>12)&63]+(i+1<arr.length?ch[(v>>>6)&63]:"=")+(i+2<arr.length?ch[v&63]:"=");
  }return out;
 }
 const gltf={asset:{version:"2.0",generator:"RAZLOM Zona original code-generated guardhouse v1"},scene:0,scenes:[{nodes:nodes.map((_,i)=>i)}],nodes,meshes,materials,images:[{uri:"data:image/png;base64,"+b64(texturePNG()),mimeType:"image/png"}],textures:[{sampler:0,source:0}],samplers:[{magFilter:9728,minFilter:9728,wrapS:10497,wrapT:10497}],buffers:[{byteLength:bytes.length,uri:"data:application/octet-stream;base64,"+b64(bytes)}],bufferViews,accessors};
 // actual source-geometry orthographic angled preview, hidden-surface painter sorting
 const eye=[.55,-.78,.45], rr=[.814,.581,0],up=[-.265,.371,.890], dot=(a,b)=>a.reduce((s,t,i)=>s+t*b[i],0);
 const fsort=faces.map(f=>({f,depth:dot(f.v.reduce((s,p)=>s.map((x,i)=>x+p[i]/4),[0,0,0]),eye)}))
 .filter(x=>dot(x.f.n,eye)>0.001).sort((a,b)=>a.depth-b.depth);
 const color=(hex,s)=>"#"+[1,3,5].map(i=>Math.max(0,Math.min(255,Math.round(parseInt(hex.slice(i,i+2),16)*s))).toString(16).padStart(2,"0")).join("");
 let polys=fsort.map(({f})=>{
  const light=.63+.28*Math.max(0,dot(f.n,[-.3,-.4,.85]))+.06*Math.max(0,dot(f.n,[.6,-.7,.3]));
  const pts=f.v.map(p=>(740+dot(p,rr)*110).toFixed(1)+","+(570-dot(p,up)*110).toFixed(1)).join(" ");
  return `<polygon points="${pts}" fill="${color(palette[f.mat],light)}" stroke="#0e1418" stroke-width=".42" stroke-linejoin="round"/>`;
 }).join("");
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1480 1030" width="1480" height="1030"><rect width="1480" height="1030" fill="#10191d"/><defs><radialGradient id="ambient"><stop stop-color="#324449"/><stop offset="1" stop-color="#10191d"/></radialGradient></defs><rect width="1480" height="1030" fill="url(#ambient)"/><g opacity=".18" stroke="#7aa1a0" stroke-width="1">${Array.from({length:19},(_,i)=>`<path d="M ${i*100} 0 V 1030"/>`).join("")}</g><g opacity=".20" stroke="#789b99" stroke-width="1">${Array.from({length:13},(_,i)=>`<path d="M 0 ${i*90} H 1480"/>`).join("")}</g>${polys}<text x="75" y="102" font-size="43" font-family="monospace" font-weight="bold" fill="#e4e1d2">RAZLOM : ZONA</text><text x="75" y="145" font-size="21" font-family="monospace" fill="#69bbb4">CHECKPOINT // GUARDHOUSE 04</text><text x="75" y="927" font-family="monospace" font-size="19" fill="#9eb2af">Actual source geometry · orthographic projection</text><text x="75" y="959" font-family="monospace" font-size="16" fill="#83928e">Not Godot gameplay · materials are vertex-flat draft</text></svg>`;
 const groups={};for(let f of faces)groups[f.mat]=(groups[f.mat]||0)+2;
 const stats={elements,quads:faces.length,triangles:faces.length*2,materials:allMats.length,vertices:faces.length*4,renderFacing:fsort.length,bytes:bytes.length,groups};
 return {gltf:JSON.stringify(gltf),svg,stats,materials:palette};
};
const output=buildAll();const dir=process.argv[2]||process.cwd();fs.mkdirSync(dir,{recursive:true});
fs.writeFileSync(path.join(dir,'SM_Guardhouse04_v1.gltf'),output.gltf);
fs.writeFileSync(path.join(dir,'Preview_Guardhouse04_SourceGeometry.svg'),output.svg);
fs.writeFileSync(path.join(dir,'guardhouse_stats.json'),JSON.stringify(output.stats,null,2));
console.log(JSON.stringify(output.stats));
