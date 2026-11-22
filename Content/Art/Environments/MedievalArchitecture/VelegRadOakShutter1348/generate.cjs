// Self-contained procedural OBJ generator. Node.js >=18.
const fs=require('fs');
const build=function build(lod){
 const v=[],uv=[],faces=[], mats=[];let current="Oak";
 function vertex(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length}
 function tri(a,b,c){faces.push([a,b,c,current])}
 function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
 function box(x0,y0,z0,x1,y1,z1,mat){current=mat;let p=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];for(const q of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]]){let a=q.map(i=>vertex(...p[i],p[i][0],p[i][2]));quad(...a)}}
 // metres: X width, Y depth, Z height; three warped planks with real relief
 const segments=lod===0?20:6, widths=[[-.47,-.16],[-.15,.15],[.16,.47]];
 widths.forEach(([a,b],j)=>{current="Oak";let front=[],back=[];for(let k=0;k<=segments;k++){let z=.07+1.72*k/segments;for(let side=0;side<2;side++){let x=side?b:a;let warp=.009*Math.sin(k*.71+j*1.9+side)+.004*Math.sin(k*2.13+j);front.push(vertex(x,-.055+warp,z,(x+.5),z/1.9));back.push(vertex(x,.035+warp,z,(x+.5),z/1.9))}}
 for(let k=0;k<segments;k++){let i=k*2;quad(front[i],front[i+1],front[i+3],front[i+2]);quad(back[i+1],back[i],back[i+2],back[i+3]);quad(front[i],back[i],back[i+2],front[i+2]);quad(back[i+1],front[i+1],front[i+3],back[i+3])}
 quad(front[0],front[1],back[1],back[0]);let n=segments*2;quad(front[n+1],front[n],back[n],back[n+1]);
 });
 // cross battens and diagonal brace on rear, iron straps front
 for(const z of [.28,1.49])box(-.45,.036,z-.052,.45,.102,z+.052,"OakDark");
 // hinges and hammered straps
 for(const z of [.32,1.51]){box(-.49,-.083,z-.052,.31,-.065,z+.052,"Iron");for(let i=0;i<5;i++){let x=-.41+i*.17;const n=lod===0?10:6;current="Iron";let center=vertex(x,-.092,z,0.5,.5),ring=[];for(let k=0;k<n;k++){let t=k*2*Math.PI/n;ring.push(vertex(x+.014*Math.cos(t),-.093,z+.014*Math.sin(t),.5+.4*Math.cos(t),.5+.4*Math.sin(t)))}for(let k=0;k<n;k++)tri(center,ring[k],ring[(k+1)%n])}}
 // chamfer-like pin cylinders vertical along Z
 function cylinder(x,y,z,r,h,n,mat){current=mat;let rings=[];for(const zz of [z,z+h]){let a=[];for(let i=0;i<n;i++){let t=2*Math.PI*i/n;a.push(vertex(x+r*Math.cos(t),y+r*Math.sin(t),zz,i/n,zz/2))}rings.push(a)}for(let i=0;i<n;i++){let j=(i+1)%n;quad(rings[0][i],rings[0][j],rings[1][j],rings[1][i])}let c0=vertex(x,y,z,.5,.5),c1=vertex(x,y,z+h,.5,.5);for(let i=0;i<n;i++){let j=(i+1)%n;tri(c0,rings[0][j],rings[0][i]);tri(c1,rings[1][i],rings[1][j])}}
 for(const z of [.25,1.44])cylinder(-.505,-.052,z,.031,.17,lod===0?16:8,"Iron");
 // forged latch and ring pull
 box(.21,-.11,.82,.39,-.08,.87,"Iron");
 const n=lod===0?32:12;current="Iron";let rings=[];for(let i=0;i<n;i++){let t=i*2*Math.PI/n;let x=.29+.071*Math.cos(t),z=.72+.071*Math.sin(t);rings.push([vertex(x-.009,-.118,z-.009,i/n,0),vertex(x+.009,-.118,z+.009,i/n,1)])}for(let i=0;i<n;i++){let j=(i+1)%n;quad(rings[i][0],rings[i][1],rings[j][1],rings[j][0])}
 let out="# Original RAZLOM procedural shutter; units metres; Z up\nmtllib materials.mtl\n";
 for(const a of v)out+=`v ${a.map(q=>q.toFixed(6)).join(" ")}\n`;for(const a of uv)out+=`vt ${a.map(q=>q.toFixed(6)).join(" ")}\n`;
 let old="";for(const [a,b,c,m] of faces){if(m!==old){out+=`usemtl ${m}\n`;old=m}out+=`f ${[a,b,c].map(i=>i+"/"+i).join(" ")}\n`}
 let deg=0;for(const [a,b,c] of faces){let p=v[a-1],q=v[b-1],r=v[c-1],u=q.map((x,i)=>x-p[i]),w=r.map((x,i)=>x-p[i]);let cr=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]];if(Math.hypot(...cr)<1e-9)deg++}
 return {out,stats:{vertices:v.length,triangles:faces.length,uv:uv.length,degenerate:deg,materials:[...new Set(faces.map(x=>x[3]))]}}
};
for(const lod of [0,1]){const r=build(lod);fs.writeFileSync('LOD'+lod+'.obj',r.out);console.log(lod,r.stats)}
