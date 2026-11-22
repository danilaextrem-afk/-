'use strict';
// Original Watcher articulated knee guard v1. Z-up metres, left/right pieces.
const fs=require('node:fs');
function build(){
const V=[],T=[],F=[],groups=[];
function vert(x,y,z,u,v){V.push([x,y,z]);T.push([u,v]);return V.length}
function tri(a,b,c){F.push([a,b,c])}
function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
function patch(name,cx,cz,r0,r1,a0,a1,rows,cols,depth,front=true){
 const start=F.length,ids=[];
 for(let j=0;j<=rows;j++){const row=[],t=j/rows,r=r0+(r1-r0)*t;
 for(let i=0;i<=cols;i++){const s=i/cols,a=a0+(a1-a0)*s;
 const x=cx+r*Math.sin(a),y=depth+0.022*Math.cos(a)+0.008*Math.sin(4*a)*Math.sin(Math.PI*t),z=cz+r*Math.cos(a);
 row.push(vert(x,y,z,s,t));}ids.push(row)}
 for(let j=0;j<rows;j++)for(let i=0;i<cols;i++)quad(ids[j][i],ids[j][i+1],ids[j+1][i+1],ids[j+1][i]);
 groups.push([name,start,F.length]);
}
function torus(name,cx,cy,cz,R,r,segments=28,tube=10,plane='xz'){
 const start=F.length,ids=[];
 for(let j=0;j<=tube;j++){const row=[],v=j/tube*2*Math.PI;
 for(let i=0;i<=segments;i++){const u=i/segments*2*Math.PI,rr=R+r*Math.cos(v),z=r*Math.sin(v);
 let x=cx+rr*Math.cos(u),y=cy+z,Z=cz+rr*Math.sin(u);
 if(plane==='xy'){x=cx+rr*Math.cos(u);y=cy+rr*Math.sin(u);Z=cz+z;}
 row.push(vert(x,y,Z,i/segments,j/tube))}ids.push(row)}
 for(let j=0;j<tube;j++)for(let i=0;i<segments;i++)quad(ids[j][i],ids[j][i+1],ids[j+1][i+1],ids[j+1][i]);
 groups.push([name,start,F.length]);
}
function cylinder(name,cx,cy,cz,R,h,n=20){
 const start=F.length,bot=[],top=[];
 for(let i=0;i<n;i++){let a=i*2*Math.PI/n;bot.push(vert(cx+R*Math.cos(a),cy,cz+R*Math.sin(a),i/n,0));top.push(vert(cx+R*Math.cos(a),cy+h,cz+R*Math.sin(a),i/n,1))}
 for(let i=0;i<n;i++)quad(bot[i],bot[(i+1)%n],top[(i+1)%n],top[i]);
 const b=vert(cx,cy,cz,.5,.5),t=vert(cx,cy+h,cz,.5,.5);
 for(let i=0;i<n;i++){tri(b,bot[(i+1)%n],bot[i]);tri(t,top[i],top[(i+1)%n])}
 groups.push([name,start,F.length]);
}
for(const [side,cx] of [['L',-.18],['R',.18]]){
 // Layered curved kneecap with distinct flexible lower and upper lames.
 patch(side+'_main_knee_shell',cx,.61,.035,.145,-1.20,1.20,9,32,-.126);
 patch(side+'_upper_articulated_lame',cx,.745,.015,.075,-1.16,1.16,5,26,-.115);
 patch(side+'_lower_articulated_lame',cx,.475,.012,.083,-1.16,1.16,5,26,-.118);
 torus(side+'_central_raised_rim',cx,-.137,.61,.052,.008,32,10);
 cylinder(side+'_central_boss',cx,-.165,.61,.029,.035,24);
 for(const sign of [-1,1]){
   cylinder(side+'_hinge_rivet_'+sign,cx+sign*.108,-.146,.61,.012,.018,16);
   torus(side+'_hinge_collar_'+sign,cx+sign*.108,-.139,.61,.018,.004,18,8);
 }
 for(let k=0;k<3;k++){let z=.55+k*.06;torus(side+'_engraved_bead_'+k,cx,-.15,z,.009,.0025,16,7)}
}
const o=['# Original RAZLOM Watcher articulated knee guards, Z-up metres, WIP','mtllib SM_Watcher_KneeGuards_v1.mtl',...V.map(p=>'v '+p.map(n=>n.toFixed(6)).join(' ')),...T.map(p=>'vt '+p.map(n=>n.toFixed(6)).join(' '))];
let g=0;for(let i=0;i<F.length;i++){if(g<groups.length&&groups[g][1]===i)o.push('g '+groups[g++][0]);o.push('f '+F[i].map(v=>v+'/'+v).join(' '))}
return {obj:o.join('\n')+'\n',vertices:V,faces:F,groups};
}
if(require.main===module){fs.writeFileSync(process.argv[2]||'SM_Watcher_KneeGuards_v1.obj',build().obj)}
module.exports={build};
