// Original parametric investigation recorder, meters; Node 18+; no dependencies.
const fs=require('fs'); const path=require('path');
function build(lod=0){
let v=[],uv=[],f=[],groups=[];let material='case';
function vert(p,t){v.push(p);uv.push(t);return v.length}
function quad(a,b,c,d,m){material=m;let n=v.length;[a,b,c,d].forEach((p,i)=>vert(p,[[0,0],[1,0],[1,1],[0,1]][i]));f.push([n+1,n+2,n+3,m],[n+1,n+3,n+4,m])}
function box(x,y,z,w,h,d,m){let x0=x-w/2,x1=x+w/2,y0=y-h/2,y1=y+h/2,z0=z-d/2,z1=z+d/2;
quad([x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1],m);
quad([x1,y0,z0],[x0,y0,z0],[x0,y1,z0],[x1,y1,z0],m);
quad([x0,y0,z0],[x0,y0,z1],[x0,y1,z1],[x0,y1,z0],m);
quad([x1,y0,z1],[x1,y0,z0],[x1,y1,z0],[x1,y1,z1],m);
quad([x0,y1,z1],[x1,y1,z1],[x1,y1,z0],[x0,y1,z0],m);
quad([x0,y0,z0],[x1,y0,z0],[x1,y0,z1],[x0,y0,z1],m)}
function cylinder(cx,cy,cz,r,depth,m,n){for(let i=0;i<n;i++){let a=i*2*Math.PI/n,b=(i+1)*2*Math.PI/n;let A=[cx+r*Math.cos(a),cy+r*Math.sin(a),cz+depth/2],B=[cx+r*Math.cos(b),cy+r*Math.sin(b),cz+depth/2],C=[cx+r*Math.cos(b),cy+r*Math.sin(b),cz-depth/2],D=[cx+r*Math.cos(a),cy+r*Math.sin(a),cz-depth/2];quad(A,B,C,D,m);let center=[cx,cy,cz+depth/2];let p=v.length;[center,A,B].forEach((q,j)=>vert(q,[[.5,.5],[0,0],[1,0]][j]));f.push([p+1,p+2,p+3,m])}}
box(0,0,0,.13,.072,.025,'case');
box(0,0,.014,.119,.060,.004,'bezel');
box(0,.015,.017,.066,.021,.002,'display');
box(0,-.011,.017,.062,.021,.002,'speaker');
for(let i=0;i<8;i++)box(-.025+i*.007,-.011,.018,.002,.015,.001,'slit');
for(let i=0;i<3;i++)cylinder(-.044+i*.044,-.028,.019,.005,.004,'button',lod?8:16);
box(.052,.013,.018,.009,.020,.003,'button');
box(-.051,.015,.018,.006,.018,.003,'button');
if(!lod){for(let i=0;i<5;i++)box(-.038+i*.019,.032,.003,.009,.003,.006,'seam');box(.064,0,0,.004,.016,.009,'metal');box(0,-.036,0,.06,.003,.008,'metal')}
let lines=['# Generated original recorder LOD'+lod,'mtllib Recorder.mtl'];let last='';for(let i=0;i<v.length;i++)lines.push('v '+v[i].map(x=>x.toFixed(6)).join(' '));for(let i=0;i<uv.length;i++)lines.push('vt '+uv[i].map(x=>x.toFixed(6)).join(' '));for(let q of f){if(q[3]!==last){lines.push('usemtl '+q[3]);last=q[3]}lines.push('f '+q.slice(0,3).map(j=>j+'/'+j).join(' '))}return {obj:lines.join('\n')+'\n',vertices:v.length,triangles:f.length,uv:uv.length,faces:f}}
const materials={case:[.045,.049,.053,.12,.68],bezel:[.12,.13,.14,.55,.42],display:[.035,.14,.17,.0,.22],speaker:[.09,.09,.10,.1,.85],slit:[.012,.012,.013,0,.9],button:[.20,.22,.23,.3,.48],seam:[.018,.02,.022,0,.8],metal:[.36,.38,.4,.85,.33]};
function mtl(){return Object.entries(materials).map(([k,a])=>'newmtl '+k+'\nKd '+a.slice(0,3).join(' ')+'\nKs '+[a[3],a[3],a[3]].join(' ')+'\nNs '+(2+100*(1-a[4])).toFixed(2)+'\n# metallic '+a[3]+' roughness '+a[4]+' (preview only)').join('\n\n')+'\n'}
if(require.main===module){for(let i=0;i<2;i++)fs.writeFileSync(path.join(__dirname,'Recorder_LOD'+i+'.obj'),build(i).obj);fs.writeFileSync(path.join(__dirname,'Recorder.mtl'),mtl())}
module.exports={build,mtl,materials};