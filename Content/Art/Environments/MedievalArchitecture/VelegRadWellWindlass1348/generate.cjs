// Original procedural medieval well windlass. Run: node generate.cjs
const fs=require('fs'),path=require('path');const root=__dirname;
function generate(lod){let lines=['# Original Razlom 1348 well windlass; Z-up, metres','mtllib materials.mtl'],index=1,tri=0,parts=0;
function emit(name,vs,faces,mat){lines.push('o '+name,'usemtl '+mat);for(const v of vs)lines.push('v '+v.map(x=>x.toFixed(6)).join(' '));for(let i=0;i<vs.length;i++)lines.push('vt '+((i%4===1||i%4===2)?1:0)+' '+((i%4===2||i%4===3)?1:0));for(const f of faces)for(let k=1;k<f.length-1;k++){lines.push('f '+[f[0],f[k],f[k+1]].map(j=>(index+j)+'/'+(index+j)).join(' '));tri++;}index+=vs.length;parts++;}
function box(name,[x,y,z],[a,b,c],mat){a/=2;b/=2;c/=2;let vs=[[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]].map(([u,v,w])=>[x+u*a,y+v*b,z+w*c]);emit(name,vs,[[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]],mat);}
function cyl(name,[x,y,z],r,length,axis,mat,n=12){let vs=[];for(const h of [-length/2,length/2])for(let i=0;i<n;i++){let t=2*Math.PI*i/n,u=r*Math.cos(t),v=r*Math.sin(t);vs.push(axis==='x'?[x+h,y+u,z+v]:axis==='y'?[x+u,y+h,z+v]:[x+u,y+v,z+h]);}let faces=[Array.from({length:n},(_,i)=>n-1-i),Array.from({length:n},(_,i)=>n+i)];for(let i=0;i<n;i++)faces.push([i,(i+1)%n,(i+1)%n+n,i+n]);emit(name,vs,faces,mat);}
const n=lod===0?12:8;
for(let tier=0;tier<3;tier++)for(let i=0;i<8;i++){let t=2*Math.PI*(i+(tier%2?0.5:0))/8;box('stone_'+tier+'_'+i,[1.03*Math.cos(t),1.03*Math.sin(t),0.17+tier*0.29],[0.75,0.39,0.275],'stone');}
for(const side of [-1,1]){box('oak_upright_'+side,[side*1.23,0,1.83],[0.22,0.25,2.5],'wood');box('iron_post_cap_'+side,[side*1.23,0,3.10],[0.27,0.3,0.10],'iron');}
cyl('oak_windlass',[0,0,2.42],0.20,2.8,'x','wood',n);
for(const x of [-1,1])cyl('iron_band_'+x,[x,0,2.42],0.215,0.095,'x','iron',n);
cyl('crank_shaft',[1.52,0,2.42],0.06,0.6,'x','iron',n);
box('crank_arm',[1.82,0,2.08],[0.11,0.10,0.72],'iron');
cyl('crank_handle',[1.82,0,1.72],0.08,0.32,'y','wood',n);
if(lod===0)for(let i=0;i<24;i++){let t=i*Math.PI/6;cyl('rope_coil_'+i,[-0.65+i*0.055,0.215*Math.cos(t),2.42+0.215*Math.sin(t)],0.025,0.10,'x','rope',8);}
cyl('bucket',[0.36,0,1.25],0.22,0.39,'z','wood',n);
for(const z of [1.08,1.41])cyl('bucket_hoop_'+z,[0.36,0,z],0.225,0.055,'z','iron',n);
box('hanging_rope',[0.36,0,1.92],[0.035,0.035,0.95],'rope');
fs.writeFileSync(path.join(root,'LOD'+lod+'.obj'),lines.join('\n')+'\n');return {vertices:index-1,triangles:tri,objects:parts};}
const a=generate(0),b=generate(1);fs.writeFileSync(path.join(root,'manifest.json'),JSON.stringify({asset_id:'RZ_ENV_VELEGRAD_WELL_WINDLASS_1348_001',version:'0.1.0',status:'geometry_wip',epoch:1348,units:'metres',up:'Z',lod0:a,lod1:b,materials:['stone','wood','iron','rope'],collision:'not authored',ue5_verified:false},null,2)+'\n');console.log(a,b);
