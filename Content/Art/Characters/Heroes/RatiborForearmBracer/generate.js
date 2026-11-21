function generate(){
let v=[],uv=[],f=[],groups=[];let mat="Leather";function vert(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length}
function tri(a,b,c){f.push([a,b,c,mat])}
function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
function ringSection(y0,y1,r0,r1,segments,offset=0){let rings=[];for(let j=0;j<2;j++){let row=[];for(let i=0;i<=segments;i++){let a=-1.24+2.48*i/segments,noise=.012*Math.sin(i*7.1+j*3.3);let r=(j?r1:r0)+noise;row.push(vert(Math.sin(a)*r,y0+(y1-y0)*j,Math.cos(a)*r+offset,i/segments,j));}rings.push(row)}for(let i=0;i<segments;i++)quad(rings[0][i],rings[0][i+1],rings[1][i+1],rings[1][i]);}
mat="Leather";ringSection(0,.36,.105,.142,32);ringSection(.36,.78,.142,.173,32);
mat="Iron";for(let y of [.04,.35,.72])ringSection(y,y+.045,.111+y*.078,.115+y*.078,32,.006);
function cylinder(cx,cy,cz,rad,depth,n){let front=[],back=[];for(let i=0;i<n;i++){let a=i*2*Math.PI/n;front.push(vert(cx+rad*Math.cos(a),cy+rad*Math.sin(a),cz+depth/2,i/n,0));back.push(vert(cx+rad*Math.cos(a),cy+rad*Math.sin(a),cz-depth/2,i/n,1))}for(let i=1;i<n-1;i++){tri(front[0],front[i],front[i+1]);tri(back[0],back[i+1],back[i])}for(let i=0;i<n;i++)quad(front[i],front[(i+1)%n],back[(i+1)%n],back[i])}
mat="Bronze";for(let y of [.08,.39,.74])for(let a of [-.95,-.55,0,.55,.95]){let radius=.115+y*.077;cylinder(Math.sin(a)*radius,y,Math.cos(a)*radius+.012,.010,.009,10)}
mat="Leather";for(let i=0;i<6;i++){let y=.15+i*.105;let x=.12+i*.003;let a=vert(-x,y,.075,0,0),b=vert(x,y+.07,.075,1,1),c=vert(x,y+.081,.075,1,0),d=vert(-x,y+.011,.075,0,1);quad(a,b,c,d)}
mat="Iron";for(let i=0;i<3;i++){let y=.19+i*.20;cylinder(0,y,.176,.024,.012,16)}
let out=["# Original Razlom Ratibor forearm bracer; meters; Y axis forearm","# open cuff intended for cloth attachment","mtllib SM_RatiborBracer_v01.mtl"];for(let p of v)out.push("v "+p.map(x=>x.toFixed(6)).join(" "));for(let p of uv)out.push("vt "+p.map(x=>x.toFixed(6)).join(" "));let current="";for(let t of f){if(t[3]!==current){current=t[3];out.push("usemtl "+current)}out.push("f "+t.slice(0,3).map(x=>x+"/"+x).join(" "))}return {obj:out.join("\n")+"\n",vertices:v.length,triangles:f.length,materials:[...new Set(f.map(x=>x[3]))]}}
const fs=require('fs');fs.writeFileSync('SM_RatiborBracer_v01.obj',generate().obj);
