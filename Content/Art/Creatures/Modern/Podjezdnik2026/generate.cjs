// Original Podjezdnik procedural source. Run: node generate.cjs
const fs=require('fs');
const buildMesh=function buildMesh(segments=12){
 const v=[],uv=[],faces=[],groups=[];let current="shadow_skin";
 const add=(p,t)=>{v.push(p);uv.push(t);return v.length};
 const tri=(a,b,c)=>faces.push([a,b,c,current]);
 function tube(points,radii,sides=segments,material="shadow_skin"){
 current=material;let rings=[];
 for(let i=0;i<points.length;i++){let p=points[i],prev=points[Math.max(0,i-1)],next=points[Math.min(points.length-1,i+1)];let axis=[next[0]-prev[0],next[1]-prev[1],next[2]-prev[2]];let al=Math.hypot(...axis)||1;axis=axis.map(x=>x/al);let ref=Math.abs(axis[2])<.9?[0,0,1]:[0,1,0];let u=[axis[1]*ref[2]-axis[2]*ref[1],axis[2]*ref[0]-axis[0]*ref[2],axis[0]*ref[1]-axis[1]*ref[0]];let ul=Math.hypot(...u);u=u.map(x=>x/ul);let w=[axis[1]*u[2]-axis[2]*u[1],axis[2]*u[0]-axis[0]*u[2],axis[0]*u[1]-axis[1]*u[0]];let ring=[];for(let j=0;j<sides;j++){let a=2*Math.PI*j/sides,rr=radii[i]*(1+.045*Math.sin(i*3+j*5));ring.push(add(p.map((x,k)=>x+rr*(Math.cos(a)*u[k]+Math.sin(a)*w[k])),[j/sides,i/(points.length-1)]));}rings.push(ring);}
 for(let i=0;i<rings.length-1;i++)for(let j=0;j<sides;j++){let a=rings[i][j],b=rings[i][(j+1)%sides],c=rings[i+1][j],d=rings[i+1][(j+1)%sides];tri(a,b,c);tri(b,d,c);}
 let start=add(points[0],[.5,0]),end=add(points[points.length-1],[.5,1]);for(let j=0;j<sides;j++){tri(start,rings[0][(j+1)%sides],rings[0][j]);tri(end,rings[rings.length-1][j],rings[rings.length-1][(j+1)%sides]);}
 }
 tube([[0,0,.9],[0,0,1.25],[0,-.06,1.65],[0,-.12,2.05],[0,-.13,2.28]],[.23,.24,.18,.14,.095],segments,"shadow_skin");
 tube([[0,-.12,2.28],[0,-.17,2.46],[0,-.18,2.67]],[.11,.13,.09],segments,"shadow_skin");
 for(const s of [-1,1]){
 tube([[s*.15,0,1.01],[s*.18,.02,.7],[s*.20,-.02,.35],[s*.23,-.16,.08]],[.13,.115,.075,.035],segments,"shadow_skin");
 tube([[s*.15,-.06,2.11],[s*.33,-.08,2.02],[s*.45,-.19,1.62],[s*.51,-.32,1.16],[s*.53,-.38,.91]],[.115,.105,.08,.05,.028],segments,"shadow_skin");
 for(let j=0;j<3;j++){let x=s*(.49+j*.035);tube([[x,-.37,.94],[x+s*.025,-.42,.77],[x+s*.045,-.48,.67]],[.024,.015,.004],Math.max(6,Math.floor(segments/2)),"shadow_skin");}
 }
 // hanging cloth-like occult mantle, jagged hems, surface not a primitive
 current="torn_mantle";let top=[],bottom=[];for(let j=0;j<segments;j++){let a=2*Math.PI*j/segments,rad=.24;top.push(add([rad*Math.cos(a),rad*Math.sin(a)-.10,2.22],[j/segments,1]));let r=.39+.045*Math.sin(j*5);bottom.push(add([r*Math.cos(a),r*Math.sin(a)-.10,1.34+.18*Math.sin(j*7)],[j/segments,0]));}
 for(let j=0;j<segments;j++){let n=(j+1)%segments;tri(top[j],bottom[j],top[n]);tri(top[n],bottom[j],bottom[n]);}
 // void face inset and tiny pale eyes: distinct from robot
 tube([[0,-.292,2.52],[0,-.303,2.55]],[.072,.07],Math.max(8,segments),"face_void");
 for(const s of [-1,1])tube([[s*.045,-.366,2.565],[s*.045,-.370,2.566]],[.012,.011],6,"cold_glint");
 let obj="# Original procedural occult creature; meters, Z up; geometry WIP\nmtllib podjezdnik.mtl\n";
 for(const p of v)obj+=`v ${p.map(x=>x.toFixed(5)).join(" ")}\n`;
 for(const t of uv)obj+=`vt ${t.map(x=>x.toFixed(5)).join(" ")}\n`;
 let last="";for(const [a,b,c,m] of faces){if(m!==last){obj+=`usemtl ${m}\n`;last=m;}obj+=`f ${a}/${a} ${b}/${b} ${c}/${c}\n`;}
 return {obj,vertices:v.length,triangles:faces.length,uv:uv.length,invalid:faces.filter(f=>f.slice(0,3).some(i=>i<1||i>v.length)||new Set(f.slice(0,3)).size<3).length};
};
for(const [name,sides] of [['LOD0',16],['LOD1',8]]){const mesh=buildMesh(sides);fs.writeFileSync('podjezdnik_'+name+'.obj',mesh.obj);console.log(name,mesh.vertices,mesh.triangles,mesh.invalid);}
