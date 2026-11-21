// Run: node validate.mjs (from this directory)
import fs from 'node:fs';
const s=fs.readFileSync(new URL('./VeilSealAmulet.obj',import.meta.url),'utf8');
let v=0,vt=0,tri=0,bad=0,degenerate=0;
for(const line of s.split(/\r?\n/)){
 if(line.startsWith('v '))v++;
 else if(line.startsWith('vt '))vt++;
 else if(line.startsWith('f ')){
  const a=line.slice(2).trim().split(/\s+/).map(x=>x.split('/').map(Number));
  if(a.length!==3 || a.some(([i,j])=>!(i>=1&&i<=v&&j>=1&&j<=vt)))bad++;
  if(new Set(a.map(x=>x[0])).size<3)degenerate++;
  tri++;
 }
}
console.log(JSON.stringify({vertices:v,uvs:vt,triangles:tri,invalid_indices:bad,duplicate_vertex_triangles:degenerate},null,2));
if(bad||degenerate||v<100||tri<100)process.exitCode=1;
