// Original project procedural wattle fence kit (Node 18+; no external asset libraries).
const fs=require('node:fs'),path=require('node:path');
function build(kind,lod){
 const hi=lod===0,pi=Math.PI,F=[],r=x=>+x.toFixed(5);
 function triangle(a,b,c,m,part,uv){const ab=b.map((x,i)=>x-a[i]),ac=c.map((x,i)=>x-a[i]),norm=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],len=Math.hypot(...norm);if(len<1e-9)throw Error("degenerate "+part);F.push({v:[a,b,c].map(v=>v.map(r)),n:norm.map(x=>r(x/len)),uv:uv||[[0,0],[1,0],[0,1]],m,part})}
 function quad(a,b,c,d,m,part){triangle(a,b,c,m,part);triangle(a,c,d,m,part,[[0,0],[1,1],[0,1]])}
 function box(x,y,z,dx,dy,dz,m,part){const a=[x-dx/2,y-dy/2,z-dz/2],b=[x+dx/2,y+dy/2,z+dz/2],v=[[a[0],a[1],a[2]],[b[0],a[1],a[2]],[b[0],b[1],a[2]],[a[0],b[1],a[2]],[a[0],a[1],b[2]],[b[0],a[1],b[2]],[b[0],b[1],b[2]],[a[0],b[1],b[2]]];for(const f of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]])quad(...f.map(i=>v[i]),m,part)}
 function rings(path,radius,material,part,N=hi?10:6){
  for(let k=0;k<path.length-1;k++){const a=path[k],b=path[k+1],v=b.map((x,i)=>x-a[i]),size=Math.hypot(...v),axis=v.map(x=>x/size),seed=Math.abs(axis[2])<.85?[0,0,1]:[0,1,0];let u=[axis[1]*seed[2]-axis[2]*seed[1],axis[2]*seed[0]-axis[0]*seed[2],axis[0]*seed[1]-axis[1]*seed[0]],L=Math.hypot(...u);u=u.map(x=>x/L);const w=[axis[1]*u[2]-axis[2]*u[1],axis[2]*u[0]-axis[0]*u[2],axis[0]*u[1]-axis[1]*u[0]];const p=(origin,t)=>origin.map((x,i)=>x+radius*(u[i]*Math.cos(t)+w[i]*Math.sin(t)));
    for(let j=0;j<N;j++){const t=2*pi*j/N,s=2*pi*(j+1)/N;quad(p(a,t),p(a,s),p(b,s),p(b,t),material,part);
     if(k===0)triangle(a,p(a,s),p(a,t),material,part+"_cut");
     if(k===path.length-2)triangle(b,p(b,t),p(b,s),material,part+"_cut");
    }
  }
 }
 function taperedStake(x,y,height,rad,seed){const seg=hi?12:7;const profile=[[0,rad*1.07],[.18,rad],[height-.18,rad*.83],[height-.08,rad*.7],[height,.004]];for(let k=0;k<profile.length-1;k++){const [a,ra]=profile[k],[b,rb]=profile[k+1];for(let j=0;j<seg;j++){let t=2*pi*j/seg,u=2*pi*(j+1)/seg;
    const at=(z,R,v)=>[x+Math.cos(v)*R*(1+.025*Math.sin(seed*2.17+z*3+v*2)),y+Math.sin(v)*R,z];
    quad(at(a,ra,t),at(a,ra,u),at(b,rb,u),at(b,rb,t),"Hazel_Bark","hewn_vertical_stake");
    if(k===0)triangle([x,y,0],at(a,ra,u),at(a,ra,t),"Hazel_Cut","stake_bottom");
  }}}
 function twistedBinding(x,y,z,N=hi?13:7){const path=[];for(let i=0;i<N;i++){const t=i*2*pi*1.5/(N-1);path.push([x+Math.cos(t)*.063,y+Math.sin(t)*.063,z+(i/(N-1))*.058]);}rings(path,.007,"Hemp_Rope","hand_lashed_binding",hi?7:5)}
 function pin(x,y,z){rings([[x,y,z],[x,y-.04,z]],.012,"Iron_Forged","hammered_reused_nail",hi?10:6)}
 if(kind==="Fence2m"){let stakes=hi?7:5;for(let i=0;i<stakes;i++){const x=.06+i*1.88/(stakes-1);taperedStake(x,0,1.59,i===0||i===stakes-1?.065:.046,i+2);
    if(hi&&i%2===0)for(const z of [.38,1.12])twistedBinding(x,0,z);}
    const rows=hi?13:7;for(let row=0;row<rows;row++){const z=.24+row*.094+(row%3)*.003;const steps=hi?28:9;const path=[];
       for(let k=0;k<=steps;k++){const x=k*2/steps,phase=2*pi*x/.32+row*pi*.84;
        const y=.069*Math.sin(phase)+.006*Math.sin(5*x+row*1.2);
        path.push([x,y,z+.007*Math.sin(1.25*x+row)]);
       }rings(path,hi?.021:.029,row%4===0?"Wattle_Dried":"Hazel_Bark","woven_horizontal_rods",hi?8:6);
    }
    if(hi)for(const side of [0,2])for(let row=0;row<13;row+=3){let x=side===0?.07:1.93;twistedBinding(x,0,.25+row*.094);}
 }
 if(kind==="CornerPost"){taperedStake(0,0,1.78,.115,7);
    for(let z of [.29,.89,1.35]){rings([[-.31,0,z],[.31,0,z]],.037,"Hazel_Cut","mortised_joiner_X",hi?12:7);
       rings([[0,-.31,z],[0,.31,z]],.037,"Hazel_Cut","mortised_joiner_Y",hi?12:7);
       twistedBinding(0,0,z);}
    if(hi)for(let z of [.35,1.25])pin(.097,-.03,z);
 }
 if(kind==="EndPost"){taperedStake(0,0,1.72,.10,11);
    for(let z of [.27,.85,1.36]){rings([[0,0,z],[-.23,0,z]],.037,"Hazel_Cut","one_side_rail_sockets",hi?12:7);twistedBinding(0,0,z)}
    // Iron hinge/tie mount for gate; this is a fixed post, leaf rotates separately.
    for(let z of [.33,1.30]){box(.07,-.08,z,.098,.035,.15,"Iron_Forged","gate_hinge_plate");rings([[.1,-.10,z-.064],[.1,-.10,z+.064]],.021,"Iron_Forged","vertical_pivot_iron",hi?14:8)}
    if(hi)pin(-.09,-.01,.85);
 }
 if(kind==="GateLeaf1m"){
    // Local hinge x=0, gate is x=0..1.08. Bottom 0.17, upper 1.40.
    const left=.045,right=1.035;
    for(let x of [left,right])rings([[x,0,.20],[x,0,1.37]],.052,"Hazel_Cut","gate_side_post",hi?12:8);
    for(let z of [.27,.75,1.30])rings([[left,0,z],[right,0,z]],.047,"Hazel_Cut","gate_horizontal_rails",hi?12:8);
    rings([[left,0,.25],[right,0,1.28]],.033,"Hazel_Cut","cross_brace",hi?12:8);
    for(let z of [.46,1.0]){twistedBinding(left,0,z);twistedBinding(right,0,z)}
    const count=hi?8:4;
    for(let row=0;row<count;row++){
      let z=.35+row*1/(count+.2),n=hi?16:6,path=[];
      for(let i=0;i<=n;i++){let x=.09+i*.89/n;path.push([x,.058*Math.sin((x/.19+row)*pi),z+.005*Math.sin(x*6+row)])}
      rings(path,hi?.019:.029,"Wattle_Dried","handwoven_gate_laths",hi?8:6);
    }
    for(const z of [.35,1.25]){box(.036,.075,z,.075,.047,.13,"Iron_Forged","hinge_iron_eye");rings([[.035,.1,z-.07],[.035,.1,z+.07]],.025,"Iron_Forged","pivot_loop",hi?12:8)}
    box(.995,-.06,.76,.10,.043,.14,"Iron_Forged","wrought_iron_latch_base");
    rings([[.96,-.08,.76],[1.17,-.08,.76]],.018,"Iron_Forged","manual_latch_bar",hi?12:7);
    rings([[1.16,-.08,.76],[1.16,-.105,.76]],.015,"Iron_Forged","latch_grip",hi?10:6);
 }
 if(!F.length)throw Error("empty "+kind);
 const min=[Infinity,Infinity,Infinity],max=[-Infinity,-Infinity,-Infinity];
 for(const f of F)for(const v of f.v)for(let j=0;j<3;j++){min[j]=Math.min(min[j],v[j]);max[j]=Math.max(max[j],v[j]);}
 return {faces:F,stats:{triangles:F.length,min,max,materials:[...new Set(F.map(f=>f.m))].sort()}};
}
function toOBJ(mesh,name,lod){
 const vs=[],uvs=[],ns=[],body=[],maps=[new Map(),new Map(),new Map()];
 const index=(arr,map,val)=>{const key=val.join(",");if(!map.has(key)){arr.push(val);map.set(key,arr.length)}return map.get(key)};
 let prev="";
 for(const f of mesh.faces){const key=f.part+"|"+f.m;if(key!==prev){body.push("o "+f.part,"usemtl "+f.m);prev=key;}
  // UV projection in physical meters with material-aware orientation and cylindrical x-axis rods.
  const v3=(p,n)=>{const abs=n.map(Math.abs),j=abs.indexOf(Math.max(...abs));
   return (j===0?[p[1]*2.3,p[2]*2.3]:j===1?[p[0]*2.3,p[2]*2.3]:[p[0]*2.3,p[1]*2.3]).map(x=>+x.toFixed(5))};
  body.push("f "+f.v.map(p=>index(vs,maps[0],p)+"/"+index(uvs,maps[1],v3(p,f.n))+"/"+index(ns,maps[2],f.n)).join(" "));
 }
 return `# RAZLOM: BLACK BELL original 1348 village wattle ${name} LOD${lod} (meters Z-up)\nmtllib M_WattleFence_1348.mtl\n`+
 vs.map(p=>"v "+p.join(" ")).join("\n")+"\n"+uvs.map(p=>"vt "+p.join(" ")).join("\n")+"\n"+ns.map(p=>"vn "+p.join(" ")).join("\n")+"\n"+body.join("\n")+"\n";
}
const out=path.join(__dirname,'..','..','Content','Art','Environment','Medieval1348','WattleFenceKit');fs.mkdirSync(out,{recursive:true});
for(const kind of ["Fence2m","CornerPost","EndPost","GateLeaf1m"])for(const lod of [0,1]){const data=build(kind,lod);fs.writeFileSync(path.join(out,'SM_Wattle_'+kind+'_1348_LOD'+lod+'.obj'),toOBJ(data,kind,lod));console.log(kind,'LOD'+lod,data.stats.triangles,'triangles')}
