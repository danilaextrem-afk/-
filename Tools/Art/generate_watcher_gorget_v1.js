// Original procedural neck gorget for Ninth Entrance Watcher. Units: metres; Y forward, Z up.
// node Tools/Art/generate_watcher_gorget_v1.js > Content/Art/Characters/Watcher/SM_Watcher_Gorget_v1.obj
const V=[], F=[];let group='';
function vertex(x,y,z){V.push([x,y,z]);return V.length}
function face(a,b,c){F.push([group,a,b,c])}
function ring(name,z,rx,ry,segments=24,offset=0){group=name;let a=[];for(let i=0;i<segments;i++){let t=2*Math.PI*i/segments+offset;a.push(vertex(rx*Math.cos(t),ry*Math.sin(t),z))}return a}
function bridge(a,b){for(let i=0;i<a.length;i++){let j=(i+1)%a.length;face(a[i],a[j],b[j]);face(a[i],b[j],b[i])}}
function band(name,z0,z1,rx0,ry0,rx1,ry1){let a=ring(name,z0,rx0,ry0),b=ring(name,z1,rx1,ry1);bridge(a,b)}
function spike(name,x,y,z,h,r){group=name;let base=[];for(let i=0;i<8;i++){let t=i*Math.PI/4;base.push(vertex(x+r*Math.cos(t),y+r*Math.sin(t),z))}let tip=vertex(x,y,z+h);for(let i=0;i<8;i++)face(base[i],base[(i+1)%8],tip)}
function rivet(name,x,y,z,r){group=name;let a=[];for(let i=0;i<8;i++){let t=i*Math.PI/4;a.push(vertex(x+r*Math.cos(t),y+r*Math.sin(t),z))}let p=vertex(x,y,z+r*.65);for(let i=0;i<8;i++)face(a[i],a[(i+1)%8],p)}
// layered flared collar: neck aperture is intentionally open
band('01_inner_black_iron',1.61,1.79,.135,.115,.155,.13);
band('02_lower_flared_plate',1.56,1.65,.25,.205,.19,.165);
band('03_upper_beveled_guard',1.77,1.85,.155,.13,.175,.148);
for(let j=0;j<9;j++){let t=2*Math.PI*j/9;let x=.205*Math.cos(t),y=.18*Math.sin(t);
  spike('04_nine_oath_spikes_'+j,x,y,1.63,.085,.023);
  rivet('05_rivets_'+j,.232*Math.cos(t),.197*Math.sin(t),1.58,.012);
}
for(let j=0;j<3;j++)band('06_chest_lamella_'+j,1.47+j*.037,1.492+j*.037,.235-j*.008,.185-j*.006,.22-j*.008,.178-j*.006);
console.log('# RAZLOM original Watcher gorget v1; metres, Z-up; open neck aperture');
for(const v of V)console.log('v '+v.map(n=>n.toFixed(6)).join(' '));
let last='';for(const f of F){if(f[0]!==last){console.log('g '+f[0]);last=f[0]}console.log('f '+f.slice(1).join(' '))}
