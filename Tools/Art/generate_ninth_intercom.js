// Original geometry generator for Ninth Entrance intercom; no asset packs.
// Run: node Tools/Art/generate_ninth_intercom.js > SM_NinthEntrance_Intercom_v1.obj
const build=function build(){
const lines=["# RAZLOM Ninth Entrance intercom, original geometry; Z up, metres, front -Y","mtllib SM_NinthEntrance_Intercom_v1.mtl"];let nv=0,nt=0,tri=0,objects=0;
function emit(name,mat,vs,fs){lines.push("o "+name,"usemtl "+mat);for(const v of vs)lines.push("v "+v.map(x=>x.toFixed(6)).join(" "));for(const f of fs){const pts=f.map(i=>vs[i]);const uv=pts.map(p=>[(p[0]+.3)/.6,(p[2]+.3)/.6]);for(const q of uv)lines.push("vt "+q.map(x=>x.toFixed(6)).join(" "));lines.push("f "+f.map((i,k)=>`${nv+i+1}/${nt+k+1}`).join(" "));nt+=3;tri++}nv+=vs.length;objects++}
function box(name,c,d,mat){const [x,y,z]=c,[a,b,h]=d.map(t=>t/2);const v=[[-a,-b,-h],[a,-b,-h],[a,b,-h],[-a,b,-h],[-a,-b,h],[a,-b,h],[a,b,h],[-a,b,h]].map(p=>p.map((t,i)=>t+c[i]));const q=[[0,2,1],[0,3,2],[4,5,6],[4,6,7],[0,1,5],[0,5,4],[1,2,6],[1,6,5],[2,3,7],[2,7,6],[3,0,4],[3,4,7]];emit(name,mat,v,q)}
function cyl(name,c,r,d,mat,N=20){const v=[];for(let j=0;j<2;j++)for(let i=0;i<N;i++){const a=2*Math.PI*i/N;v.push([c[0]+r*Math.cos(a),c[1]+(j?d/2:-d/2),c[2]+r*Math.sin(a)])}const f=[];for(let i=1;i<N-1;i++){f.push([0,i+1,i],[N,N+i,N+i+1])}for(let i=0;i<N;i++){const k=(i+1)%N;f.push([i,k,N+k],[i,N+k,N+i])}emit(name,mat,v,f)}
function torus(name,c,R,r,mat,N=32,M=10){const v=[],f=[];for(let i=0;i<N;i++){let t=2*Math.PI*i/N;for(let j=0;j<M;j++){let a=2*Math.PI*j/M;v.push([c[0]+(R+r*Math.cos(a))*Math.cos(t),c[1]+r*Math.sin(a),c[2]+(R+r*Math.cos(a))*Math.sin(t)])}}for(let i=0;i<N;i++)for(let j=0;j<M;j++){let a=i*M+j,b=((i+1)%N)*M+j,d=i*M+(j+1)%M,e=((i+1)%N)*M+(j+1)%M;f.push([a,b,e],[a,e,d])}emit(name,mat,v,f)}
box("diecast_housing",[0,0,0],[.164,.045,.286],"enamel");box("rear_flange",[0,.024,0],[.174,.009,.298],"dark_steel");box("faceplate",[0,-.026,0],[.151,.003,.269],"brushed_steel");box("display_recess",[0,-.030,.089],[.125,.004,.050],"dark_steel");box("display_glass",[0,-.033,.089],[.111,.002,.034],"glass");box("speaker_recess",[0,-.030,.026],[.124,.003,.045],"dark_steel");
for(let row=0;row<4;row++)for(let col=0;col<10;col++)cyl(`speaker_port_${row}_${col}`,[-.051+col*.011,-.034,.010+row*.010],.0026,.0012,"shadow",12);
for(let row=0;row<3;row++)for(let col=0;col<3;col++){let x=(col-1)*.042,z=-.035-row*.029;box(`key_base_${row}_${col}`,[x,-.033,z],[.033,.007,.023],"dark_steel");box(`key_face_${row}_${col}`,[x,-.038,z],[.028,.002,.018],"brushed_steel");if(row===1&&col===1)cyl("key5_pip",[x,-.040,z],.0018,.001,"enamel",12)}
cyl("reader_black",[0,-.032,-.120],.014,.003,"shadow",40);torus("reader_bezel",[0,-.035,-.120],.014,.0023,"brass",40,10);torus("reader_inner",[0,-.036,-.120],.008,.0012,"brass",40,8);
for(const x of [-.066,.066])for(const z of [-.127,.127]){cyl("washer",[x,-.030,z],.0058,.002,"dark_steel");cyl("screw_head",[x,-.032,z],.0036,.002,"brushed_steel");box("screw_slot",[x,-.034,z],[.0047,.0005,.0008],"shadow")}
box("bottom_lip",[0,-.030,-.143],[.162,.011,.006],"dark_steel");for(const x of [-.078,.078])box("side_seam",[x,-.027,0],[.0015,.001,.27],"shadow");for(const x of [-.055,0,.055])cyl("hood_rivet",[x,-.028,.139],.0025,.002,"brass",12);
return {obj:lines.join("\n")+"\n",stats:{nv,nt,tri,objects}}
};
if(require.main===module){process.stdout.write(build().obj)}
module.exports={build};
