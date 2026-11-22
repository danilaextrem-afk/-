// Original ninefold flail head for Watcher, metre units, Y up. Node >=18.
// Run: node generate.js > SM_Watcher_NinefoldFlailHead_v1.obj
const v=[],uv=[],f=[];let mat="forged_iron";
function vert(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length}
function tri(a,b,c,m=mat){f.push([a,b,c,m])}
function ring(y,r,n=18,phase=0,m="forged_iron"){let ids=[];for(let i=0;i<n;i++){let t=2*Math.PI*i/n+phase;ids.push(vert(r*Math.cos(t),y,r*Math.sin(t),i/n,(y+.2)/.4))}return ids}
function bridge(a,b,m){for(let i=0;i<a.length;i++){let j=(i+1)%a.length;tri(a[i],b[i],b[j],m);tri(a[i],b[j],a[j],m)}}
function cone(x,y,z,r,h,n=10,m="dark_steel"){let base=[];for(let i=0;i<n;i++){let t=2*Math.PI*i/n;base.push(vert(x+r*Math.cos(t),y,z+r*Math.sin(t),i/n,0))}let tip=vert(x,y+h,z,.5,1);for(let i=0;i<n;i++)tri(base[i],base[(i+1)%n],tip,m);let c=vert(x,y,z,.5,.5);for(let i=0;i<n;i++)tri(c,base[(i+1)%n],base[i],m)}
// Hollow segmented head, forged bands and nine spikes.
const levels=[[-.14,.056],[-.115,.091],[-.075,.104],[0,.112],[.075,.104],[.115,.091],[.14,.056]];
let rings=levels.map(([y,r],i)=>ring(y,r,36,i%2*Math.PI/36));
for(let i=0;i<rings.length-1;i++)bridge(rings[i],rings[i+1],i===0||i===5?"dark_steel":"forged_iron");
let bot=vert(0,-.14,0,.5,0),top=vert(0,.14,0,.5,1);
for(let i=0;i<36;i++){tri(bot,rings[0][(i+1)%36],rings[0][i]);tri(top,rings[6][i],rings[6][(i+1)%36])}
// Nine distinct radial teeth with tapered triangular facets.
for(let k=0;k<9;k++){let t=k*2*Math.PI/9,dx=Math.cos(t),dz=Math.sin(t),px=-dz,pz=dx;
let a=vert(.102*dx+.018*px,0,.102*dz+.018*pz,0,0);
let b=vert(.102*dx-.018*px,0,.102*dz-.018*pz,1,0);
let c=vert(.102*dx,.039,.102*dz,.5,.4);
let d=vert(.102*dx,-.039,.102*dz,.5,.4);
let tip=vert(.193*dx,.008,.193*dz,.5,1);
for(const [i,j] of [[a,c],[c,b],[b,d],[d,a]])tri(i,j,tip,"dark_steel");
tri(a,d,c,"dark_steel");tri(b,c,d,"dark_steel");
}
// Suspension lug and bored visual opening, separate metal ring.
for(let q=0;q<2;q++){let rr=q===0?.031:.021, ids=[];for(let i=0;i<24;i++){let t=2*Math.PI*i/24;ids.push(vert(rr*Math.cos(t),.165+rr*Math.sin(t),0,i/24,q))}if(q)bridge(outer,ids,"bronze");else var outer=ids}
let backOuter=outer.map(i=>vert(v[i-1][0],v[i-1][1],.014,uv[i-1][0],uv[i-1][1]));
let backInner=[];for(let i=0;i<24;i++){let t=2*Math.PI*i/24;backInner.push(vert(.021*Math.cos(t),.165+.021*Math.sin(t),.014,i/24,1))}
bridge(backOuter,backInner,"bronze");bridge(outer,backOuter,"bronze");
const mats=["forged_iron","dark_steel","bronze"];
console.log("# Original Watcher Ninefold Flail Head v1; metres; Y up; WIP geometry");
console.log("mtllib SM_Watcher_NinefoldFlailHead_v1.mtl");
for(const p of v)console.log("v "+p.map(n=>n.toFixed(6)).join(" "));
for(const p of uv)console.log("vt "+p.map(n=>n.toFixed(6)).join(" "));
let last="";for(const [a,b,c,m] of f){if(m!==last){console.log("usemtl "+m);last=m}console.log("f "+[a,b,c].map(i=>i+"/"+i).join(" "))}
