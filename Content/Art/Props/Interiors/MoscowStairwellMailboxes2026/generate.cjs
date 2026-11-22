function generate(lod){
 let v=[],vt=[],vn=[],faces=[],groups={};const mat=["paint","steel","paper","dark"];let active="paint";
 function tri(a,b,c){faces.push(`f ${a} ${b} ${c}`)}
 function quad(p,n,material){let start=v.length+1;for(let i=0;i<4;i++){v.push(p[i]);vt.push([[0,0],[1,0],[1,1],[0,1]][i]);vn.push(n)}faces.push(`usemtl ${material}`);tri(`${start}/${start}/${start}`,`${start+1}/${start+1}/${start+1}`,`${start+2}/${start+2}/${start+2}`);tri(`${start}/${start}/${start}`,`${start+2}/${start+2}/${start+2}`,`${start+3}/${start+3}/${start+3}`)}
 function box(x,y,z,w,h,d,m){let a=x-w/2,b=x+w/2,c=y-d/2,e=y+d/2,f=z,g=z+h;quad([[a,c,f],[b,c,f],[b,e,f],[a,e,f]],[0,0,1],m);quad([[b,c,g],[a,c,g],[a,e,g],[b,e,g]],[0,0,-1],m);quad([[a,c,g],[b,c,g],[b,c,f],[a,c,f]],[0,-1,0],m);quad([[b,e,g],[a,e,g],[a,e,f],[b,e,f]],[0,1,0],m);quad([[a,e,g],[a,c,g],[a,c,f],[a,e,f]],[-1,0,0],m);quad([[b,c,g],[b,e,g],[b,e,f],[b,c,f]],[1,0,0],m)}
 // X horizontal, Y depth, Z up; units meters
 box(0,0,0,1.14,.72,.18,"steel");
 for(let r=0;r<2;r++)for(let c=0;c<3;c++){
 const x=-.365+c*.365,z=.045+r*.345;
 box(x,-.102,z,.34,.315,.014,"paint");
 box(x,-.115,z+.235,.245,.042,.006,"paper");
 box(x+.12,-.121,z+.09,.018,.04,.006,"dark");
 if(lod===0){box(x,-.124,z+.03,.23,.012,.008,"steel");box(x-.12,-.123,z+.09,.012,.035,.012,"steel");box(x+.115,-.122,z+.245,.018,.012,.009,"steel")}
 }
 if(lod===0){for(let i=0;i<4;i++){let x=-.51+i*.34;box(x,-.01,.708,.016,.012,.19,"steel")}}
 return {obj:`# Razlom original procedural mesh; meters, Z up\nmtllib mailboxes.mtl\no MoscowStairwellMailboxes_LOD${lod}\n`+v.map(a=>"v "+a.join(" ")).join("\n")+"\n"+vt.map(a=>"vt "+a.join(" ")).join("\n")+"\n"+vn.map(a=>"vn "+a.join(" ")).join("\n")+"\n"+faces.join("\n")+"\n",verts:v.length,triangles:faces.filter(x=>x.startsWith("f ")).length,valid:v.every(a=>a.every(Number.isFinite))&&faces.filter(x=>x.startsWith("f ")).length===v.length/4*2};
}
const fs=require('fs');for(const lod of [0,1])fs.writeFileSync('mailboxes_LOD'+lod+'.obj',generate(lod).obj);
