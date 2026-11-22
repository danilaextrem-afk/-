'use strict';
// Original Watcher segmented waist harness, metres, Z-up. Procedural UV draft.
const fs=require('node:fs');
function build(){
const V=[],T=[],F=[],groups=[];
function vert(x,y,z,u,v){V.push([x,y,z]);T.push([u,v]);return V.length;}
function tri(a,b,c){F.push([a,b,c]);}
function quad(a,b,c,d){tri(a,b,c);tri(a,c,d);}
function ring(name,R,r,z0,z1,segments=48){
groups.push([name,F.length]);const bot=[],top=[];
for(let i=0;i<segments;i++){let a=2*Math.PI*i/segments,cs=Math.cos(a),sn=Math.sin(a);bot.push(vert(R*cs,R*sn,z0,i/segments,0));top.push(vert(R*cs,R*sn,z1,i/segments,1));}
for(let i=0;i<segments;i++){let j=(i+1)%segments;quad(bot[i],bot[j],top[j],top[i]);}
const bi=[],ti=[];for(let i=0;i<segments;i++){let a=2*Math.PI*i/segments;bi.push(vert((R-r)*Math.cos(a),(R-r)*Math.sin(a),z0,i/segments,0));ti.push(vert((R-r)*Math.cos(a),(R-r)*Math.sin(a),z1,i/segments,1));}
for(let i=0;i<segments;i++){let j=(i+1)%segments;quad(ti[i],ti[j],bi[j],bi[i]);quad(top[i],top[j],ti[j],ti[i]);quad(bi[i],bi[j],bot[j],bot[i]);}
}
function plate(name,angle,arc,top,bottom,outer,inner){
groups.push([name,F.length]);let n=8,outerTop=[],outerBottom=[],innerTop=[],innerBottom=[];
for(let i=0;i<=n;i++){let a=angle+arc*(i/n-.5),ct=Math.cos(a),st=Math.sin(a),t=i/n;let scallop=.012*Math.sin(Math.PI*t);outerTop.push(vert((outer+scallop)*ct,(outer+scallop)*st,top,t,0));outerBottom.push(vert((outer+.018*Math.sin(Math.PI*t))*ct,(outer+.018*Math.sin(Math.PI*t))*st,bottom-.02*Math.sin(Math.PI*t),t,1));innerTop.push(vert(inner*ct,inner*st,top,t,0));innerBottom.push(vert(inner*ct,inner*st,bottom-.02*Math.sin(Math.PI*t),t,1));}
for(let i=0;i<n;i++){quad(outerTop[i],outerTop[i+1],outerBottom[i+1],outerBottom[i]);quad(innerBottom[i],innerBottom[i+1],innerTop[i+1],innerTop[i]);quad(outerTop[i],innerTop[i],innerTop[i+1],outerTop[i+1]);quad(outerBottom[i+1],innerBottom[i+1],innerBottom[i],outerBottom[i]);}
quad(outerTop[0],outerBottom[0],innerBottom[0],innerTop[0]);quad(innerTop[n],innerBottom[n],outerBottom[n],outerTop[n]);
}
function stud(name,x,y,z,rad=.013){groups.push([name,F.length]);let base=[],tip=vert(x*1.07,y*1.07,z,0.5,0.5);for(let i=0;i<12;i++){let a=2*Math.PI*i/12;base.push(vert(x+rad*Math.cos(a),y+rad*Math.sin(a),z+rad*.2,Math.cos(a)*.5+.5,Math.sin(a)*.5+.5));}for(let i=0;i<12;i++)tri(base[i],base[(i+1)%12],tip);}
ring('forged_belt_upper',.245,.026,.04,.103);ring('forged_belt_lower',.256,.025,-.04,-.007);
for(let i=0;i<12;i++){let a=i*2*Math.PI/12;plate('hip_lamella_'+String(i+1).padStart(2,'0'),a,.42,-.012,-.29,.278,.254);stud('upper_rivet_'+i,.254*Math.cos(a),.254*Math.sin(a),.073);stud('lower_rivet_'+i,.265*Math.cos(a),.265*Math.sin(a),-.027);}
plate('front_crest',-Math.PI/2,.65,.115,-.13,.295,.263);
return {V,T,F,groups};
}
function obj(m){let s=['# RAZLOM original Watcher articulated waist harness v1; metres; Z up','# WIP mesh, UV draft, not skinned'];for(const v of m.V)s.push('v '+v.map(x=>x.toFixed(6)).join(' '));for(const t of m.T)s.push('vt '+t.map(x=>x.toFixed(6)).join(' '));let gi=0;for(let i=0;i<m.F.length;i++){if(gi<m.groups.length&&m.groups[gi][1]===i){s.push('g '+m.groups[gi][0]);gi++;}s.push('f '+m.F[i].map(x=>x+'/'+x).join(' '));}return s.join('\n')+'\n';}
if(require.main===module){const out=process.argv[2]||'SM_Watcher_WaistHarness_v1.obj';fs.writeFileSync(out,obj(build()));console.log(out);}
module.exports={build,obj};
