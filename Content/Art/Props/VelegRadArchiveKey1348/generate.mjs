// Reproducible generator: Node.js; run node generate.mjs
function generate(detail=1){
const V=[],T=[],F=[];let count=0;
function tri(a,b,c){F.push(`f ${a}/${a} ${b}/${b} ${c}/${c}`)}
function vert(x,y,z,u,v){V.push(`v ${x.toFixed(5)} ${y.toFixed(5)} ${z.toFixed(5)}`);T.push(`vt ${u.toFixed(5)} ${v.toFixed(5)}`);return ++count}
function tube(cx,cy,cz,rx,ry,radius,segments,sides){let rings=[];for(let i=0;i<=segments;i++){let a=i/segments*Math.PI*2,ring=[];for(let j=0;j<=sides;j++){let b=j/sides*Math.PI*2;ring.push(vert(cx+(rx+radius*Math.cos(b))*Math.cos(a),cy+(ry+radius*Math.cos(b))*Math.sin(a),cz+radius*Math.sin(b),i/segments,j/sides))}rings.push(ring)}for(let i=0;i<segments;i++)for(let j=0;j<sides;j++){let a=rings[i][j],b=rings[i+1][j],c=rings[i+1][j+1],d=rings[i][j+1];tri(a,b,c);tri(a,c,d)}}
function box(x0,y0,z0,x1,y1,z1){let pts=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];for(let face of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]]){let ids=face.map((k,j)=>vert(...pts[k],j===1||j===2?1:0,j>=2?1:0));tri(ids[0],ids[1],ids[2]);tri(ids[0],ids[2],ids[3])}}
tube(0,0,0,0.23,0.23,0.058,detail?48:16,detail?12:6);
box(-.075,-.85,-.065,.075,-.23,.065);box(-.085,-1.08,-.065,.085,-.84,.065);
box(-.085,-1.08,-.065,.34,-.98,.065);box(-.085,-.91,-.065,.23,-.83,.065);
if(detail){for(let i=0;i<8;i++){let a=i*Math.PI/4;let x=.31*Math.cos(a),y=.31*Math.sin(a);box(x-.025,y-.025,-.075,x+.025,y+.025,.075)}}
return {obj:["# Original Velеgrad archive key, meters; Z up","o VelegRadArchiveKey1348","mtllib materials.mtl",...V,...T,"usemtl aged_iron",...F,""].join("\n"),vertices:count,triangles:F.length}
}
import {writeFileSync} from 'node:fs';
writeFileSync('LOD0.obj',generate(1).obj);writeFileSync('LOD1.obj',generate(0).obj);
