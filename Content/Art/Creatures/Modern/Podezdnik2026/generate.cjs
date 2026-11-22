// Original procedural Pod'ezdnik mesh, Z-up meters. Node.js 18+; no dependencies.
const fs=require('fs');
function build(lod){
 const V=[],UV=[],F=[];let mat='';
 function tube(name,pts,rr,n){let st=V.length;
  pts.forEach(([x,y,z],i)=>{for(let j=0;j<n;j++){let a=2*Math.PI*j/n;V.push([x+rr[i]*Math.cos(a),y+rr[i]*Math.sin(a),z]);UV.push([j/n,i/(pts.length-1)]);}});
  for(let i=0;i<pts.length-1;i++)for(let j=0;j<n;j++){let a=st+i*n+j,b=st+i*n+(j+1)%n,c=b+n,d=a+n;F.push([a,b,c,name],[a,c,d,name]);}
 }
 const n=lod===0?16:8;
 tube('charred_skin',[[0,0,.86],[.02,0,1.12],[.04,0,1.5],[.08,0,1.83],[.17,0,2.07],[.22,0,2.22]],[.17,.23,.22,.16,.12,.07],n);
 tube('void_face',[[.22,0,2.19],[.26,0,2.35],[.30,0,2.5],[.32,0,2.61]],[.09,.13,.115,.035],n);
 for(let s of [-1,1]){
  tube('charred_skin',[[s*.14,0,.98],[s*.18,0,.57],[s*.21,0,.13],[s*.21,0,.025]],[.105,.083,.052,.075],n);
  tube('charred_skin',[[s*.19,0,1.91],[s*.35,0,1.67],[s*.44,0,1.27],[s*.49,0,.91]],[.11,.08,.057,.045],n);
  for(let k=0;k<3;k++)tube('bone_claw',[[s*(.45+.045*k),.025*(k-1),.93],[s*(.48+.055*k),.03*(k-1),.74],[s*(.51+.06*k),.04*(k-1),.60]],[.025,.018,.004],lod===0?8:5);
 }
 for(let s of [-1,1])tube('eye_ash',[[.34,s*.055,2.46],[.38,s*.055,2.46]],[.034,.034],8);
 let out='# Original Razlom Pod'ezdnik, meters Z up, draft\nmtllib podezdnik.mtl\n';
 V.forEach(v=>out+='v '+v.map(x=>x.toFixed(6)).join(' ')+'\n');
 UV.forEach(v=>out+='vt '+v.map(x=>x.toFixed(6)).join(' ')+'\n');
 let last='';F.forEach(([a,b,c,m])=>{if(m!==last){out+='usemtl '+m+'\n';last=m;}out+='f '+[a,b,c].map(i=>(i+1)+'/'+(i+1)).join(' ')+'\n';});
 let deg=0;for(let [a,b,c] of F){let u=V[b].map((x,i)=>x-V[a][i]),v=V[c].map((x,i)=>x-V[a][i]);let cross=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];if(Math.hypot(...cross)<2e-9)deg++;}
 return {obj:out,qa:{vertices:V.length,uv:UV.length,triangles:F.length,degenerate:deg}};
}
for(let lod of [0,1]){let r=build(lod);fs.writeFileSync('podezdnik_LOD'+lod+'.obj',r.obj);console.log(lod,r.qa);}
