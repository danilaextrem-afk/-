import {writeFileSync} from 'node:fs';
function make(lod){
const N=lod?8:16,V=[],F=[];let uv=[];
function v(x,y,z,u=0,w=0){V.push([x,y,z]);uv.push([u,w]);return V.length}
function q(a,b,c,d,m){F.push([a,b,c,m],[a,c,d,m])}
function box(x,y,z,X,Y,Z,m){let p=[[0,0,0],[1,0,0],[1,1,0],[0,1,0],[0,0,1],[1,0,1],[1,1,1],[0,1,1]].map(([a,b,c])=>v(x+(a-.5)*X,y+(b-.5)*Y,z+(c-.5)*Z,a,b));for(let f of [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]])q(...f.map(i=>p[i]),m)}
function cyl(x0,x1,y,z,r,m){let p=[];for(let e=0;e<2;e++)for(let i=0;i<N;i++){let t=2*Math.PI*i/N;p.push(v(e?x1:x0,y+r*Math.cos(t),z+r*Math.sin(t),i/N,e))}for(let i=0;i<N;i++){let j=(i+1)%N;q(p[i],p[j],p[N+j],p[N+i],m)}}
for(let x of [-.88,.88]){box(x,0,.95,.18,.22,1.9,'oak');box(x,0,.10,.42,.42,.20,'oak');box(x,0,1.70,.30,.30,.14,'iron')}
cyl(-1.02,1.02,0,1.38,.145,'oak');
for(let x of [-.54,.54]){cyl(x-.055,x+.055,0,1.38,.29,'oak');cyl(x-.075,x-.045,0,1.38,.185,'iron')}
cyl(1.02,1.12,0,1.38,.085,'iron');box(1.13,.23,1.38,.085,.48,.09,'iron');cyl(1.09,1.36,.46,1.38,.065,'oak');
let coils=lod?6:14;for(let k=0;k<coils;k++){let x=-.43+k*.86/(coils-1);cyl(x-.011,x+.011,0,1.38,.169,'hemp')}
box(-.40,-.18,.80,.035,.035,.96,'hemp');
let s='mtllib winch.mtl\n# original well winch, Z up\n';for(let p of V)s+='v '+p.map(n=>n.toFixed(5)).join(' ')+'\n';for(let t of uv)s+='vt '+t.map(n=>n.toFixed(5)).join(' ')+'\n';let last='';for(let f of F){if(f[3]!==last){last=f[3];s+='usemtl '+last+'\n'}s+='f '+f.slice(0,3).map(n=>n+'/'+n).join(' ')+'\n'}return s;
}
writeFileSync('winch_LOD0.obj',make(false));writeFileSync('winch_LOD1.obj',make(true));
console.log('Generated LOD0 and LOD1');
