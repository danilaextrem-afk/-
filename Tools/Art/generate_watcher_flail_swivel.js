/* Original procedural mesh; units metres; run: node generate_watcher_flail_swivel.js */
const fs=require('fs');
function build(){
 const v=[],uv=[],faces=[],groups=[];let group="";
 const add=(p,t)=>{v.push(p);uv.push(t);return v.length};
 const tri=(a,b,c)=>faces.push([a,b,c]);
 function torus(name,center,major,minor,axis,segments=32,tube=12){
  groups.push([name,faces.length]);
  const ids=[];
  for(let i=0;i<=segments;i++){ids[i]=[];let a=2*Math.PI*i/segments;
   for(let j=0;j<=tube;j++){let b=2*Math.PI*j/tube,R=major+minor*Math.cos(b),h=minor*Math.sin(b);
    let p=axis==="x"?[center[0]+h,center[1]+R*Math.cos(a),center[2]+R*Math.sin(a)]:[center[0]+R*Math.cos(a),center[1]+h,center[2]+R*Math.sin(a)];
    ids[i][j]=add(p,[i/segments,j/tube]);
   }
  }
  for(let i=0;i<segments;i++)for(let j=0;j<tube;j++){let a=ids[i][j],b=ids[i+1][j],c=ids[i+1][j+1],d=ids[i][j+1];tri(a,b,c);tri(a,c,d)}
 }
 function cylinder(name,c,rad,len,axis,n=32){
  groups.push([name,faces.length]);let rings=[];
  for(let k=0;k<2;k++){rings[k]=[];for(let i=0;i<=n;i++){let a=i*2*Math.PI/n,p=axis==="z"?[c[0]+rad*Math.cos(a),c[1]+rad*Math.sin(a),c[2]+(k-.5)*len]:[c[0]+(k-.5)*len,c[1]+rad*Math.cos(a),c[2]+rad*Math.sin(a)];rings[k][i]=add(p,[i/n,k]);}}
  for(let i=0;i<n;i++){let a=rings[0][i],b=rings[0][i+1],c=rings[1][i+1],d=rings[1][i];tri(a,b,c);tri(a,c,d)}
  for(let k=0;k<2;k++){let center=add(axis==="z"?[c[0],c[1],c[2]+(k-.5)*len]:[c[0]+(k-.5)*len,c[1],c[2]],[.5,.5]);for(let i=0;i<n;i++)k?tri(center,rings[k][i],rings[k][i+1]):tri(center,rings[k][i+1],rings[k][i]);}
 }
 torus("Eye_Head",[0,0,.055],.045,.008,"x");torus("Eye_Chain",[0,0,-.055],.045,.008,"z");
 cylinder("Swivel_Barrel",[0,0,0],.018,.082,"z");
 cylinder("Collar_Upper",[0,0,.032],.024,.012,"z");cylinder("Collar_Lower",[0,0,-.032],.024,.012,"z");
 for(let i=0;i<6;i++){let a=i*Math.PI/3;cylinder("Rivet_"+i,[.024*Math.cos(a),.024*Math.sin(a),0],.004,.012,"z",12)}
 let obj="# RAZLOM original swivel geometry, metres, Z up\nmtllib SM_Watcher_Flail_Swivel_v1.mtl\no SM_Watcher_Flail_Swivel_v1\n";
 for(const p of v)obj+="v "+p.map(x=>x.toFixed(6)).join(" ")+"\n";
 for(const p of uv)obj+="vt "+p.map(x=>x.toFixed(6)).join(" ")+"\n";
 let gi=0;for(let i=0;i<faces.length;i++){if(gi<groups.length&&groups[gi][1]===i){obj+="g "+groups[gi][0]+"\nusemtl ForgedIron\n";gi++}obj+="f "+faces[i].map(n=>n+"/"+n).join(" ")+"\n";}
 return {obj,stats:{vertices:v.length,uv:uv.length,triangles:faces.length,groups:groups.length},v,faces};
}
const result=build(); fs.writeFileSync('SM_Watcher_Flail_Swivel_v1.obj',result.obj); console.log(result.stats);
