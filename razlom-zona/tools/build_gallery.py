"""Generate the four-metre sloping service-gallery bay. Python 3, Pillow."""
import base64
import json
import math
import random
import struct
from pathlib import Path
from PIL import Image, ImageDraw

DEST = Path(__file__).resolve().parents[1] / 'assets/bunker/gallery.gltf'
rng = random.Random(402)
atlas = Image.new('RGB', (128, 64))
colors = [(96, 104, 98), (62, 76, 72), (141, 79, 49), (169, 142, 66),
          (59, 65, 65), (124, 168, 157), (203, 207, 178), (104, 52, 42)]
draw = ImageDraw.Draw(atlas)
for tile, rgb in enumerate(colors):
    ox, oy = tile % 4 * 32, tile // 4 * 32
    for y in range(32):
        for x in range(32):
            grain = rng.randrange(-9, 10)
            if tile == 0 and y in (2, 26):
                grain -= 14
            if tile == 3 and (x + y) % 18 < 8:
                grain -= 67
            if tile == 4 and (x % 8 == 1 or y % 16 == 1):
                grain -= 25
            draw.point((ox+x, oy+y), tuple(max(0, v+grain) for v in rgb))
    for _ in range(12):
        x, y = rng.randrange(2, 30), rng.randrange(2, 27)
        draw.line((ox+x,oy+y,ox+x,oy+y+rng.randrange(1,5)), fill=tuple(v-18 for v in rgb))

# White stencil on the oxide-red service plate. Draw in pixels to keep hard edges.
for start, glyph in [(2,["1111","1000","1000","1000","1000"]),
                     (12,["000","000","111","000","000"]),
                     (21,["101","101","111","001","001"])]:
    for row, line in enumerate(glyph):
        for col, bit in enumerate(line):
            if bit == '1':
                x, y = 96+start+2*col, 32+11+2*row
                draw.rectangle((x,y,x+1,y+1),fill=(224,207,165))
pos, norm, uv, indices = [], [], [], []
def quad(points, tile):
    # Slope is one metre per four metres of run, with the pivot at the near sill.
    p = [(x,y+z/4,z) for x,y,z in points]
    a,b = [[p[k][i]-p[0][i] for i in range(3)] for k in (1,2)]
    n = (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
    length = math.hypot(*n)
    start = len(pos)//3
    for point,(u,v) in zip(p,[(0,1),(1,1),(1,0),(0,0)]):
        pos.extend(point)
        norm.extend(c/length for c in n)
        uv.extend(((32*(tile%4)+.5+31*u)/128,(32*(tile//4)+.5+31*v)/64))
    indices.extend(start+i for i in (0,1,2,0,2,3))

def prism(section, front, back, tile):
    # Counterclockwise convex XY cross section. Near and far caps are quads.
    n = len(section)
    for i in range(n):
        a,b = section[i], section[(i+1)%n]
        quad([(*a,back),(*b,back),(*b,front),(*a,front)], tile)
    quad([(*p,front) for p in section], tile)
    quad([(*p,back) for p in reversed(section)], tile)

def bar(x1,y1,x2,y2,front,back,tile):
    prism([(x1,y1),(x2,y1),(x2,y2),(x1,y2)],front,back,tile)

def arch(front, back, thickness, tile):
    inner = [(-1.8,0),(-1.8,2),(-1.32,2.62),(-.85,2.85),(.85,2.85),(1.32,2.62),(1.8,2),(1.8,0)]
    outer = [(x+(-thickness if x<0 else thickness), y+(thickness if y>2 else 0)) for x,y in inner]
    for i in range(len(inner)-1):
        prism([inner[i], inner[i+1], outer[i+1], outer[i]], front,back,tile)

arch(0,-4,.24,0)
bar(-2.04,-.19,2.04,0,0,-4,4)
for z in (-.14,-2.14):
    # Steel reinforcing ribs follow the full faceted vault.
    profile=[(-1.8,0),(-1.8,2),(-1.32,2.62),(-.85,2.85),(.85,2.85),(1.32,2.62),(1.8,2),(1.8,0)]
    for a,b in zip(profile,profile[1:]):
        def inset(p):
            return (p[0]*.943,p[1]*.963)
        prism([inset(a),inset(b),b,a],z,z-.13,1)
for side in (-1,1):
    x=side*1.55
    bar(x-.055,.012,x+.055,.03,0,-4,3)
for y,r,tile in ((.73,.095,2),(1.03,.067,1)):
    # Octagonal return pipes, with fixed collars between expansion joints.
    for z1,z2,radius,t in [(0,-4,r,tile),(-.46,-.57,r+.035,4),(-3.4,-3.51,r+.035,4)]:
        circle=[(-1.56+radius*math.cos(i*math.pi/4),y+radius*math.sin(i*math.pi/4)) for i in range(8)]
        for i,a in enumerate(circle):
            b=circle[(i+1)%8]
            quad([(*a,z2),(*b,z2),(*b,z1),(*a,z1)],t)
# Stencilled bay marker on the right wall, readable on approach.
bar(1.748,1.32,1.80,1.71,-2.60,-3.24,1)
quad([(1.745,1.32,-3.22),(1.745,1.32,-2.62),(1.745,1.71,-2.62),(1.745,1.71,-3.22)],7)
for z in (-.65,-3.65):
    bar(-1.76,.56,-1.46,.62,z,z-.08,1)
bar(.36,2.64,.63,2.79,-.75,-1.82,1)
bar(.40,2.61,.59,2.645,-.81,-1.76,6)
for z in (-.84,-1.11,-1.38,-1.68):
    bar(.39,2.596,.60,2.62,z,z-.035,4)

binary=bytearray()
views=[]
accessors=[]
def accessor(values, fmt, kind, width, target):
    while len(binary)%4:
        binary.append(0)
    raw=struct.pack('<'+str(len(values))+fmt,*values)
    views.append(dict(buffer=0,byteOffset=len(binary),byteLength=len(raw),target=target))
    binary.extend(raw)
    item=dict(bufferView=len(views)-1,componentType=5126 if fmt=='f' else 5123,
              count=len(values)//width,type=kind)
    if values is pos:
        for key,fn in [('min',min),('max',max)]:
            item[key]=[fn(values[i::3]) for i in range(3)]
    accessors.append(item)
    return len(accessors)-1
attrs={key:accessor(values,'f',kind,width,34962) for key,values,kind,width in
       [('POSITION',pos,'VEC3',3),('NORMAL',norm,'VEC3',3),('TEXCOORD_0',uv,'VEC2',2)]}
primitive=dict(attributes=attrs,indices=accessor(indices,'H','SCALAR',1,34963),material=0)
def uri(kind,data):
    return 'data:'+kind+';base64,'+base64.b64encode(data).decode()
atlas.save(DEST.with_name("gallery_atlas.png"))
doc=dict(asset={'version':'2.0'},scene=0,scenes=[{'nodes':[0]}],nodes=[dict(mesh=0,name='ServiceGallery')],
         meshes=[dict(primitives=[primitive])], materials=[dict(name='Service enamel and cast concrete',
         pbrMetallicRoughness=dict(baseColorTexture=dict(index=0),metallicFactor=0,roughnessFactor=.92))],
         images=[dict(uri='gallery_atlas.png')],samplers=[dict(magFilter=9728,minFilter=9984)],
         textures=[dict(sampler=0,source=0)],buffers=[dict(byteLength=len(binary),uri=uri('application/octet-stream',binary))],
         bufferViews=views,accessors=accessors)
DEST.write_text(json.dumps(doc,separators=(',',':'))+'\n')
print('triangles',len(indices)//3,'bounds',accessors[0]['min'],accessors[0]['max'])
