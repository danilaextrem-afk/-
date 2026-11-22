'use strict';
// Deterministic OBJ assembly. Inputs must be checked out from the same art branch.
const fs = require('node:fs');
const path = require('node:path');
const root = process.argv[2] || '.';
const parts = [
  ['chain','Content/Art/Characters/WatcherFlailChain/SM_Watcher_FlailChain_v1.obj',0,false],
  ['swivel','Content/Art/Characters/WatcherFlailSwivel/SM_Watcher_Flail_Swivel_v1.obj',1.55,false],
  ['head','Content/Art/Characters/WatcherNinefoldFlailHead/SM_Watcher_NinefoldFlailHead_v1.obj',1.78,true]
];
function assemble(read) {
  const out = ['# RAZLOM original flail static assembly preview OBJ', '# metres, Z-up; NOT physics or UE5-ready', '# Source assets remain independent for rigging'];
  let vo=0, to=0;
  for (const [name, file, z, rotate] of parts) {
    const src = read(file).split(/\r?\n/), vertices=[], tex=[], faces=[];
    for (const line of src) {
      if (line.startsWith('v ')) {
        const v = line.trim().split(/\s+/).slice(1).map(Number);
        if (v.length !== 3 || !v.every(Number.isFinite)) throw Error('Invalid vertex in '+file);
        const p = rotate ? [v[0],v[2],-v[1]+z] : [v[0],v[1],v[2]+z];
        vertices.push('v '+p.map(x=>x.toFixed(6)).join(' '));
      } else if (line.startsWith('vt ')) tex.push(line);
      else if (line.startsWith('f ')) {
        const corners = line.slice(2).trim().split(/\s+/).map(x=>{
          const [a,b]=x.split('/').map(Number);
          if (!(a>0 && a<=vertices.length && b>0 && b<=tex.length)) throw Error('Invalid face in '+file);
          return (a+vo)+'/'+(b+to);
        });
        if (corners.length !== 3) throw Error('Non-triangle in '+file);
        faces.push('f '+corners.join(' '));
      }
    }
    if (!vertices.length || !tex.length || !faces.length) throw Error('Empty part: '+name);
    out.push('o WatcherFlail_'+name, ...vertices, ...tex, 'g WatcherFlail_'+name, ...faces);
    vo+=vertices.length; to+=tex.length;
  }
  return out.join('\n')+'\n';
}
if (require.main === module) {
  const output=process.argv[3] || 'SM_Watcher_Flail_Assembly_StaticPreview_v1.obj';
  fs.writeFileSync(output,assemble(p=>fs.readFileSync(path.join(root,p),'utf8')));
  console.log('Wrote '+output);
}
module.exports={assemble,parts};
