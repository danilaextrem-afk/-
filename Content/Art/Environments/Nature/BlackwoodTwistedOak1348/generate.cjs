// Original procedural twisted oak for RAZLOM. Node.js 18+.
const fs=require('fs');
const generate=function generate(lod=0){
 const V=[],UV=[],F=[],groups=[];let material="bark";
 function vert(x,y,z,u,v){V.push([x,y,z]);UV.push([u,v]);return V.length}
 function tube(a,b,r0,r1,sides,mat){let start=F.length;for(let k=0;k<2;k++){let p=k?b:a,r=k?r1:r0;let dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],len=Math.hypot(dx,dy,dz),nx=-dy/Math.hypot(dx,dy),ny=dx/Math.hypot(dx,dy);if(!Number.isFinite(nx)){nx=1;ny=0}let ux=ny*dz/len,uy=-nx*dz/len,uz=(nx*dy-ny*dx)/len;for(let j=0;j<=sides;j++){let t=j/sides*2*Math.PI,co=Math.cos(t),si=Math.sin(t);vert(p[0]+r*(nx*co+ux*si),p[1]+r*(ny*co+uy*si),p[2]+r*uz*si,j/sides,k)}}let off=V.length-2*(sides+1);for(let j=0;j<sides;j++){let x=off+j+1,y=off+j+2,z=off+sides+j+2,w=off+sides+j+3;F.push([x,y,z],[y,w,z])}groups.push({mat,start,count:F.length-start})}
 const n=lod?7:12;
 let trunk=[[0,0,0],[.08,0,.65],[.14,-.04,1.35],[.08,.12,2.1],[-.12,.16,2.8],[-.25,.1,3.5],[-.42,.1,4.15]];
 for(let i=0;i<trunk.length-1;i++)tube(trunk[i],trunk[i+1],.48-i*.052,.43-i*.052,n,"bark");
 for(let i=0;i<(lod?5:8);i++){let a=trunk[2+i%4],ang=i*2.39996,dir=[Math.cos(ang),Math.sin(ang)];let mid=[a[0]+dir[0]*(.85+i*.04),a[1]+dir[1]*(.85+i*.04),a[2]+.55+(i%3)*.15],end=[mid[0]+dir[0]*(.8+i*.03),mid[1]+dir[1]*(.8+i*.03),mid[2]+.3];tube(a,mid,.19,.11,n,"bark");tube(mid,end,.11,.025,n,"bark");if(!lod){let twig=[end[0]+dir[1]*.35,end[1]-dir[0]*.35,end[2]+.4];tube(mid,twig,.055,.008,7,"bark")}}
 for(let i=0;i<(lod?5:8);i++){let ang=i*2*Math.PI/(lod?5:8),dir=[Math.cos(ang),Math.sin(ang)];tube([dir[0]*.15,dir[1]*.15,.23],[dir[0]*(.9+(i%3)*.12),dir[1]*(.9+(i%3)*.12),-.08],.22,.045,n,"root")}
 let obj="# Original procedural oak, meters Z-up; UV per vertex\nmtllib materials.mtl\n";V.forEach(v=>obj+="v "+v.map(x=>x.toFixed(5)).join(" ")+"\n");UV.forEach(v=>obj+="vt "+v.map(x=>x.toFixed(5)).join(" ")+"\n");let prev="",g=0;for(let i=0;i<F.length;i++){while(g<groups.length-1&&i>=groups[g].start+groups[g].count)g++;let m=groups[g].mat;if(m!==prev){obj+="usemtl "+m+"\n";prev=m}obj+="f "+F[i].map(x=>x+"/"+x).join(" ")+"\n"}return {obj,vertices:V.length,triangles:F.length,uv:UV.length,invalid:F.filter(f=>f.some(x=>x<1||x>V.length)||new Set(f).size<3).length}
};
for(const [name,lod] of [['LOD0',0],['LOD1',1]])fs.writeFileSync(name+'.obj',generate(lod).obj);
