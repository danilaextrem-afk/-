// Original procedural stairwell electrical panel, Razlom 2026. Node >=18
const fs=require('fs'); const path=require('path');
const out=process.argv[2]||__dirname;
function build(detail=1){
let v=[],uv=[],faces=[],current='steel';const mats=['steel','paint','bakelite','copper','warning'];
function vert(p,t){v.push(p);uv.push(t);return v.length;}
function quad(a,b,c,d,mat){faces.push({ids:[a,b,c],mat});faces.push({ids:[a,c,d],mat});}
function box(cx,cy,cz,sx,sy,sz,mat){
let x=cx-sx/2,X=cx+sx/2,y=cy-sy/2,Y=cy+sy/2,z=cz-sz/2,Z=cz+sz/2;
for(const q of [[[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],[[X,y,z],[x,y,z],[x,Y,z],[X,Y,z]],[[x,y,z],[x,y,Z],[x,Y,Z],[x,Y,z]],[[X,y,Z],[X,y,z],[X,Y,z],[X,Y,Z]],[[x,Y,Z],[X,Y,Z],[X,Y,z],[x,Y,z]],[[x,y,z],[X,y,z],[X,y,Z],[x,y,Z]]]){
const ids=q.map((p,i)=>vert(p,[[0,0],[1,0],[1,1],[0,1]][i]));quad(...ids,mat);
}}
function cyl(cx,cy,cz,r,depth,mat,n=16){
const a=[],b=[];for(let i=0;i<n;i++){const t=i*2*Math.PI/n;const x=cx+Math.cos(t)*r,y=cy+Math.sin(t)*r;a.push(vert([x,y,cz-depth/2],[i/n,0]));b.push(vert([x,y,cz+depth/2],[i/n,1]));}
for(let i=0;i<n;i++){const j=(i+1)%n;quad(a[i],a[j],b[j],b[i],mat);}
const front=vert([cx,cy,cz+depth/2],[.5,.5]);const back=vert([cx,cy,cz-depth/2],[.5,.5]);
for(let i=0;i<n;i++){let j=(i+1)%n;faces.push({ids:[front,b[i],b[j]],mat});faces.push({ids:[back,a[j],a[i]],mat});}
}
box(0,0,0,.76,1.08,.16,'steel');box(0,0,.086,.72,1.03,.025,'paint');
box(0,.37,.109,.62,.16,.013,'steel');box(0,-.39,.11,.63,.12,.015,'steel');
for(let i=0;i<6;i++){let x=-.26+i*.104;box(x,.12,.13,.083,.31,.045,'bakelite');box(x,.22,.158,.04,.075,.02,'paint');if(detail)cyl(x,.04,.16,.013,.008,'copper',12);}
for(let i=0;i<4;i++){let x=-.23+i*.15;box(x,-.18,.128,.10,.11,.022,'bakelite');if(detail)cyl(x,-.18,.145,.018,.015,'copper',12);}
for(const x of [-.32,.32])for(const y of [-.48,.48])cyl(x,y,.112,.014,.012,'steel',detail?12:8);
box(0,.37,.12,.19,.055,.013,'warning');
let obj='mtllib panel.mtl\no BreakerPanel2026\n';v.forEach(p=>obj+='v '+p.map(x=>x.toFixed(6)).join(' ')+'\n');uv.forEach(p=>obj+='vt '+p.map(x=>x.toFixed(6)).join(' ')+'\n');
let last='';for(const f of faces){if(last!==f.mat){obj+='usemtl '+f.mat+'\n';last=f.mat;}obj+='f '+f.ids.map(i=>i+'/'+i).join(' ')+'\n';}
for(const f of faces)for(const i of f.ids)if(i<1||i>v.length)throw Error('bad index');
return {obj,vertices:v.length,triangles:faces.length,uv:uv.length};
}
fs.mkdirSync(out,{recursive:true});let lod0=build(1),lod1=build(0);
fs.writeFileSync(path.join(out,'LOD0.obj'),lod0.obj);fs.writeFileSync(path.join(out,'LOD1.obj'),lod1.obj);
fs.writeFileSync(path.join(out,'manifest.json'),JSON.stringify({asset_id:'RZ_PROP_INTERIOR_STAIRWELL_BREAKER_PANEL_2026_001',version:'0.1.0',status:'geometry-wip',era:2026,units:'metres',lod0:{vertices:lod0.vertices,triangles:lod0.triangles},lod1:{vertices:lod1.vertices,triangles:lod1.triangles},pbr:'pbr.json',validation:'face index range passed; UE5 and render not tested'},null,2));
console.log(JSON.stringify({lod0:{vertices:lod0.vertices,triangles:lod0.triangles},lod1:{vertices:lod1.vertices,triangles:lod1.triangles}}));