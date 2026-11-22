function generate(segments=24){
const V=[],T=[],F=[],groups={};let mat="";const add=(p,uv)=>{V.push(p);T.push(uv);return V.length};
const tri=(a,b,c)=>{(groups[mat]??=[]).push([a,b,c])};
function quad(a,b,c,d){const i=[add(a,[0,0]),add(b,[1,0]),add(c,[1,1]),add(d,[0,1])];tri(i[0],i[1],i[2]);tri(i[0],i[2],i[3])}
function box(x,y,z,w,d,h,m){mat=m;let x0=x-w/2,x1=x+w/2,y0=y-d/2,y1=y+d/2,z0=z-h/2,z1=z+h/2;quad([x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]);quad([x1,y0,z0],[x0,y0,z0],[x0,y1,z0],[x1,y1,z0]);quad([x0,y0,z0],[x1,y0,z0],[x1,y0,z1],[x0,y0,z1]);quad([x1,y1,z0],[x0,y1,z0],[x0,y1,z1],[x1,y1,z1]);quad([x1,y0,z0],[x1,y1,z0],[x1,y1,z1],[x1,y0,z1]);quad([x0,y1,z0],[x0,y0,z0],[x0,y0,z1],[x0,y1,z1])}
function cylinder(x,y,z,r,h,m,n=segments){mat=m;let lo=[],hi=[];for(let j=0;j<n;j++){let a=2*Math.PI*j/n;lo.push(add([x+r*Math.cos(a),y+r*Math.sin(a),z],[j/n,0]));hi.push(add([x+r*Math.cos(a),y+r*Math.sin(a),z+h],[j/n,1]))}let ct=add([x,y,z+h],[.5,.5]),cb=add([x,y,z],[.5,.5]);for(let j=0;j<n;j++){let k=(j+1)%n;tri(lo[j],lo[k],hi[k]);tri(lo[j],hi[k],hi[j]);tri(ct,hi[j],hi[k]);tri(cb,lo[k],lo[j])}}
box(0,0,.055,.24,.145,.11,"charcoal");box(0,-.073,.058,.232,.009,.096,"anodized");box(0,.074,.058,.232,.009,.096,"charcoal");box(0,0,.113,.218,.125,.006,"anodized");
box(-.034,-.007,.119,.128,.081,.006,"smoked_glass");box(-.034,-.007,.124,.11,.064,.003,"cassette");
for(const x of [-.067,.002]){cylinder(x,-.006,.126,.023,.002,"spool",segments);cylinder(x,-.006,.128,.007,.002,"charcoal",segments)}
for(let i=0;i<6;i++)box(.064,-.039+i*.013,.119,.049,.004,.003,"grille");
for(let i=0;i<4;i++)box(-.072+i*.039,.055,.12,.027,.017,.011,i===2?"red":"button");
cylinder(.082,.039,.116,.017,.014,"spool");cylinder(.082,.039,.13,.011,.003,"charcoal");
box(.118,-.023,.068,.004,.023,.025,"anodized");box(-.119,.014,.069,.004,.026,.021,"anodized");
box(0,0,-.002,.18,.085,.005,"rubber");
let obj="# Original procedural 3D mesh. Z up, metres. UV per vertex.\n",faces=0;for(const v of V)obj+="v "+v.map(x=>x.toFixed(6)).join(" ")+"\n";for(const t of T)obj+="vt "+t.map(x=>x.toFixed(6)).join(" ")+"\n";obj+="mtllib recorder.mtl\n";for(const [m,fs] of Object.entries(groups)){obj+="usemtl "+m+"\n";for(const f of fs){obj+="f "+f.map(i=>`${i}/${i}`).join(" ")+"\n";faces++}}return {obj,vertices:V.length,triangles:faces,uv:T.length,valid:Object.values(groups).flat().every(f=>new Set(f).size===3&&f.every(i=>i>0&&i<=V.length))};
}
const fs=require('node:fs');const path=require('node:path');for(const [n,s] of [['recorder_LOD0.obj',24],['recorder_LOD1.obj',12]])fs.writeFileSync(path.join(__dirname,n),generate(s).obj);console.log(JSON.stringify({lod0:generate(24).triangles,lod1:generate(12).triangles}));
