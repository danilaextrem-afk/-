// Original deterministic mesh generator. Node.js: node generate.cjs
const fs=require('fs');const path=require('path');
function mesh(detail){
 const v=[],uv=[],f=[];let material='';
 const vert=(x,y,z,u,w)=>{v.push([x,y,z]);uv.push([u,w]);return v.length};
 const tri=(a,b,c,m)=>f.push([a,b,c,m]);
 function box(x,y,z,w,h,d,m){
 const p=[[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]];
 const q=[[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]];
 for(const face of q){const a=face.map((i,k)=>vert(x+p[i][0]*w/2,y+p[i][1]*h/2,z+p[i][2]*d/2,k===1||k===2?1:0,k>1?1:0));tri(a[0],a[1],a[2],m);tri(a[0],a[2],a[3],m)}
 }
 function cylinder(x,y,z,r,h,n,m){
 const low=[],high=[];for(let i=0;i<n;i++){const a=i*2*Math.PI/n;low.push(vert(x+r*Math.cos(a),y-h/2,z+r*Math.sin(a),i/n,0));high.push(vert(x+r*Math.cos(a),y+h/2,z+r*Math.sin(a),i/n,1))}
 const a=vert(x,y-h/2,z,.5,.5),b=vert(x,y+h/2,z,.5,.5);
 for(let i=0;i<n;i++){const j=(i+1)%n;tri(low[i],low[j],high[j],m);tri(low[i],high[j],high[i],m);tri(a,low[j],low[i],m);tri(b,high[i],high[j],m)}
 }
 const n=detail?12:6;
 cylinder(0,-.25,0,.063,1.4,n,'oak');
 for(const y of [-.74,-.57,.25,.38])cylinder(0,y,0,.077,.048,n,'bronze');
 box(0,.54,0,.78,.24,.24,'aged_iron');
 for(const x of [-.40,.40])box(x,.54,0,.13,.20,.19,'blackened_steel');
 for(const x of [-.27,.27])for(const z of [-.14,.14])cylinder(x,.69,z,.025,.03,detail?8:4,'bronze');
 for(const z of [-.14,.14])for(const x of [-.22,0,.22]){
 cylinder(x,.54,z,.061,.025,n,'seal_ash');
 if(detail)box(x,.54,z+(z>0?.02:-.02),.07,.013,.014,'bronze');
 }
 cylinder(0,-.99,0,.09,.08,n,'blackened_steel');
 if(!v.every(p=>p.every(Number.isFinite))||!f.every(([a,b,c])=>a!==b&&b!==c&&a!==c&&a>0&&b>0&&c>0&&a<=v.length&&b<=v.length&&c<=v.length))throw Error('geometry invalid');
 let s='# Original mesh, meters, Y up\nmtllib B3_Sealbreaker_Hammer.mtl\n';
 for(const p of v)s+='v '+p.map(x=>x.toFixed(6)).join(' ')+'\n';
 for(const p of uv)s+='vt '+p.join(' ')+'\n';
 for(const [a,b,c,m] of f){if(m!==material){s+='usemtl '+m+'\n';material=m}s+='f '+[a,b,c].map(i=>i+'/'+i).join(' ')+'\n'}
 return {obj:s,vertices:v.length,triangles:f.length};
}
for(const [lod,detail] of [[0,true],[1,false]]){const x=mesh(detail);fs.writeFileSync(path.join(__dirname,'B3_Sealbreaker_Hammer_LOD'+lod+'.obj'),x.obj);console.log('LOD'+lod,x.vertices,x.triangles)}
