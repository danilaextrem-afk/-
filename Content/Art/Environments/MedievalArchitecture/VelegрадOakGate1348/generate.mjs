// Original parametric Velegрад gate, meters, Y-up. node generate.mjs > gate_lod0.obj
const v=[],uv=[],faces=[];let mat='oak';
function box(x,y,z,w,h,d,m){let p=[[x,y,z],[x+w,y,z],[x+w,y+h,z],[x,y+h,z],[x,y,z+d],[x+w,y,z+d],[x+w,y+h,z+d],[x,y+h,z+d]];let f=[[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]];let start=v.length;v.push(...p);for(let a of f){let u=uv.length;uv.push([0,0],[1,0],[1,1],[0,1]);faces.push({m,verts:a.map((i,j)=>[start+i+1,u+j+1])})}}
for(let i=0;i<9;i++){let x=-1.35+i*.3;box(x,0,0,.286,3.2,.115,'oak');box(x+.035,.13,-.008,.01,2.9,.006,'grain')}
for(let y of [.24,1.5,2.82]){box(-1.42,y,-.075,2.84,.145,.065,'iron');for(let i=0;i<9;i++)box(-1.25+i*.3,y+.05,-.093,.05,.05,.025,'rivet')}
for(let x of [-1.5,1.42])box(x,-.08,-.12,.08,3.4,.26,'oak');
box(-.18,1.3,-.12,.36,.48,.08,'iron');box(-.05,1.47,-.145,.10,.15,.02,'void');
for(let y of [.35,2.6])box(-1.45,y,-.16,.35,.18,.08,'hinge');
let out=['# RZ_ENV_1348_VELEGRAD_OAK_GATE_001 v0.1','# Original procedural geometry; Y up; meters'];for(let p of v)out.push('v '+p.join(' '));for(let t of uv)out.push('vt '+t.join(' '));let last='';for(let f of faces){if(f.m!==last){out.push('usemtl '+f.m);last=f.m}out.push('f '+f.verts.map(q=>q[0]+'/'+q[1]).join(' '))}process.stdout.write(out.join('\n')+'\n');