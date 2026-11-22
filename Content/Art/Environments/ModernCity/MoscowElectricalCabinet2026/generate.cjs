// Original Razlom procedural Moscow electrical cabinet, units metres
const fs=require('fs');
const make=(lod)=>{
let verts=[],uv=[],faces=[],groups=[];let current='';
function box(name,x,y,z,w,d,h,mat){
 const v=[[x-w/2,y-d/2,z-h/2],[x+w/2,y-d/2,z-h/2],[x+w/2,y+d/2,z-h/2],[x-w/2,y+d/2,z-h/2],[x-w/2,y-d/2,z+h/2],[x+w/2,y-d/2,z+h/2],[x+w/2,y+d/2,z+h/2],[x-w/2,y+d/2,z+h/2]];
 const quads=[[0,1,2,3],[4,7,6,5],[0,4,5,1],[1,5,6,2],[2,6,7,3],[3,7,4,0]];
 groups.push({name,mat,start:faces.length});let off=verts.length;verts.push(...v);
 for(const q of quads)for(const t of [[q[0],q[1],q[2]],[q[0],q[2],q[3]]]){
 let idx=[];for(const k of t){let p=v[k];uv.push([((p[0]+p[2])*.37)%1,((p[1]+p[2])*.37)%1]);idx.push([off+k+1,uv.length]);}faces.push(idx);
 }
}
box('steel_housing',0,0,1.2,.82,.19,1.18,'steel');
box('painted_door',0,-.111,1.2,.77,.025,1.13,'paint');
box('hinge_upper',.4,-.125,1.74,.055,.045,.105,'steel');box('hinge_lower',.4,-.125,.66,.055,.045,.105,'steel');
box('meter_window',-.16,-.132,1.45,.32,.014,.22,'glass');
box('meter_display',-.16,-.145,1.45,.24,.008,.08,'screen');
box('warning_sign',.18,-.132,1.42,.19,.009,.19,'yellow');
box('door_latch',.27,-.151,1.08,.045,.025,.095,'steel');
if(lod===0){for(let i=0;i<3;i++)box('vent_'+i,-.16,-.135,.84+i*.035,.36,.014,.013,'dark');for(let i=0;i<6;i++)box('bolt_'+i,-.32+(i%3)*.32,-.14,i<3?.69:1.7,.023,.011,.023,'steel');}
let out=['# Original Razlom mesh, metres'];for(const v of verts)out.push('v '+v.join(' '));for(const t of uv)out.push('vt '+t.join(' '));
for(let i=0;i<groups.length;i++){let g=groups[i];out.push('o '+g.name,'usemtl '+g.mat);let end=i+1<groups.length?groups[i+1].start:faces.length;for(let j=g.start;j<end;j++)out.push('f '+faces[j].map(p=>p.join('/')).join(' '));}
return {obj:out.join('\n')+'\n',vertices:verts.length,triangles:faces.length};
};
if(require.main===module){for(const lod of [0,1])fs.writeFileSync(__dirname+'/cabinet_LOD'+lod+'.obj',make(lod).obj)}
module.exports=make;