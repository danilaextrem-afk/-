// Original procedural asset generator. Node.js 18+; run: node generate.cjs
const fs=require('node:fs');
function build(lod=0){
 const V=[],T=[],F=[],groups=[];let mat="";const n=lod?8:16;
 function vert(x,y,z,u,v){V.push([x,y,z]);T.push([u,v]);return V.length}
 function face(a,b,c){F.push([a,b,c,mat])}
 function material(s){mat=s}
 function ring(y,rx,rz,segments=n,cx=0,cz=0,twist=0){
  return Array.from({length:segments},(_,i)=>{let a=2*Math.PI*i/segments+twist;return vert(cx+rx*Math.cos(a),y,cz+rz*Math.sin(a),i/segments,(y+0.6)/1.5)})
 }
 function bridge(a,b){for(let i=0;i<a.length;i++){let j=(i+1)%a.length;face(a[i],a[j],b[j]);face(a[i],b[j],b[i])}}
 function capped(y0,y1,rx,rz,m,segments=n){material(m);let a=ring(y0,rx,rz,segments),b=ring(y1,rx,rz,segments);bridge(a,b);let c=vert(0,y0,0,.5,0),d=vert(0,y1,0,.5,1);for(let i=0;i<segments;i++){let j=(i+1)%segments;face(c,a[j],a[i]);face(d,b[i],b[j])}}
 capped(-.49,-.17,.034,.034,"dark_wood");
 for(let k=0;k<(lod?3:7);k++)capped(-.46+k*.043,-.448+k*.043,.036,.037,"bronze");
 capped(-.56,-.49,.067,.052,"bronze");
 capped(-.16,-.125,.14,.038,"iron");
 // Guard curved upward swept arms as closed tapered rectangular segments
 function bar(p,q,w,h,m){material(m);let ax=p[0],ay=p[1],bx=q[0],by=q[1];let dx=bx-ax,dy=by-ay,L=Math.hypot(dx,dy),nx=-dy/L*w/2,ny=dx/L*w/2;let a=[vert(ax+nx,ay+ny,-h/2,0,0),vert(ax-nx,ay-ny,-h/2,1,0),vert(ax-nx,ay-ny,h/2,1,1),vert(ax+nx,ay+ny,h/2,0,1)],b=[vert(bx+nx,by+ny,-h/2,0,0),vert(bx-nx,by-ny,-h/2,1,0),vert(bx-nx,by-ny,h/2,1,1),vert(bx+nx,by+ny,h/2,0,1)];bridge(a,b);face(a[0],a[2],a[1]);face(a[0],a[3],a[2]);face(b[0],b[1],b[2]);face(b[0],b[2],b[3])}
 for(let s of [-1,1]){bar([s*.045,-.14],[s*.13,-.12],.033,.05,"iron");bar([s*.13,-.12],[s*.18,-.07],.027,.043,"iron")}
 // tapered leaf blade with raised central ridge, sections in y, triangulated closed perimeter
 material("steel");
 let sections=lod?[[ -.125,.049,.013],[.04,.063,.014],[.25,.047,.012],[.49,.018,.007],[.62,.001,.001]]:[[-.125,.048,.014],[-.06,.053,.015],[.04,.066,.017],[.16,.057,.015],[.28,.048,.013],[.40,.034,.011],[.52,.017,.007],[.64,.001,.001]];
 let rings=sections.map(([y,w,d])=>[vert(-w,y,0,0,y),vert(0,y,d,.5,y),vert(w,y,0,1,y),vert(0,y,-d,.5,y)]);
 for(let i=0;i<rings.length-1;i++)bridge(rings[i],rings[i+1]);
 face(rings[0][0],rings[0][1],rings[0][2]);face(rings[0][0],rings[0][2],rings[0][3]);
 let last=rings.at(-1);face(last[0],last[2],last[1]);face(last[0],last[3],last[2]);
 // etched dark fuller inset along front
 if(!lod){material("patina");bar([0,-.07],[0,.36],.006,.002,"patina")}
 const lines=["# Original procedural mesh; meters, Y up","# Asset RZ_EQ_VELEGRAD_RITUAL_BLADE_1348_001",`mtllib blade.mtl`];
 for(const v of V)lines.push("v "+v.map(x=>x.toFixed(6)).join(" "));
 for(const t of T)lines.push("vt "+t.map(x=>x.toFixed(6)).join(" "));
 let old="";for(const [a,b,c,m] of F){if(m!==old){lines.push("usemtl "+m);old=m}lines.push(`f ${a}/${a} ${b}/${b} ${c}/${c}`)}
 return {obj:lines.join("\n")+"\n",vertices:V.length,triangles:F.length,uv:T.length,degenerate:F.filter(([a,b,c])=>a===b||b===c||a===c).length};
}
for(const lod of [0,1]) fs.writeFileSync('blade_lod'+lod+'.obj',build(lod).obj);
