// Original Krivorog occult creature. Node.js, no external dependencies.
const fs=require('fs');
function build(high){
 const v=[],uv=[],f=[];const sides=high?12:7;
 function tube(mat,points,radii,n=sides){
 const start=v.length;
 for(let j=0;j<points.length;j++)for(let i=0;i<n;i++){let a=2*Math.PI*i/n,p=points[j],r=radii[j];v.push([p[0]+r[0]*Math.cos(a),p[1]+r[1]*Math.sin(a),p[2]]);uv.push([i/n,j/(points.length-1)]);}
 for(let j=0;j<points.length-1;j++)for(let i=0;i<n;i++){let a=start+j*n+i,b=start+j*n+(i+1)%n,c=b+n,d=a+n;f.push([a,b,d,mat],[b,c,d,mat]);}
 for(let i=1;i<n-1;i++){let k=start+(points.length-1)*n;f.push([start,start+i+1,start+i,mat],[k,k+i,k+i+1,mat]);}
 }
 tube('skin',[[0,0,.88],[0,0,1.25],[0,0,1.65],[0,0,1.97]],[[.23,.14],[.32,.2],[.27,.18],[.16,.13]]);
 tube('skin',[[0,0,1.9],[0,0,2.09]],[[.1,.1],[.09,.09]]);
 tube('skin',[[0,0,2.06],[0,.02,2.23],[0,.02,2.42],[0,0,2.48]],[[.12,.12],[.17,.15],[.145,.14],[.04,.04]]);
 for(let s of [-1,1]){
 tube('skin',[[s*.17,0,.98],[s*.2,-.03,.65],[s*.22,.06,.35],[s*.25,-.03,.13]],[[.14,.14],[.12,.12],[.085,.09],[.07,.08]]);
 tube('horn',[[s*.25,-.03,.13],[s*.25,-.14,.035]],[[.09,.11],[.13,.18]]);
 tube('skin',[[s*.27,0,1.83],[s*.43,-.03,1.48],[s*.5,-.1,1.12],[s*.51,-.16,.94]],[[.12,.11],[.095,.09],[.07,.07],[.075,.07]]);
 for(let i=0;i<3;i++){let x=s*(.47+i*.035),y=-.16+(i-1)*.055;tube('horn',[[x,y,.95],[x+s*.025,y-.045,.76],[x+s*.045,y-.07,.69]],[[.027,.027],[.02,.02],[.002,.002]],high?7:5);}
 }
 tube('horn',[[-.095,0,2.43],[-.17,.01,2.66],[-.24,-.05,2.85],[-.32,-.12,2.98]],[[.095,.085],[.075,.07],[.043,.04],[.003,.003]]);
 tube('horn',[[.1,0,2.43],[.19,.02,2.58],[.24,.075,2.65]],[[.085,.08],[.05,.048],[.002,.002]]);
 for(let x of [-.073,.073])tube('eye',[[x,-.146,2.29],[x,-.158,2.32]],[[.038,.015],[.034,.012]],high?9:6);
 for(let i=0;i<5;i++){let z=1.12+i*.16;tube('horn',[[0,.14,z],[0,.22,z+.065],[0,.26,z+.12]],[[.075,.035],[.045,.025],[.002,.002]],high?7:5);}
 let out=['# Original Krivorog, meters, Z-up','mtllib krivorog.mtl','o Krivorog'];
 for(let p of v)out.push('v '+p.map(x=>x.toFixed(5)).join(' '));
 for(let p of uv)out.push('vt '+p.map(x=>x.toFixed(6)).join(' '));
 let mat='';for(let t of f){if(mat!==t[3]){mat=t[3];out.push('usemtl '+mat);}out.push('f '+t.slice(0,3).map(i=>(i+1)+'/'+(i+1)).join(' '));}
 return {obj:out.join('\n')+'\n',vertices:v.length,triangles:f.length,uv:uv.length,invalid:f.filter(t=>new Set(t.slice(0,3)).size<3).length};
}
if(require.main===module){for(let h of [true,false]){let m=build(h);fs.writeFileSync('krivorog_LOD'+(h?0:1)+'.obj',m.obj);console.log(m.vertices,m.triangles);}}
module.exports={build};
