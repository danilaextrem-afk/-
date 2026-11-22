// Reproducible mesh generator. Run: node generate.js
const fs=require('node:fs');
const build=function build(lod=0){
let V=[],T=[],F=[],M=[];function cube(x,y,z,a,b,c,mat){const p=[[x-a/2,y-b/2,z-c/2],[x+a/2,y-b/2,z-c/2],[x+a/2,y+b/2,z-c/2],[x-a/2,y+b/2,z-c/2],[x-a/2,y-b/2,z+c/2],[x+a/2,y-b/2,z+c/2],[x+a/2,y+b/2,z+c/2],[x-a/2,y+b/2,z+c/2]],s=[[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]];for(const q of s){const start=V.length;for(let j=0;j<4;j++){V.push(p[q[j]]);T.push([[0,0],[1,0],[1,1],[0,1]][j]);}F.push([start+1,start+2,start+3],[start+1,start+3,start+4]);M.push(mat,mat)}}
for(const x of [-1.45,1.45])for(const y of [-.42,.42]){cube(x,y,1.18,.075,.075,2.36,"steel");cube(x,y,.07,.25,.25,.14,"concrete")}
cube(0,0,2.42,3.2,1.18,.10,"steel");cube(0,0,2.49,3.24,1.22,.035,"enamel");cube(0,.43,1.24,2.85,.025,2.15,"glass");
if(lod===0){for(const x of [-.95,0,.95])cube(x,.43,1.23,.035,.055,2.15,"steel");for(const x of [-1.05,1.05])cube(x,-.12,.58,.06,.65,1.04,"steel");for(const z of [.52,.91])cube(0,-.14,z,2.3,.37,.055,"wood");cube(0,-.13,.97,2.3,.06,.22,"wood");cube(-1.1,-.48,1.83,.68,.04,.54,"enamel");cube(1.13,-.48,1.75,.38,.05,.6,"steel");for(let i=0;i<8;i++)cube(-1.36+i*.39,0,2.36,.022,1.16,.018,"steel");}
else{cube(0,-.14,.55,2.3,.37,.07,"wood");cube(0,-.13,.96,2.3,.06,.22,"wood");}
let lines=["# Original bus shelter 2026. Metres, Z up, X right.","mtllib shelter.mtl",`o RZ_MODERN_BUS_SHELTER_LOD${lod}`];for(const p of V)lines.push("v "+p.map(n=>n.toFixed(5)).join(" "));for(const t of T)lines.push("vt "+t.join(" "));let last="";for(let i=0;i<F.length;i++){if(M[i]!==last){last=M[i];lines.push("usemtl "+last)}lines.push("f "+F[i].map(n=>`${n}/${n}`).join(" "))}return {obj:lines.join("\n")+"\n",vertices:V.length,triangles:F.length,valid:F.every(t=>t.every(n=>n>0&&n<=V.length)),uv:T.every(t=>t.every(n=>n>=0&&n<=1))};};
for(const lod of [0,1])fs.writeFileSync(`shelter_LOD${lod}.obj`,build(lod).obj);
