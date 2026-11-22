'use strict';
const assert=require('node:assert/strict');
const {assemble,parts}=require('./build_watcher_flail_assembly');
function tetra(){return 'v 0 0 0\nv 1 0 0\nv 0 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nf 1/1 2/2 3/3\n';}
const src=Object.fromEntries(parts.map(p=>[p[1],tetra()]));
const obj=assemble(p=>src[p]);
assert.equal((obj.match(/^v /gm)||[]).length,9);
assert.equal((obj.match(/^vt /gm)||[]).length,9);
assert.equal((obj.match(/^f /gm)||[]).length,3);
assert.match(obj,/f 7\/7 8\/8 9\/9/);
assert.match(obj,/v 0\.000000 0\.000000 1\.780000/);
assert.equal(assemble(p=>src[p]),obj);
assert.throws(()=>assemble(p=>p===parts[0][1]?'v NaN 0 0\n'+tetra():src[p]),/Invalid vertex/);
assert.throws(()=>assemble(p=>p===parts[0][1]?tetra().replace('3/3','8/3'):src[p]),/Invalid face/);
console.log('PASS: 7 checks (vertices, UV, faces, offsets, axis transform, determinism, invalid inputs)');
