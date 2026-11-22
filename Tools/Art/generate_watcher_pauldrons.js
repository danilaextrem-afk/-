// Original procedural Watcher shoulder armor, meters, OBJ triangulated.
// Node.js: node Tools/Art/generate_watcher_pauldrons.js > Content/Art/Characters/Watcher/SM_Watcher_Pauldrons_v1.obj
const vertices=[], faces=[], groups=[];
let active="armor";
function group(name){active=name}
function v(x,y,z){vertices.push([x,y,z]);return vertices.length}
function tri(a,b,c){faces.push([active,a,b,c])}
function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
function strip(centerX,layer,segments=28,rows=9){
 const grid=[];
 for(let j=0;j<=rows;j++){
  const t=j/rows, ring=[];
  for(let i=0;i<=segments;i++){
   const theta=(-0.91+1.82*i/segments)*Math.PI;
   const shoulder=0.245+0.03*layer;
   const radius=0.17+0.045*Math.sin(Math.PI*t)+0.012*layer;
   const x=centerX+(centerX>0?1:-1)*(0.018+shoulder*t+radius*Math.sin(theta)*0.38);
   const y=radius*Math.cos(theta)+0.02*Math.sin(7*theta+layer);
   const z=1.91-0.24*t-0.028*layer+0.038*Math.cos(theta)+0.012*Math.sin(9*t+theta*3);
   ring.push(v(x,y,z));
  }
  grid.push(ring);
 }
 for(let j=0;j<rows;j++)for(let i=0;i<segments;i++)quad(grid[j][i],grid[j][i+1],grid[j+1][i+1],grid[j+1][i]);
}
function cylinder(cx,cy,cz,r,depth,n=12){
 const a=[],b=[];
 for(let i=0;i<n;i++){
  const q=i*2*Math.PI/n;
  a.push(v(cx+r*Math.cos(q),cy+r*Math.sin(q),cz));
  b.push(v(cx+r*Math.cos(q),cy+r*Math.sin(q),cz+depth));
 }
 const ac=v(cx,cy,cz),bc=v(cx,cy,cz+depth);
 for(let i=0;i<n;i++){const k=(i+1)%n;quad(a[i],a[k],b[k],b[i]);tri(ac,a[k],a[i]);tri(bc,b[i],b[k])}
}
function spike(x,y,z,s){
 const a=v(x-s,y-s,z),b=v(x+s,y-s,z),c=v(x+s,y+s,z),d=v(x-s,y+s,z),tip=v(x,y,z+s*2.5);
 tri(a,b,tip);tri(b,c,tip);tri(c,d,tip);tri(d,a,tip);quad(a,d,c,b);
}
for(const side of [-1,1]){
 const cx=side*0.20;
 for(let layer=0;layer<3;layer++){group("FORGED_LAYER_"+side+"_"+layer);strip(cx,layer)}
 group("RIVETS_"+side);
 for(let i=0;i<9;i++){
  const x=side*(0.25+0.033*i),z=1.89-0.025*i;
  cylinder(x,0.12,z,0.009,0.012,10);
 }
 group("NINE_THORNS_"+side);
 for(let i=0;i<9;i++){
  const x=side*(0.22+0.033*i);
  spike(x,0.105,1.925-0.025*i,0.009);
 }
}
let output="# RAZLOM original Watcher layered shoulder armor v1; meters; +Z up; unrigged draft\n";
let prev="";
for(const p of vertices)output+="v "+p.map(x=>x.toFixed(6)).join(" ")+"\n";
for(const [g,a,b,c] of faces){if(g!==prev){output+="g "+g+"\n";prev=g}output+="f "+a+" "+b+" "+c+"\n"}
process.stdout.write(output);
