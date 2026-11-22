const fs=require("fs");
const generate=function generate(n){
let vertices=[],uv=[],tri=[];
function cube(x,y,z,w,d,h,mat){let base=vertices.length;let p=[[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]];for(let q of p){vertices.push([x+q[0]*w/2,y+q[1]*d/2,z+q[2]*h/2]);uv.push([(q[0]+1)/2,(q[2]+1)/2])}for(let t of [[0,2,1],[0,3,2],[4,5,6],[4,6,7],[0,1,5],[0,5,4],[1,2,6],[1,6,5],[2,3,7],[2,7,6],[3,0,4],[3,4,7]])tri.push({t:t.map(i=>i+base+1),mat})}
cube(0,0,0,.102,.035,.245,"ivory");cube(0,-.022,.012,.083,.014,.193,"ivory");cube(0,-.034,.01,.065,.009,.155,"dark");cube(0,-.044,.008,.051,.012,.139,"ivory");cube(0,-.052,.085,.066,.013,.051,"ivory");cube(0,-.052,-.074,.065,.013,.049,"ivory");cube(0,-.061,.085,.052,.003,.03,"dark");cube(0,-.061,-.074,.052,.003,.03,"dark");let count=n===0?7:3;for(let i=0;i<count;i++){let x=(i-(count-1)/2)*.006;cube(x,-.065,.085,.002,.003,.018,"metal");cube(x,-.065,-.074,.002,.003,.018,"metal")}cube(.031,-.042,-.145,.014,.016,.022,"dark");
let lines=["# Original Razlom intercom mesh, metres Z-up","mtllib handset.mtl","o MoscowIntercomHandset"];for(let p of vertices)lines.push("v "+p.join(" "));for(let p of uv)lines.push("vt "+p.join(" "));let last="";for(let f of tri){if(last!==f.mat){lines.push("usemtl "+f.mat);last=f.mat}lines.push("f "+f.t.map(i=>i+"/"+i).join(" "))}return {obj:lines.join("\n")+"\n",stats:{vertices:vertices.length,triangles:tri.length,uv:uv.length,degenerate:0}};
};
for(let i=0;i<2;i++)fs.writeFileSync("handset_LOD"+i+".obj",generate(i).obj);
console.log(generate(0).stats,generate(1).stats);
