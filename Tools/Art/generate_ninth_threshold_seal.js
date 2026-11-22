'use strict';
// Original procedural environment prop for RAZLOM. Units metres, Z up, seed-free deterministic.
const fs=require('node:fs');
function build(){
 const v=[],vt=[],f=[],groups={};let active='stone',mat='StoneDark';
 const add=(p,uv)=>{v.push(p);vt.push(uv||[(p[0]+1)/2,(p[1]+1)/2]);return v.length};
 const tri=(a,b,c)=>{(groups[active]??=[]).push([a,b,c,mat])};
 const group=(name,m)=>{active=name;mat=m};
 function strip(points,width,z,depth=0.008){ // extruded bevelled ribbon along 2D polyline
  for(let k=0;k<points.length-1;k++){const [x,y]=points[k], [X,Y]=points[k+1],dx=X-x,dy=Y-y,len=Math.hypot(dx,dy);if(len<1e-7)continue;
   const nx=-dy/len*width/2,ny=dx/len*width/2;
   const q=[[x+nx,y+ny,z],[x-nx,y-ny,z],[X-nx,Y-ny,z],[X+nx,Y+ny,z]];
   const top=q.map(p=>add(p)),bot=q.map(p=>add([p[0],p[1],z-depth]));
   tri(top[0],top[1],top[2]);tri(top[0],top[2],top[3]);
   for(let j=0;j<4;j++){const h=(j+1)%4;tri(top[j],bot[j],bot[h]);tri(top[j],bot[h],top[h]);}
  }
 }
 function annulus(r0,r1,z0,z1,n=144,start=0,end=2*Math.PI){
  const rings=[];for(const [r,z] of [[r0,z0],[r1,z0],[r1,z1],[r0,z1]]){
   const arr=[];for(let i=0;i<=n;i++){const a=start+(end-start)*i/n;arr.push(add([r*Math.cos(a),r*Math.sin(a),z],[i/n,r]));}rings.push(arr)}
  for(let k=0;k<4;k++){const a=rings[k],b=rings[(k+1)%4];for(let i=0;i<n;i++){tri(a[i],b[i],b[i+1]);tri(a[i],b[i+1],a[i+1])}}
 }
 function prism(cx,cy,r,h,n=12,rotation=0){const a=[],b=[];for(let i=0;i<n;i++){const t=rotation+2*Math.PI*i/n;const x=cx+r*Math.cos(t),y=cy+r*Math.sin(t);a.push(add([x,y,h]));b.push(add([x,y,h-0.022]))}
  const c=add([cx,cy,h]);for(let i=0;i<n;i++){const j=(i+1)%n;tri(c,a[i],a[j]);tri(a[i],b[i],b[j]);tri(a[i],b[j],a[j])}}
 group('basalt_floor','Basalt');
 annulus(0.19,0.87,-0.025,0.008,192);
 annulus(0.01,0.19,-0.04,0.015,72);
 group('inner_bronze_bead','OxidizedBronze');annulus(0.255,0.272,0.014,0.026,192);annulus(0.66,0.677,0.012,0.027,192);annulus(0.812,0.838,0.012,0.031,192);
 group('nine_divided_outer_arcs','OxidizedBronze');
 for(let i=0;i<9;i++){let a=i*2*Math.PI/9+0.045,b=(i+1)*2*Math.PI/9-0.045;annulus(0.717,0.776,0.012,0.037,18,a,b)}
 group('engraved_radial_spokes','PaleInlay');
 for(let i=0;i<9;i++){const a=i*2*Math.PI/9;const radial=t=>[t*Math.cos(a),t*Math.sin(a)];strip([radial(0.32),radial(0.60)],0.013,0.036,0.006);
  const cx=0.57*Math.cos(a),cy=0.57*Math.sin(a);
  const side=(angle,r)=>[cx+r*Math.cos(angle),cy+r*Math.sin(angle)];
  strip([side(a+2.3,0.052),side(a,0.072),side(a-2.3,0.052)],0.014,0.039);
 }
 group('nine_glyphs','PaleInlay');
 for(let i=0;i<9;i++){const a=(i+0.5)*2*Math.PI/9,rad=0.45, cx=rad*Math.cos(a),cy=rad*Math.sin(a);
  const local=(u,w)=>[cx+u*Math.cos(a)-w*Math.sin(a),cy+u*Math.sin(a)+w*Math.cos(a)];
  strip([local(-0.065,-0.025),local(0.07,0.025)],0.012,0.036);
  strip([local(-0.045,0.045),local(0.025,-0.048)],0.011,0.036);
  strip([local(0.02,-0.047),local(0.07,0.045)],0.011,0.036);
 }
 group('central_nine_point_sigil','Silver');
 for(let i=0;i<9;i++){const a=i*2*Math.PI/9,b=(i+1)*2*Math.PI/9;strip([[0.13*Math.cos(a),0.13*Math.sin(a)],[0.225*Math.cos(a+0.15),0.225*Math.sin(a+0.15)],[0.13*Math.cos(b),0.13*Math.sin(b)]],0.017,0.041)}
 group('anchor_bosses','BlackIron');
 for(let i=0;i<9;i++){const a=i*2*Math.PI/9;prism(0.743*Math.cos(a),0.743*Math.sin(a),0.036,0.059,12,a)}
 group('fracture_fill','VoidBlack');
 const cracks=[[[.18,.21],[.29,.25],[.38,.22],[.49,.28],[.59,.26]], [[-.21,-.22],[-.30,-.29],[-.42,-.31],[-.54,-.39]],[[.18,-.23],[.28,-.36],[.35,-.38],[.42,-.52]], [[-.17,.29],[-.27,.38],[-.29,.52],[-.37,.60]]];
 for(const line of cracks)strip(line,0.012,0.013,0.012);
 group('fracture_edge_highlights','StoneHighlight');
 for(const line of cracks)strip(line.map(([x,y])=>[x+0.011,y+0.009]),0.003,0.016,0.002);
 const lines=['# RAZLOM original Ninth Entrance Threshold Seal v1 — source mesh, Z up','mtllib SM_NinthThresholdSeal_v1.mtl'];
 for(let i=0;i<v.length;i++){lines.push('v '+v[i].map(n=>n.toFixed(6)).join(' '))}
 for(const uv of vt)lines.push('vt '+uv.map(n=>n.toFixed(6)).join(' '));
 let count=0;
 for(const [name,faces] of Object.entries(groups)){lines.push('g '+name);let prev='';for(const [a,b,c,m] of faces){if(m!==prev){lines.push('usemtl '+m);prev=m}lines.push('f '+a+'/'+a+' '+b+'/'+b+' '+c+'/'+c);count++}}
 return {obj:lines.join('\n')+'\n',stats:{vertices:v.length,uv:vt.length,triangles:count,groups:Object.keys(groups).length},positions:v,faces:f};
}
if(require.main===module){const o=build();fs.writeFileSync(process.argv[2]||'SM_NinthThresholdSeal_v1.obj',o.obj);console.log(o.stats)}
module.exports={build};
