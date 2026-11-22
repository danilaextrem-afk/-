// Original parametric entrance facade, meters; Z up. Run: node generate.cjs
const fs=require('fs'),path=require('path');const dir=__dirname;
function build(low){let v=[],uv=[],f=[];let mat='';
function vertex(p,t){v.push(p);uv.push(t);return v.length}
function face(a,b,c){f.push([a,b,c,mat])}
function box(x,y,z,w,d,h,m){mat=m;let p=[[x,y,z],[x+w,y,z],[x+w,y+d,z],[x,y+d,z],[x,y,z+h],[x+w,y,z+h],[x+w,y+d,z+h],[x,y+d,z+h]];
for(let ids of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]]){let a=ids.map((id,i)=>vertex(p[id],[[0,0],[1,0],[1,1],[0,1]][i]));face(a[0],a[1],a[2]);face(a[0],a[2],a[3])}}
box(-2.2,0,0,4.4,.28,3.5,'concrete');box(-1.25,-.20,0,2.5,.22,2.6,'dark_metal');
for(let x of [-1.11,.03]){box(x,-.245,.06,1.08,.045,2.38,'paint');box(x+.09,-.30,.87,.91,.015,1.24,'glass')}
box(-1.8,-.88,2.75,3.6,.92,.17,'concrete');box(-1.8,-.91,2.68,3.6,.05,.07,'dark_metal');
box(-1.8,-.93,2.63,3.6,.035,.025,'lamp');
for(let x of [-1.72,1.57])box(x,-.73,0,.15,.15,2.75,'dark_metal');
for(let [x,y,z,w,d] of [[-1.65,-1.25,-.15,3.3,1.25],[-1.52,-1.5,-.30,3.04,.26],[-1.38,-1.75,-.45,2.76,.25]])box(x,y,z,w,d,.15,'concrete');
box(1.35,-.36,1.12,.22,.13,.37,'dark_metal');box(1.39,-.40,1.2,.14,.015,.12,'glass');
for(let x of [.95,-.14])box(x,-.38,1.08,.07,.07,.28,'steel');
for(let i=0;i<(low?3:9);i++)for(let x of [-2.21,2.19])box(x,-.035,.35+i*.29,.012,.04,.012,'seam');
if(!low)for(let i=0;i<8;i++)box(-1.68+i*.48,-.945,2.69,.17,.025,.018,'rust');
let s='mtllib entrance.mtl\no SM_MoscowPanelEntrance2026_LOD'+(low?1:0)+'\n';
for(let p of v)s+='v '+p.join(' ')+'\n';for(let p of uv)s+='vt '+p.join(' ')+'\n';let last='';
for(let [a,b,c,m] of f){if(m!==last){s+='usemtl '+m+'\n';last=m}s+='f '+[a,b,c].map(i=>i+'/'+i).join(' ')+'\n'}
fs.writeFileSync(path.join(dir,'entrance_LOD'+(low?1:0)+'.obj'),s);
return {vertices:v.length,triangles:f.length,uv:uv.length,invalid:f.filter(t=>new Set(t.slice(0,3)).size!==3||t.slice(0,3).some(i=>i<1||i>v.length)).length}}
console.log(JSON.stringify([build(false),build(true)]));