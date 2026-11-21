// Reproducible geometry generator for B3 mask. Run: node generate.mjs
const fs=require('node:fs');
const build=function build(){
const v=[],uv=[],faces=[];const mats=["bone","iron","gold","void"];
function quad(a,b,c,d,m){faces.push([a,b,c,m],[a,c,d,m])}
function ring(cx,cy,cz,rx,ry,depth,segments,m,seed=0){
let start=v.length;for(let layer=0;layer<2;layer++)for(let i=0;i<segments;i++){let t=i*2*Math.PI/segments,noise=1+0.035*Math.sin(i*3.17+seed)+0.02*Math.cos(i*7.9);v.push([cx+rx*Math.cos(t)*noise,cy+ry*Math.sin(t)*noise,cz+layer*depth]);uv.push([i/segments,layer]);}
for(let i=0;i<segments;i++){let j=(i+1)%segments;quad(start+i,start+j,start+segments+j,start+segments+i,m);}
}
function cone(cx,cy,cz,r,h,segments,m,tilt=0){let s=v.length;for(let layer=0;layer<2;layer++)for(let i=0;i<segments;i++){let t=2*Math.PI*i/segments,rr=layer?0.025:r;v.push([cx+tilt*layer+rr*Math.cos(t),cy+rr*Math.sin(t),cz+layer*h]);uv.push([i/segments,layer]);}for(let i=0;i<segments;i++)quad(s+i,s+(i+1)%segments,s+segments+(i+1)%segments,s+segments+i,m)}
ring(0,0,0,0.38,0.52,0.095,96,0,2);
ring(0,0,0.1,0.39,0.54,0.035,96,1,5);
for(let i=0;i<13;i++){let a=i*2*Math.PI/13;cone(0.37*Math.cos(a),0.5*Math.sin(a),0.1,0.042,0.15+(i%3)*0.025,12,2,0.03*Math.sin(a))}
for(let x of [-0.16,0.16]){ring(x,0.11,0.14,0.105,0.085,0.035,48,3,4);ring(x,0.11,0.17,0.125,0.105,0.02,48,1,2)}
ring(0,-0.15,0.15,0.11,0.18,0.08,64,0,1);
for(let i=0;i<9;i++)cone(-0.23+i*0.058,-0.37,0.13,0.024,0.10,12,0,0);
let obj="# Original B3 Graveless Prince ceremonial death mask; Z front, meters\nmtllib B3DeathMask.mtl\n";v.forEach(p=>obj+=`v ${p.map(x=>x.toFixed(6)).join(" ")}\n`);uv.forEach(p=>obj+=`vt ${p.map(x=>x.toFixed(6)).join(" ")}\n`);let last=-1;for(const [a,b,c,m] of faces){if(last!==m){obj+=`usemtl ${mats[m]}\n`;last=m}obj+=`f ${a+1}/${a+1} ${b+1}/${b+1} ${c+1}/${c+1}\n`;}
return {obj,vertices:v.length,triangles:faces.length,valid:faces.every(f=>new Set(f.slice(0,3)).size===3&&f.slice(0,3).every(i=>i>=0&&i<v.length))};
};
const result=build();
if(!result.valid)throw Error('invalid geometry indices');
fs.writeFileSync('B3DeathMask.obj',result.obj);
console.log(JSON.stringify({vertices:result.vertices,triangles:result.triangles,valid:result.valid}));
