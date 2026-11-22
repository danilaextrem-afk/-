// Reproducible original mesh generator; run node generate.js in asset directory.
const fs=require('fs');
function generate(segments,rings){
 const parts=[["torso",0,0,1.72,.35,.19,.68],["pelvis",0,0,1.13,.26,.18,.26],["head",0,-.035,2.43,.21,.18,.32],["left_arm",-.42,0,1.65,.13,.13,.58],["right_arm",.42,0,1.65,.13,.13,.58],["left_leg",-.17,0,.61,.15,.15,.63],["right_leg",.17,0,.61,.15,.15,.63],["left_claw",-.47,-.03,.94,.12,.10,.31],["right_claw",.47,-.03,.94,.12,.10,.31]];
 let vs=[],uv=[],fs=[]; for(let k=0;k<parts.length;k++){let [name,cx,cy,cz,sx,sy,sz]=parts[k],offset=vs.length;
 for(let j=0;j<=rings;j++){let theta=Math.PI*j/rings;for(let i=0;i<=segments;i++){let phi=2*Math.PI*i/segments;let x=Math.sin(theta)*Math.cos(phi),y=Math.sin(theta)*Math.sin(phi),z=Math.cos(theta);let wave=1+.055*Math.sin(13*z+1.7*k)+.035*Math.cos(19*x+7*y);vs.push([cx+sx*x*wave,cy+sy*y*wave,cz+sz*z]);uv.push([i/segments,j/rings]);}}
 for(let j=0;j<rings;j++)for(let i=0;i<segments;i++){let a=offset+j*(segments+1)+i,b=a+segments+1; if(j>0)fs.push([a,b,a+1]);if(j<rings-1)fs.push([a+1,b,b+1]);}
 }
 let s="# Original RAZLOM occult shadow; geometry prototype; Z-up meters\nmtllib Shadow.mtl\no ShadowApparition\n";for(let v of vs)s+="v "+v.map(x=>x.toFixed(6)).join(" ")+"\n";for(let t of uv)s+="vt "+t.map(x=>x.toFixed(6)).join(" ")+"\n";s+="usemtl ShadowSkin\n";for(let f of fs)s+="f "+f.map(i=>(i+1)+"/"+(i+1)).join(" ")+"\n";
 return {obj:s,vertices:vs.length,triangles:fs.length,uv:uv.length,index_valid:fs.every(f=>f.every(i=>i>=0&&i<vs.length)),bounds:[vs.reduce((a,v)=>a.map((x,i)=>Math.min(x,v[i])),[Infinity,Infinity,Infinity]),vs.reduce((a,v)=>a.map((x,i)=>Math.max(x,v[i])),[-Infinity,-Infinity,-Infinity])]};
}
for(const [name,seg,rings] of [['Shadow_LOD0.obj',16,10],['Shadow_LOD1.obj',8,5]])fs.writeFileSync(name,generate(seg,rings).obj);
