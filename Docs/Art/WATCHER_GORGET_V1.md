# Ninth Entrance Watcher — ritual gorget v1

Original actual OBJ mesh: `Content/Art/Characters/Watcher/SM_Watcher_Gorget_v1.obj`.
Rebuild: `node Tools/Art/generate_watcher_gorget_v1.js > Content/Art/Characters/Watcher/SM_Watcher_Gorget_v1.obj`.

**Intended scene:** Act I, Ninth Entrance, Watcher enemy; armored neck silhouette under the existing mask and above the chest garment.
**Mesh:** 450 positions, 432 triangles, 9 ritual spikes, 9 rivets, 3 chest lamellae. Z-up metres, nominal collar span Z 1.47–1.85 m. Nine spikes are original procedural geometry. All faces are triangles; source indices validated on generation.
**Status:** geometric draft / blockout, not production-ready. The rings are deliberately open and unthickened; normals, UV, skin weights, material assignments, collision, LOD, overlap with existing Watcher mask and pauldrons, and UE5 import/render are **not verified**.
**Art direction:** dark forged iron, worn edges, restrained ritual ornament; material color is a proposal, not an authored PBR texture.
**Integration blocker:** confirm Watcher skeleton neck/chest sockets, adjust mesh to actual character proportions and add UV + thickness + normal generation before engine import. Keep as independent static piece until skinning is tested.
