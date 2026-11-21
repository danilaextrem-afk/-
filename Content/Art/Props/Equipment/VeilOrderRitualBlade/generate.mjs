// Original procedural mesh source, Razlom Veil Order ritual blade v0.1
// Run: node generate.mjs  (writes OBJ, MTL and four P3 PPM PBR maps)
import fs from "node:fs";
const V=[],T=[],F=[]; let groups={}; let current="iron";
function v(x,y,z,u,w){V.push([x,y,z]);T.push([u,w]);return V.length}
function face(a,b,c){F.push([current,a,b,c])}
function bridge(a,b){for(let i=0;i<a.length;i++){let j=(i+1)%a.length;face(a[i],b[i],b[j]);face(a[i],b[j],a[j])}}
function ring(y,rx,rz,n=24,phase=0){return Array.from({length:n},(_,i)=>{let t=2*Math.PI*i/n+phase;return v(rx*Math.cos(t),y,rz*Math.sin(t),i/n,(y+100)/240)})}
function lathe(profile,mat,n=24){current=mat;let prev=null;for(const [y,rx,rz] of profile){let r=ring(y,rx,rz,n);if(prev)bridge(prev,r);prev=r}}
function blade(){current="steel";let prev=null;const N=9;for(let k=0;k<=48;k++){let y=18+k*2.45, q=k/48, half=10.5*(1-Math.pow(q,2.6))+0.12;let thick=1.8*(1-q)+0.04;let a=[];for(let j=0;j<N;j++){let t=2*Math.PI*j/N;let x=half*Math.cos(t), z=thick*Math.sin(t)*(0.68+0.32*Math.abs(Math.cos(t)));a.push(v(x,y,z,j/(N-1),q))}if(prev)bridge(prev,a);prev=a}}
blade();
lathe([[13,12,1.8],[15,16,2.4],[17,15.5,2.2],[19,11,1.6]],"blackiron",32);
lathe([[-24,3.5,3.5],[-22,4.3,4.3],[-20,4.2,4.2],[9,3.9,3.9],[12,4.7,4.7],[14,3.6,3.6]],"leather",24);
lathe([[-31,2.0,2],[-29,5.5,5.5],[-27,6.5,6.5],[-25,5.7,5.7],[-23,3.8,3.8]],"bronze",32);
for(let s of [-1,1]){current="blackiron";lathe([[15,s*1.0,1],[16,s*1.1,1]],"blackiron",8)}
// forged quillons along X, with riveted terminal bosses
function tubeX(sign){current="blackiron";let old=null;for(let k=0;k<=12;k++){let x=sign*(10+k*1.5),radius=2.4*(1-k/15)+0.4;let a=[];for(let j=0;j<12;j++){let t=2*Math.PI*j/12;a.push(v(x,16+radius*Math.cos(t),radius*Math.sin(t),j/12,k/12))}if(old)bridge(old,a);old=a}}
tubeX(-1);tubeX(1);
// rivet bosses on crossguard
for(let x of [-10,-5,5,10]){current="bronze";let a=[],b=[];for(let j=0;j<12;j++){let t=2*Math.PI*j/12;a.push(v(x+1.1*Math.cos(t),16+1.1*Math.sin(t),2.0,j/12,0));b.push(v(x+0.65*Math.cos(t),16+0.65*Math.sin(t),2.8,j/12,1))}bridge(a,b)}
const lines=["# Original Razlom mesh; centimeters; Y-long; material slots; UV per vertex","mtllib SM_VeilOrderRitualBlade_v01.mtl","o SM_VeilOrderRitualBlade_v01",...V.map(a=>"v "+a.slice(0,3).map(x=>x.toFixed(5)).join(" ")),...T.map(a=>"vt "+a.map(x=>x.toFixed(5)).join(" "))];
let last="";for(const [mat,a,b,c] of F){if(mat!==last){lines.push("usemtl "+mat);last=mat}lines.push("f "+[a,b,c].map(i=>i+"/"+i).join(" "))}fs.writeFileSync("SM_VeilOrderRitualBlade_v01.obj",lines.join("\n")+"\n");
const mats={steel:[166,173,178,0.77,0.31],blackiron:[52,56,60,0.83,0.68],bronze:[113,79,43,0.92,0.44],leather:[62,36,26,0.0,0.83]};
fs.writeFileSync("SM_VeilOrderRitualBlade_v01.mtl",Object.entries(mats).map(([k,a])=>"newmtl "+k+"\nKd "+a.slice(0,3).map(v=>(v/255).toFixed(4)).join(" ")+"\nKs 0.2 0.2 0.2\nNs 30\n").join("\n"));
const W=32,H=32;
for(const [name,channel] of [["BaseColor",0],["Roughness",1],["Metallic",2],["Normal",3]]){let rows=["P3",W+" "+H,"255"];for(let y=0;y<H;y++){let line=[];for(let x=0;x<W;x++){let noise=((x*73+y*137+x*y*17)%29)-14;let rgb=channel===0?[Math.max(0,166+noise),Math.max(0,173+noise),Math.max(0,178+noise)]:channel===1?Array(3).fill(90+noise):channel===2?[230,230,230]:[128,128,255];line.push(rgb.join(" "))}rows.push(line.join(" "))}fs.writeFileSync("T_VeilBlade_"+name+"_draft.ppm",rows.join("\n")+"\n")}
fs.writeFileSync("mesh_stats.json",JSON.stringify({asset_id:"razlom.equipment.veil_order_ritual_blade",version:"0.1.0",vertices:V.length,triangles:F.length,uvs:T.length,material_slots:Object.keys(mats),unit:"cm",up_axis:"+Z",long_axis:"+Y",status:"materials_wip",lod:"not authored",ue5_import:"not tested",quality_floor:"BlackBell v3 art-approved, comparison pending"},null,2));
console.log({vertices:V.length,triangles:F.length});
