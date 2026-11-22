// Original procedural VelegRad chest, metres, Z-up. Node.js >=18.
const fs=require('node:fs');
function build(lod){
 const v=[],uv=[],faces=[];let mat='oak';const seg=lod?8:16;
 function vert(p,t){v.push(p);uv.push(t);return v.length}
 function tri(a,b,c){faces.push([a,b,c,mat])}
 function quad(a,b,c,d){const i=[vert(a,[0,0]),vert(b,[1,0]),vert(c,[1,1]),vert(d,[0,1])];tri(i[0],i[1],i[2]);tri(i[0],i[2],i[3])}
 function box(cx,cy,cz,sx,sy,sz,m){mat=m;let x=cx-sx/2,X=cx+sx/2,y=cy-sy/2,Y=cy+sy/2,z=cz-sz/2,Z=cz+sz/2;
 quad([x,y,z],[X,y,z],[X,y,Z],[x,y,Z]);quad([X,Y,z],[x,Y,z],[x,Y,Z],[X,Y,Z]);quad([x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]);quad([X,y,z],[X,Y,z],[X,Y,Z],[X,y,Z]);quad([x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]);quad([x,y,z],[x,Y,z],[X,Y,z],[X,y,z])}
 function arc(x,width){mat='iron';for(let i=0;i<seg;i++){let a=Math.PI*i/seg,b=Math.PI*(i+1)/seg;let p=t=>[x,.36*Math.cos(t),1.02+.36*Math.sin(t)],q=t=>[x+width,.36*Math.cos(t),1.02+.36*Math.sin(t)];quad(p(a),q(a),q(b),p(b))}}
 box(0,0,.53,1.55,.82,.9,'oak');box(0,0,.13,1.62,.9,.12,'iron');box(0,0,1,1.6,.86,.1,'oak');
 for(let j=0;j<seg;j++){let a=Math.PI*j/seg,b=Math.PI*(j+1)/seg;mat='oak';let p=(t,x)=>[x,.41*Math.cos(t),1.02+.36*Math.sin(t)];quad(p(a,-.79),p(a,.79),p(b,.79),p(b,-.79))}
 for(let x of [-.58,.58]){box(x,0,.53,.075,.92,.91,'iron');arc(x-.042,.084)}
 box(0,-.46,.78,.23,.055,.32,'iron');box(0,-.51,.77,.1,.05,.15,'bronze');
 for(let x of [-.69,.69])for(let y of [-.46,.46])box(x,y,.56,.07,.05,.73,'iron');
 if(!lod){for(let x of [-.67,.67])for(let z of [.3,.54,.78])box(x,-.493,z,.06,.035,.06,'bronze');for(let x of [-.5,-.25,0,.25,.5])box(x,-.415,.55,.016,.018,.48,'carving')}
 let out=['# original mesh, metres, Z-up','mtllib materials.mtl'];
 for(let p of v)out.push('v '+p.map(n=>n.toFixed(6)).join(' '));
 for(let t of uv)out.push('vt '+t.map(n=>n.toFixed(6)).join(' '));
 let current='';for(let [a,b,c,m] of faces){if(m!==current){out.push('usemtl '+m);current=m}out.push('f '+[a,b,c].map(n=>n+'/'+n).join(' '))}
 return out.join('\n')+'\n';
}
for(let lod=0;lod<=1;lod++)fs.writeFileSync('LOD'+lod+'.obj',build(lod));
