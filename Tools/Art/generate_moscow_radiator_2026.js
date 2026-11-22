// RAZLOM / Original cast iron section radiator for Moscow interiors, 2026. Node.js 18+.
const fs=require('node:fs');const path=require('node:path');
function generate(hi){
 const ts=[],PI=Math.PI,round=x=>+x.toFixed(6);
 const ncol=hi?8:8,segments=hi?16:8;
 function tri(a,b,c,m,part,uv){const A=b.map((x,i)=>x-a[i]),B=c.map((x,i)=>x-a[i]),n=[A[1]*B[2]-A[2]*B[1],A[2]*B[0]-A[0]*B[2],A[0]*B[1]-A[1]*B[0]],L=Math.hypot(...n);if(L<1e-10)throw Error("degenerate "+part);ts.push({v:[a,b,c].map(p=>p.map(round)),uv:uv||[[0,0],[1,0],[0,1]],n:n.map(x=>round(x/L)),material:m,part})}
 function quad(a,b,c,d,m,part,uv){const U=uv||[[0,0],[1,0],[1,1],[0,1]];tri(a,b,c,m,part,[U[0],U[1],U[2]]);tri(a,c,d,m,part,[U[0],U[2],U[3]])}
 function box(x,y,z,dx,dy,dz,m,part){const A=[x-dx/2,y-dy/2,z-dz/2],B=[x+dx/2,y+dy/2,z+dz/2],V=[[A[0],A[1],A[2]],[B[0],A[1],A[2]],[B[0],B[1],A[2]],[A[0],B[1],A[2]],[A[0],A[1],B[2]],[B[0],A[1],B[2]],[B[0],B[1],B[2]],[A[0],B[1],B[2]]];for(const f of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]])quad(...f.map(i=>V[i]),m,part)}
 function capsule(cx,cy,rings,ns,m,part){for(let k=0;k<rings.length-1;k++){const r=rings[k],s=rings[k+1];for(let j=0;j<ns;j++){const a=2*PI*j/ns,b=2*PI*(j+1)/ns,at=(R,t)=>[cx+R[1]*Math.cos(t),cy+R[2]*Math.sin(t),R[0]];quad(at(r,a),at(r,b),at(s,b),at(s,a),m,part,[[j/ns,k/(rings.length-1)],[(j+1)/ns,k/(rings.length-1)],[(j+1)/ns,(k+1)/(rings.length-1)],[j/ns,(k+1)/(rings.length-1)]])}}for(let j=0;j<ns;j++){const a=2*PI*j/ns,b=2*PI*(j+1)/ns;for(const [r,down] of [[rings[0],true],[rings[rings.length-1],false]]){const p=t=>[cx+r[1]*Math.cos(t),cy+r[2]*Math.sin(t),r[0]];if(down)tri([cx,cy,r[0]],p(b),p(a),m,part);else tri([cx,cy,r[0]],p(a),p(b),m,part)}}}
 function cylinder(a,b,r,m,part,ns=segments){
   const d=b.map((x,i)=>x-a[i]),L=Math.hypot(...d),w=d.map(x=>x/L);
   let seed=Math.abs(w[2])<.9?[0,0,1]:[0,1,0];
   let u=[w[1]*seed[2]-w[2]*seed[1],w[2]*seed[0]-w[0]*seed[2],w[0]*seed[1]-w[1]*seed[0]];const H=Math.hypot(...u);u=u.map(x=>x/H);
   let v=[w[1]*u[2]-w[2]*u[1],w[2]*u[0]-w[0]*u[2],w[0]*u[1]-w[1]*u[0]];
   const at=(C,t)=>C.map((x,k)=>x+r*(u[k]*Math.cos(t)+v[k]*Math.sin(t)));
   for(let i=0;i<ns;i++){let t=2*PI*i/ns,s=2*PI*(i+1)/ns,A=at(a,t),B=at(a,s),C=at(b,s),D=at(b,t);quad(A,B,C,D,m,part);tri(a,B,A,m,part);tri(b,D,C,m,part)}
 }
 function torusX(x,y,z,R,r,mat,part,major=hi?24:12,minor=hi?8:4){
    const at=(t,s)=>[x+r*Math.sin(s),y+(R+r*Math.cos(s))*Math.cos(t),z+(R+r*Math.cos(s))*Math.sin(t)];
    for(let i=0;i<major;i++)for(let j=0;j<minor;j++){let t=2*PI*i/major,u=2*PI*(i+1)/major,s=2*PI*j/minor,q=2*PI*(j+1)/minor;quad(at(t,s),at(u,s),at(u,q),at(t,q),mat,part)}
 }
 const prof=hi?[[.17,.011,.026],[.19,.025,.043],[.22,.035,.053],[.26,.036,.057],[.30,.024,.038],[.36,.022,.034],[.44,.022,.034],[.56,.022,.034],[.66,.026,.04],[.73,.034,.054],[.77,.037,.057],[.82,.03,.047],[.84,.012,.027]]:[[.17,.02,.04],[.22,.035,.05],[.33,.023,.038],[.67,.023,.038],[.76,.035,.05],[.83,.02,.037]];
 for(let i=0;i<ncol;i++){const cx=(i-3.5)*.090;
   for(let side of [-1,1]){let x=cx+side*(hi?.019:.012);
     capsule(x,0,prof,segments,"Enamel_Cream","cast_section_"+i+"_vertical");
     if(hi){for(let z of [.252,.76]){const band=[[z-.008,.039,.059],[z,.040,.061],[z+.008,.039,.059]];capsule(x,0,band,12,"Enamel_Edge","raised_mold_seam")}}
   }
   cylinder([cx-.040,0,.77],[cx+.040,0,.77],.035,"Enamel_Cream","section_upper_bridge",segments);
   cylinder([cx-.040,0,.24],[cx+.040,0,.24],.035,"Enamel_Cream","section_lower_bridge",segments);
   if(hi){cylinder([cx-.035,-.053,.742],[cx+.035,-.053,.742],.007,"Rust_Oxide","minor_paint_flake_seam",8)}
 }
 // Cast manifold along the upper and lower back, with section coupling rings.
 for(const z of [.242,.77]){cylinder([-.385,0,z],[.385,0,z],.020,"Cast_Iron_Exposed","continuous_back_manifold");
  for(let i=0;i<7;i++){let x=(i-3)*.09+.045;cylinder([x-.008,0,z],[x+.008,0,z],.025,"Enamel_Edge","coupling_collars",hi?12:8)}
 }
 // Rust traces are confined to mechanical seams and end caps, not uniform decay.
 for(const x of [-.355,.355])for(const z of [.245,.775]){
  cylinder([x-.010,0,z],[x+.010,0,z],.028,"Rust_Oxide","old_end_joint",hi?12:8);
 }
 // Wall mounts and rear saddles — wall lies behind +Y.
 for(const x of [-.265,.265]){
  box(x,.092,.64,.055,.151,.025,"Steel_Pipe","wall_bracket_upper_arm");
  box(x,.162,.637,.080,.012,.115,"Steel_Pipe","wall_anchor_backplate");
  box(x,.093,.275,.055,.151,.026,"Steel_Pipe","wall_bracket_lower_arm");
  box(x,.162,.275,.08,.012,.115,"Steel_Pipe","wall_anchor_lower_plate");
  for(const z of [.606,.669,.241,.310])cylinder([x,.166,z],[x,.17,z],.009,"Cast_Iron_Exposed","anchor_screw_head",hi?12:6);
 }
 // End outlet / supply connection to the valve and return at base.
 cylinder([.355,0,.77],[.484,0,.77],.024,"Steel_Pipe","supply_pipe");
 cylinder([.466,0,.77],[.503,0,.77],.032,"Brass_Valve","threaded_brass_union");
 cylinder([.492,0,.77],[.542,0,.77],.020,"Brass_Valve","valve_spindle");
 cylinder([-.484,0,.242],[-.355,0,.242],.025,"Steel_Pipe","return_pipe");
 cylinder([-.508,0,.242],[-.473,0,.242],.031,"Cast_Iron_Exposed","drain_union");
 // A proper ring wheel with radial spokes, not a flat decal.
 torusX(.557,0,.77,.051,.009,"Brass_Valve","five_spoke_valve_wheel");
 cylinder([.539,0,.77],[.565,0,.77],.014,"Brass_Valve","wheel_hub");
 for(let i=0;i<5;i++){const t=2*PI*i/5;cylinder([.557,0,.77],[.557,.048*Math.cos(t),.77+.048*Math.sin(t)],.006,"Brass_Valve","wheel_spokes",hi?12:6)}
 // Left-hand air bleed screw, modest and accessible.
 cylinder([-.385,0,.77],[-.428,0,.77],.019,"Steel_Pipe","air_bleed_nozzle");
 cylinder([-.430,0,.77],[-.446,0,.77],.023,"Brass_Valve","air_bleed_screw");
 return ts;
}
function toOBJ(ts,lod){
 const v=[],vt=[],vn=[],out=[],maps=[new Map(),new Map(),new Map()];
 function index(arr,map,vec){const k=vec.join("/");if(!map.has(k)){arr.push(vec);map.set(k,arr.length)}return map.get(k)}
 let group="";
 for(const t of ts){const g=t.part+"_"+t.material;if(g!==group){out.push("o "+t.part);out.push("usemtl "+t.material);group=g;}
 out.push("f "+t.v.map((p,i)=>index(v,maps[0],p)+"/"+index(vt,maps[1],t.uv[i].map(q=>+q.toFixed(6)))+"/"+index(vn,maps[2],t.n)).join(" "))}
 return "# RAZLOM Moscow 2026 original apartment radiator "+lod+", units meters Z up\nmtllib M_ApartmentRadiator_2026.mtl\n"+v.map(q=>"v "+q.join(" ")).join("\n")+"\n"+vt.map(q=>"vt "+q.join(" ")).join("\n")+"\n"+vn.map(q=>"vn "+q.join(" ")).join("\n")+"\n"+out.join("\n")+"\n";
}
const MTL="# ORIGINAL DRAFT PALETTE; FINAL PBR MAPS ARE NOT INCLUDED\nnewmtl Enamel_Cream\nKa 0.04 0.04 0.04\nKd 0.68 0.67 0.59\nKs 0.08 0.08 0.075\nNs 42\nillum 2\nnewmtl Enamel_Edge\nKa 0.04 0.04 0.04\nKd 0.55 0.55 0.51\nKs 0.08 0.08 0.08\nNs 34\nillum 2\nnewmtl Cast_Iron_Exposed\nKa 0.04 0.04 0.04\nKd 0.115 0.12 0.115\nKs 0.20 0.20 0.18\nNs 70\nillum 2\nnewmtl Rust_Oxide\nKa 0.04 0.04 0.04\nKd 0.205 0.083 0.036\nKs 0.025 0.025 0.022\nNs 14\nillum 2\nnewmtl Brass_Valve\nKa 0.04 0.04 0.04\nKd 0.50 0.32 0.095\nKs 0.35 0.28 0.12\nNs 88\nillum 2\nnewmtl Steel_Pipe\nKa 0.04 0.04 0.04\nKd 0.26 0.28 0.30\nKs 0.27 0.28 0.29\nNs 72\nillum 2\n";
const target=path.resolve(process.argv[2]||'Content/Art/Environment/Moscow2026/ApartmentRadiator');fs.mkdirSync(target,{recursive:true});for(const [lod,hi] of [['LOD0',true],['LOD1',false]]){const mesh=generate(hi);fs.writeFileSync(path.join(target,'SM_ApartmentRadiator_2026_'+lod+'.obj'),toOBJ(mesh,lod));console.log(lod,mesh.length,'triangles')}fs.writeFileSync(path.join(target,'M_ApartmentRadiator_2026.mtl'),MTL);
