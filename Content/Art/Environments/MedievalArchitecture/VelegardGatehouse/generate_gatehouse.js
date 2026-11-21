// Deterministic authored original mesh generator; Node.js 18+. Run: node generate_gatehouse.js
const fs=require("fs");
function buildGatehouse(lod=0){
let v=[],uv=[],faces=[],groups=[];let material="timber";
function vert(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length}
function face(points,mat=material){let ids=points.map(p=>vert(...p));faces.push({ids,mat});}
function quad(a,b,c,d,mat=material){face([a,b,c],mat);face([a,c,d],mat)}
function box(cx,cy,cz,sx,sy,sz,mat=material){let x=cx-sx/2,X=cx+sx/2,y=cy-sy/2,Y=cy+sy/2,z=cz-sz/2,Z=cz+sz/2;let p=(a,b,c,u,w)=>[a,b,c,u,w];quad(p(x,y,z,0,0),p(X,y,z,1,0),p(X,y,Z,1,1),p(x,y,Z,0,1),mat);quad(p(X,Y,z,0,0),p(x,Y,z,1,0),p(x,Y,Z,1,1),p(X,Y,Z,0,1),mat);quad(p(x,Y,z,0,0),p(x,y,z,1,0),p(x,y,Z,1,1),p(x,Y,Z,0,1),mat);quad(p(X,y,z,0,0),p(X,Y,z,1,0),p(X,Y,Z,1,1),p(X,y,Z,0,1),mat);quad(p(x,y,Z,0,0),p(X,y,Z,1,0),p(X,Y,Z,1,1),p(x,Y,Z,0,1),mat);quad(p(x,Y,z,0,0),p(X,Y,z,1,0),p(X,y,z,1,1),p(x,y,z,0,1),mat)}
function roof(){const z=550,peak=760;for(let side of [-1,1]){const y1=side*325,y2=0;quad([-430,y1,z,0,0],[430,y1,z,1,0],[430,y2,peak,1,1],[-430,y2,peak,0,1],"shingle");}face([[-430,-325,z,0,0],[-430,325,z,1,0],[-430,0,peak,0.5,1]],"timber");face([[430,325,z,0,0],[430,-325,z,1,0],[430,0,peak,0.5,1]],"timber")}
for(let side of [-1,1])for(let ix=-1;ix<=1;ix++)box(ix*280,side*250,45,250,210,90,"stone");
for(let side of [-1,1])for(let ix of [-1,1])box(ix*350,side*225,285,48,48,480,"timber");
for(let side of [-1,1])for(let z of [150,300,455])box(0,side*225,z,760,35,28,"timber");
for(let ix of [-1,1])for(let z of [160,330])box(ix*350,0,z,35,510,30,"timber");
box(0,0,495,740,520,35,"timber");roof();
for(let side of [-1,1]){for(let ix=-3;ix<=3;ix++)box(ix*102,side*255,530,25,26,95,"timber");}
if(lod===0){
for(let side of [-1,1])for(let ix=-3;ix<=3;ix++)for(let row=0;row<2;row++)box(ix*110,side*285,140+row*130,95,18,100,"oak");
for(let side of [-1,1])for(let i=-3;i<=3;i++){box(i*108,side*228,585,9,18,85,"iron");}
for(let side of [-1,1])for(let ix of [-1,1])for(let z of [130,300,465])box(ix*350,side*235,z,65,60,14,"iron");
for(let ix of [-1,1])for(let step=0;step<6;step++)box(ix*320,(-1+step*.4)*200,65+step*36,110,65,35,"stone");
for(let side of [-1,1])for(let i=-3;i<=3;i++){box(i*110,side*245,470,100,18,12,"iron");}
for(let i=-3;i<=3;i++)for(let side of [-1,1])box(i*105,side*145,567,80,90,15,"shingle");
}
let out=["# RAZLOM Velegard gatehouse original procedural mesh, centimeters, Z up",`# LOD${lod}, source: generate_gatehouse.js`,"mtllib gatehouse.mtl","o SM_VelegardGatehouse_1348_LOD"+lod];
for(let a of v)out.push("v "+a.map(x=>x.toFixed(4)).join(" "));
for(let a of uv)out.push("vt "+a.map(x=>x.toFixed(6)).join(" "));
let last="";for(let f of faces){if(f.mat!==last){out.push("usemtl "+f.mat);last=f.mat}out.push("f "+f.ids.map(i=>`${i}/${i}`).join(" "))}
return {text:out.join("\n")+"\n",vertices:v.length,triangles:faces.length,materials:[...new Set(faces.map(f=>f.mat))]};
}
for(const lod of [0,1]) fs.writeFileSync("SM_VelegardGatehouse_1348_LOD"+lod+".obj",buildGatehouse(lod).text);
