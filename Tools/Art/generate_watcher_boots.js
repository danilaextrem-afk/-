// Original procedural Watcher paired boots. Run: node Tools/Art/generate_watcher_boots.js > Content/Art/Characters/Watcher/SM_Watcher_Boots_v1.obj
function generate(){
 const V=[],F=[],G=[];let group="";
 const v=(x,y,z)=>{V.push([x,y,z]);return V.length};
 const tri=(a,b,c)=>F.push([a,b,c]);
 const quad=(a,b,c,d)=>{tri(a,b,c);tri(a,c,d)};
 const ring=(cx,cy,cz,rx,rz,n=12)=>Array.from({length:n},(_,i)=>{const t=2*Math.PI*i/n;return v(cx+rx*Math.cos(t),cy,cz+rz*Math.sin(t))});
 const loft=(sections,n=12)=>{let prev=null;for(const s of sections){let next=ring(...s,n);if(prev)for(let i=0;i<n;i++)quad(prev[i],prev[(i+1)%n],next[(i+1)%n],next[i]);prev=next;}};
 const box=(cx,cy,cz,sx,sy,sz)=>{let k=V.length;for(let z of [-1,1])for(let y of [-1,1])for(let x of [-1,1])v(cx+x*sx/2,cy+y*sy/2,cz+z*sz/2);for(let q of [[1,2,4,3],[5,7,8,6],[1,5,6,2],[3,4,8,7],[1,3,7,5],[2,6,8,4]])quad(...q.map(i=>k+i));};
 const boot=(side)=>{const x=side*.18;group=side<0?"Watcher_LeftBoot":"Watcher_RightBoot";G.push([group,F.length]);loft([[x,.025,.075,.115,.08],[x,.045,.075,.118,.082],[x,.10,.075,.12,.086],[x,.15,.065,.105,.075],[x,.27,.06,.093,.073],[x,.40,.052,.087,.068],[x,.52,.045,.093,.07]],16);
 // toe points towards -Z; armor and layered toe cap
 loft([[x,.045,-.045,.105,.075],[x,.055,-.095,.108,.08],[x,.07,-.155,.105,.075],[x,.095,-.18,.09,.062],[x,.115,-.15,.075,.05]],16);
 for(let j=0;j<4;j++){let y=.19+j*.075;loft([[x,y,.065,.103-j*.003,.077],[x,y+.012,.065,.109-j*.003,.08],[x,y+.032,.065,.101-j*.003,.073]],16);}
 // side straps, rivets
 for(let s of [-1,1])for(let j=0;j<3;j++){let y=.225+j*.105;box(x+s*.096,y,.02,.022,.036,.105);loft([[x+s*.109,y,.073,.013,.013],[x+s*.111,y+.008,.073,.014,.014]],8);}
 // ankle cuff, heel iron, ornamental forward spur
 loft([[x,.49,.048,.105,.079],[x,.51,.048,.111,.083],[x,.54,.048,.109,.082]],16);
 box(x,.12,.145,.12,.085,.023);
 loft([[x,.125,-.19,.024,.022],[x,.135,-.23,.018,.017],[x,.14,-.258,.003,.004]],10);
 };
 boot(-1);boot(1);
 const lines=["# RAZLOM Ninth Entrance Watcher boots v1","# metres; Z forward; Y up; paired boots; procedural draft"];
 for(const p of V)lines.push("v "+p.map(n=>n.toFixed(6)).join(" "));
 let idx=0;for(let i=0;i<F.length;i++){if(idx<G.length&&G[idx][1]===i)lines.push("g "+G[idx++][0]);lines.push("f "+F[i].join(" "));}
 return {obj:lines.join("\n")+"\n",vertices:V.length,triangles:F.length,groups:G.map(g=>g[0])};
}
process.stdout.write(generate().obj);
