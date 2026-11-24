"""Small metre-scale, Y-up mesh writer with flat normals and padded atlas UVs."""
from collections import defaultdict
import json
import math
import struct

import numpy as np
from atlas import OUT, SIZE, COLS, ROWS, rects

class Model:
    def __init__(self, name):
        self.name = name
        self.groups = defaultdict(lambda: [[],[],[],[]])

    def face(self, points, tile, group="painted", uv=None):
        points=np.asarray(points,dtype=float)
        normal=np.cross(points[1]-points[0],points[2]-points[0])
        length=np.linalg.norm(normal)
        if length < 1e-7:
            raise ValueError(f"Degenerate face in {self.name}: {points}")
        normal/=length
        pos,norm,tex,indices=self.groups[group]
        start=len(pos)
        pos.extend(points); norm.extend([normal]*len(points))
        x,y,w,h=rects[tile]
        if uv is None:
            uv=[(0,1),(1,1),(1,0),(0,0)][:len(points)]
        for u,v in uv:
            tex.append(((x+w*u)/(COLS*SIZE),(y+h*v)/(ROWS*SIZE)))
        for i in range(1,len(points)-1):
            indices.extend([start,start+i,start+i+1])

    def box(self, center, size, tile):
        x,y,z=np.array(size)/2
        corners=np.array([[-x,-y,-z],[x,-y,-z],[x,y,-z],[-x,y,-z],[-x,-y,z],[x,-y,z],[x,y,z],[-x,y,z]])+center
        for order in [(3,2,1,0),(4,5,6,7),(0,4,7,3),(5,1,2,6),(7,6,2,3),(0,1,5,4)]:
            self.face(corners[list(order)],tile)

    def spar(self, a, b, radius, tile, end=None, sides=6):
        a,b=np.asarray(a),np.asarray(b)
        n=b-a; n=n/np.linalg.norm(n)
        axis=np.cross(n,[0,1,0] if abs(n[1])<.95 else [1,0,0]); axis/=np.linalg.norm(axis)
        other=np.cross(n,axis); end=radius if end is None else end
        rings=[axis*math.cos(i*math.tau/sides)+other*math.sin(i*math.tau/sides) for i in range(sides)]
        for i,p in enumerate(rings):
            q=rings[(i+1)%sides]
            self.face([a+p*radius,a+q*radius,b+q*end,b+p*end],tile)
            self.face([b,b+p*end,b+q*end],tile)

    def panel(self, x, y, z, width, height, tile):
        self.face([(x-width/2,y-height/2,z),(x+width/2,y-height/2,z),
                   (x+width/2,y+height/2,z),(x-width/2,y+height/2,z)],tile)

    def save(self):
        data=bytearray();views=[];access=[]
        def acc(array,dtype,kind):
            a=np.asarray(array,dtype=dtype); data.extend(bytes(-len(data)%4))
            views.append(dict(buffer=0,byteOffset=len(data),byteLength=a.nbytes))
            data.extend(a.tobytes())
            item=dict(bufferView=len(views)-1,componentType=5126 if dtype=="<f4" else 5125,type=kind,count=len(a))
            if kind=="VEC3":item.update(min=a.min(axis=0).tolist(),max=a.max(axis=0).tolist())
            access.append(item);return len(access)-1
        materials=[];primitives=[]
        for name,(pos,nor,uv,indices) in self.groups.items():
            mat=dict(name=name,doubleSided=True,pbrMetallicRoughness=dict(baseColorTexture={"index":0},metallicFactor=0,roughnessFactor=.88))
            if name=="canopy":mat.update(alphaMode="MASK",alphaCutoff=.46)
            materials.append(mat)
            primitives.append(dict(attributes=dict(POSITION=acc(pos,"<f4","VEC3"),NORMAL=acc(nor,"<f4","VEC3"),TEXCOORD_0=acc(uv,"<f4","VEC2")),
                                   material=len(materials)-1,indices=acc(indices,"<u4","SCALAR")))
        doc=dict(asset=dict(version="2.0",generator="Zalesye district workshop"),scene=0,scenes=[dict(nodes=[0])],
                 nodes=[dict(name=self.name,mesh=0)],meshes=[dict(primitives=primitives)],materials=materials,
                 textures=[dict(source=0,sampler=0)],images=[dict(uri="district.png")],samplers=[dict(magFilter=9728,minFilter=9984)],
                 accessors=access,bufferViews=views,buffers=[dict(byteLength=len(data))])
        header=json.dumps(doc,separators=(",",":")).encode();header+=b" "*(-len(header)%4);data+=bytes(-len(data)%4)
        (OUT/(self.name+".glb")).write_bytes(struct.pack("<4sII",b"glTF",2,28+len(header)+len(data))+struct.pack("<I4s",len(header),b"JSON")+header+struct.pack("<I4s",len(data),b"BIN\0")+data)
