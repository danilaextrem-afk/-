// Original procedural B3 phase-1 body blocking mesh. Node 18+; metres, Z up.
const fs=require('fs');const path=require('path');
function build(lod=0){
 const vertices=[],uv=[],faces=[],groups=[];let material="bone";
 function v(x,y,z,u,w){vertices.push([x,y,z]);uv.push([u,w]);return vertices.length}
 function tri(a,b,c){faces.push([a,b,c]);groups.push(material)}
 function tube(name,centers,radii,sides=12){
  const first=vertices.length+1;for(let j=0;j<centers.length;j++){
   const p=centers[j],r=radii[j];for(let i=0;i<sides;i++){
    const a=2*Math.PI*i/sides;v(p[0]+r[0]*Math.cos(a),p[1]+r[1]*Math.sin(a),p[2],i/sides,j/(centers.length-1));
   }
  }
  for(let j=0;j<centers.length-1;j++)for(let i=0;i<sides;i++){const a=first+j*sides+i,b=first+j*sides+(i+1)%sides,c=b+sides,d=a+sides;tri(a,b,c);tri(a,c,d)}
  for(let i=1;i<sides-1;i++){tri(first,first+i+1,first+i);let t=first+(centers.length-1)*sides;tri(t,t+i,t+i+1)}
 }
 function part(mat,pts,rad,s){material=mat;tube("",pts,rad,s)}
 const s=lod?8:16;
 // Torso, shoulders, pelvis, skull: visibly separate organic silhouettes
 part("corroded_armor",[[0,0,.88],[0,0,1.06],[0,0,1.32],[0,0,1.55],[0,0,1.66]],[[.19,.13],[.26,.17],[.30,.18],[.35,.16],[.12,.10]],s);
 part("aged_bone",[[0,0,1.59],[0,0,1.76],[0,0,1.91],[0,0,2.04]],[[.09,.09],[.13,.13],[.135,.115],[.07,.075]],s);
 part("corroded_armor",[[0,0,.72],[0,0,.85],[0,0,.98]],[[.20,.14],[.26,.17],[.22,.14]],s);
 for(const side of [-1,1]){
  const x=side;
  part("aged_bone",[[x*.20,0,.80],[x*.22,0,.56],[x*.24,0,.29],[x*.24,-.03,.10]],[[.11,.11],[.095,.09],[.075,.075],[.09,.13]],s);
  part("corroded_armor",[[x*.29,0,1.54],[x*.43,0,1.42],[x*.48,0,1.15],[x*.49,-.01,.91]],[[.14,.13],[.13,.13],[.09,.09],[.07,.075]],s);
  part("aged_bone",[[x*.49,-.01,.92],[x*.50,-.01,.79],[x*.51,-.03,.67]],[[.08,.07],[.07,.06],[.045,.045]],s);
  // uneven royal mantle drape, torn cloth in broad scallops
  material="funeral_cloth";const n=lod?5:11;
  for(let i=0;i<n;i++){const a=-1.1+2.2*i/(n-1),z=1.52-.42*(i%3)/3;
   const p0=[Math.sin(a)*.36,Math.cos(a)*.16,1.56],p1=[Math.sin(a)*(.48+ .04*(i%2)),Math.cos(a)*.24,.98],p2=[Math.sin(a)*(.52+.05*(i%3)),Math.cos(a)*.27,.47+.08*(i%3)];
   const k=vertices.length+1;for(const p of [p0,p1,p2])v(...p,i/(n-1),p[2]/2);tri(k,k+1,k+2);
  }
 }
 // Crown: eight teeth of unequal heights, rigid crown ring
 material="tarnished_gold";
 const n=lod?8:16;const start=vertices.length+1;
 for(let j=0;j<2;j++)for(let i=0;i<n;i++){let a=2*Math.PI*i/n;v(.16*Math.cos(a),.16*Math.sin(a),1.99+j*.095,i/n,j)}
 for(let i=0;i<n;i++){let a=start+i,b=start+(i+1)%n,c=b+n,d=a+n;tri(a,b,c);tri(a,c,d)}
 for(let i=0;i<8;i++){let a=2*Math.PI*i/8;const k=vertices.length+1;v(.16*Math.cos(a-.18),.16*Math.sin(a-.18),2.08,0,0);v(.16*Math.cos(a+.18),.16*Math.sin(a+.18),2.08,1,0);v(.16*Math.cos(a),.16*Math.sin(a),2.17+.06*(i%3),.5,1);tri(k,k+1,k+2)}
 // three ritual seals on chest, shoulder and back, reserved for breakable phase variants
 for(const [x,y,z] of [[0,-.195,1.34],[-.39,-.04,1.47],[0,.19,1.21]]){
 material="seal_ember";const k=vertices.length+1;v(x-.065,y,z-.065,0,0);v(x+.065,y,z-.065,1,0);v(x+.065,y,z+.065,1,1);v(x-.065,y,z+.065,0,1);tri(k,k+1,k+2);tri(k,k+2,k+3)
 }
 let out="# Original B3 Prince without Grave phase-1 body; metres Z-up; draft\nmtllib B3_Prince.mtl\n";
 for(const p of vertices)out+="v "+p.map(x=>x.toFixed(6)).join(" ")+"\n";
 for(const p of uv)out+="vt "+p.map(x=>x.toFixed(6)).join(" ")+"\n";
 let last="";for(let i=0;i<faces.length;i++){if(groups[i]!==last){last=groups[i];out+="usemtl "+last+"\n"}out+="f "+faces[i].map(k=>k+"/"+k).join(" ")+"\n"}
 return {out,stats:{vertices:vertices.length,triangles:faces.length,uv:uv.length},vertices,uv,faces}
}
if(require.main===module){const dest=process.argv[2]||".";fs.mkdirSync(dest,{recursive:true});for(const lod of [0,1])fs.writeFileSync(path.join(dest,"B3_Prince_LOD"+lod+".obj"),build(lod).out)}
module.exports={build};