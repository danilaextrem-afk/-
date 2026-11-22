'use strict';
// Original Watcher hip tassets: 8 articulated-looking overlapping plates, rim studs and central seal.
// Units metres, Y-up, geometry-only WIP; deterministic, no external assets.
const fs=require('fs');
function build(){
let v=[],uv=[],f=[],groups=[],g='';const pi=Math.PI;
function group(n){g=n;groups.push(n)}
function vertex(x,y,z,u,w){v.push([x,y,z]);uv.push([u,w]);return v.length}
function tri(a,b,c){f.push([a,b,c,g])}
function quad(a,b,c,d){tri(a,b,c);tri(a,c,d)}
function box(x,y,z,sx,sy,sz){const pts=[[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]].map(([a,b,c])=>vertex(x+a*sx/2,y+b*sy/2,z+c*sz/2,(a+1)/2,(b+1)/2));for(const [a,b,c,d] of [[0,3,2,1],[4,5,6,7],[0,4,7,3],[1,2,6,5],[0,1,5,4],[3,7,6,2]])quad(pts[a],pts[b],pts[c],pts[d])}
function plate(theta,j){group('tasset_'+j);const rings=5,cols=9;let grid=[];for(let r=0;r<=rings;r++){let row=[];for(let c=0;c<=cols;c++){const u=c/cols,t=r/rings;const a=theta+(u-.5)*.39;const y=.12-t*.28-.035*Math.sin(t*pi);const rad=.32+.06*t;const x=rad*Math.sin(a),z=rad*Math.cos(a);row.push(vertex(x,y,z,u,t))}grid.push(row)}for(let r=0;r<rings;r++)for(let c=0;c<cols;c++)quad(grid[r][c],grid[r][c+1],grid[r+1][c+1],grid[r+1][c]); // stamped edges
for(let r of [0,rings])for(let c=0;c<cols;c++){const a=grid[r][c],b=grid[r][c+1],va=v[a-1],vb=v[b-1],d=vertex(va[0]*.987,va[1]-.012,va[2]*.987,c/cols,r/rings),e=vertex(vb[0]*.987,vb[1]-.012,vb[2]*.987,(c+1)/cols,r/rings);quad(a,b,e,d)}}
function rivet(x,y,z,r){const seg=12;let top=vertex(x,y+.006,z,0.5,0.5),rim=[];for(let k=0;k<seg;k++){let a=2*pi*k/seg;rim.push(vertex(x+r*Math.cos(a),y,z+r*Math.sin(a),.5+.5*Math.cos(a),.5+.5*Math.sin(a)))}for(let k=0;k<seg;k++)tri(top,rim[k],rim[(k+1)%seg])}
for(let j=0;j<8;j++){const a=j*2*pi/8;plate(a,j);group('rivet_pair_'+j);for(const d of [-.11,.11]){const t=a+d;rivet(.326*Math.sin(t),.135,.326*Math.cos(t),.013)}}
group('belt');for(let j=0;j<32;j++){let a=2*pi*j/32,b=2*pi*(j+1)/32;let p=[vertex(.315*Math.sin(a),.15,.315*Math.cos(a),j/32,0),vertex(.315*Math.sin(b),.15,.315*Math.cos(b),(j+1)/32,0),vertex(.315*Math.sin(b),.21,.315*Math.cos(b),(j+1)/32,1),vertex(.315*Math.sin(a),.21,.315*Math.cos(a),j/32,1)];quad(...p)}
group('central_seal');box(0,.18,.32,.11,.09,.035);group('seal_spine');box(0,.12,.342,.025,.15,.014);
let s=['# Original Watcher segmented hip tassets, Y up, metres','mtllib SM_Watcher_HipTassets_v1.mtl'];for(const p of v)s.push('v '+p.map(x=>x.toFixed(6)).join(' '));for(const p of uv)s.push('vt '+p.map(x=>x.toFixed(6)).join(' '));let old='';for(const [a,b,c,name] of f){if(name!==old){s.push('g '+name);old=name}s.push('f '+a+'/'+a+' '+b+'/'+b+' '+c+'/'+c)}return {obj:s.join('\n')+'\n',verts:v.length,faces:f.length,groups:groups.length};
}
if(require.main===module){const r=build();fs.writeFileSync(process.argv[2]||'SM_Watcher_HipTassets_v1.obj',r.obj);console.log(r.verts,r.faces,r.groups)}
module.exports={build};
