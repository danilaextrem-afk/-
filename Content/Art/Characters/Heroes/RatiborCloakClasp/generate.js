// Original procedural geometry: Ratibor's cloak clasp. Run: node generate.js
const fs=require('fs'),path=require('path'); const out=__dirname;
let V=[],UV=[],F=[],groups=[];
function vertex(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
function tri(a,b,c,mat){F.push([a,b,c,mat])}
function quad(a,b,c,d,mat){tri(a,b,c,mat);tri(a,c,d,mat)}
function torus(R,r,segments,tube,offset,mat,scale=1){
let rows=[];for(let i=0;i<=segments;i++){let t=2*Math.PI*i/segments,row=[];for(let j=0;j<=tube;j++){let p=2*Math.PI*j/tube,rad=R+r*Math.cos(p);row.push(vertex(offset[0]+scale*rad*Math.cos(t),offset[1]+scale*rad*Math.sin(t),offset[2]+r*Math.sin(p),i/segments,j/tube))}rows.push(row)}
for(let i=0;i<segments;i++)for(let j=0;j<tube;j++)quad(rows[i][j],rows[i+1][j],rows[i+1][j+1],rows[i][j+1],mat)}
function cylinder(a,b,r,seg,mat){let A=[],B=[];for(let i=0;i<seg;i++){let t=2*Math.PI*i/seg,dx=r*Math.cos(t),dy=r*Math.sin(t);A.push(vertex(a[0]+dx,a[1]+dy,a[2],i/seg,0));B.push(vertex(b[0]+dx,b[1]+dy,b[2],i/seg,1))}
for(let i=0;i<seg;i++)quad(A[i],A[(i+1)%seg],B[(i+1)%seg],B[i],mat);
for(let i=1;i<seg-1;i++){tri(A[0],A[i+1],A[i],mat);tri(B[0],B[i],B[i+1],mat)}}
function prism(points,z0,z1,mat){let a=[],b=[];for(let i=0;i<points.length;i++){const p=points[i];a.push(vertex(p[0],p[1],z0,(p[0]+.16)/.32,(p[1]+.16)/.32));b.push(vertex(p[0],p[1],z1,(p[0]+.16)/.32,(p[1]+.16)/.32))}
for(let i=1;i<points.length-1;i++){tri(a[0],a[i+1],a[i],mat);tri(b[0],b[i],b[i+1],mat)}
for(let i=0;i<points.length;i++){let j=(i+1)%points.length;quad(a[i],a[j],b[j],b[i],mat)}}
// centimeters-to-meters UE5 convention: 0.13m overall clasp diameter, Y up in OBJ
torus(.055,.009,96,12,[0,0,.015],'AgedBronze');
torus(.041,.0018,96,8,[0,0,.026],'PolishedBronze');
torus(.034,.0013,96,8,[0,0,.027],'OxidizedBronze');
for(let i=0;i<12;i++){let t=2*Math.PI*i/12;let x=.055*Math.cos(t),y=.055*Math.sin(t);
torus(.004,.0017,12,8,[x,y,.025],i%3===0?'DarkIron':'PolishedBronze')}
for(let i=0;i<6;i++){let t=2*Math.PI*i/6;let x=.029*Math.cos(t),y=.029*Math.sin(t);
cylinder([x,y,.015],[x,y,.029],.0035,12,'DarkIron')}
let star=[];for(let i=0;i<16;i++){let t=Math.PI*2*i/16,rr=i%2?.015:.024;star.push([rr*Math.cos(t),rr*Math.sin(t)])}
prism(star,.008,.012,'PolishedBronze');
torus(.010,.002,32,8,[0,0,.016],'DarkIron');
cylinder([-.062,-.003,.031],[.064,-.003,.031],.0028,20,'DarkIron');
prism([[.066,-.003],[.051,-.009],[.051,.003]],.029,.033,'PolishedBronze');
prism([[-.071,-.014],[-.058,-.014],[-.054,.014],[-.069,.014]],-.004,.003,'Leather');
prism([[.054,-.014],[.071,-.014],[.068,.014],[.055,.014]],-.004,.003,'Leather');
const names=['AgedBronze','PolishedBronze','OxidizedBronze','DarkIron','Leather'];
let obj='# Ratibor cloak clasp original procedural mesh, meters, +Z front\\nmtllib SM_RatiborCloakClasp_v01.mtl\\no SM_RatiborCloakClasp_v01\\n';
for(const v of V)obj+='v '+v.map(x=>x.toFixed(7)).join(' ')+'\\n';
for(const uv of UV)obj+='vt '+uv.map(x=>x.toFixed(6)).join(' ')+'\\n';
let last='';for(const [a,b,c,m] of F){if(m!==last){obj+='usemtl '+m+'\\n';last=m}obj+='f '+[a,b,c].map(i=>i+'/'+i).join(' ')+'\\n'}
fs.writeFileSync(path.join(out,'SM_RatiborCloakClasp_v01.obj'),obj.replace(/\\n/g,'\n'));
const palette={AgedBronze:[66,52,37],PolishedBronze:[146,113,63],OxidizedBronze:[34,75,68],DarkIron:[38,41,43],Leather:[72,43,29]};
let mtl='# Procedural roughness/metalness reference, maps authored below\n';
for(const n of names){const c=palette[n];mtl+='newmtl '+n+'\nKd '+c.map(x=>(x/255).toFixed(4)).join(' ')+'\nKs 0.12 0.12 0.12\nNs 60\nmap_Kd T_'+n+'_BaseColor.ppm\n'}
fs.writeFileSync(path.join(out,'SM_RatiborCloakClasp_v01.mtl'),mtl);
for(const [n,c] of Object.entries(palette))for(const type of ['BaseColor','Roughness','Metallic','Normal']){
let pixels=[];for(let y=0;y<32;y++)for(let x=0;x<32;x++){let noise=((x*19+y*43+x*y*7)%31)-15;let col=type==='BaseColor'?c.map(v=>Math.max(0,Math.min(255,v+noise))):type==='Roughness'?Array(3).fill(n==='Leather'?205:n==='PolishedBronze'?94:155):type==='Metallic'?Array(3).fill(n==='Leather'?0:245):[128,128,255];pixels.push(col.join(' '))}
fs.writeFileSync(path.join(out,'T_'+n+'_'+type+'.ppm'),'P3\n32 32\n255\n'+pixels.join('\n')+'\n')}
const report={asset_id:'RZ_HERO_RATIBOR_CLOAK_CLASP',version:'0.1.0',mesh:'SM_RatiborCloakClasp_v01.obj',vertices:V.length,triangles:F.length,uv_vertices:UV.length,material_slots:names,texture_maps_per_material:['BaseColor','Roughness','Metallic','Normal'],texture_format:'PPM P3 source, convert for UE5',source:'generate.js',rig:'static wearable attachment, socket requested: spine_03',lod:'LOD0 only; LOD1 pending',status:'geometry_wip',engine_validation:'not run',render_validation:'not run',visual_floor:'owner-approved bell v3; not yet matched'};
fs.writeFileSync(path.join(out,'asset_manifest.json'),JSON.stringify(report,null,2)+'\n');
console.log(report);
