"""Paint a seamless dusk panorama in direction space, with a branched sky fracture."""
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.ndimage import map_coordinates

out = Path(__file__).resolve().parents[1] / "assets/village"
rng = np.random.default_rng(543)
w,h=1536,512
az,alt=np.meshgrid(np.linspace(-np.pi,np.pi,w,endpoint=False),np.linspace(1.57,-1.57,h))
x=np.cos(az)*np.cos(alt)
y=np.sin(alt)
z=np.sin(az)*np.cos(alt)
field=rng.random((64,64,64),dtype=np.float32)
def noise(a,b,c):
    return map_coordinates(field,[a,b,c],order=1,mode='grid-wrap',prefilter=False)
def fbm(a,b,c):
    return sum(noise(a*2**k,b*2**k,c*2**k)*(.53**k) for k in range(6))/2.078
# Intersect an elevated cloud deck; distant cloud banks compress at the horizon.
den=np.maximum(y+.24,.20)
a=x/den*2.4
b=z/den*2.4
warp=fbm(a+4,b+7,y*2)
cloud=fbm(a+warp*4,b+warp*2,warp*2+3)
sun=np.clip(x,0,1)**4
height=np.clip(alt/.95,0,1)
horizon=np.array([174,67,50])+sun[...,None]*np.array([80,53,10])
top=np.array([39,17,42])
sky=horizon*(1-height[...,None])+top*height[...,None]
rim=np.exp(-((cloud-.47)/.057)**2)*(.24+.76*sun)
mass=np.clip((cloud-.425)*6.6,0,.94)
sky=sky*(1-mass[...,None])+np.array([49,26,46])*mass[...,None]
sky+=rim[...,None]*[95,30,12]
sky+=np.exp(-((alt-.075)/.09)**2)[...,None]*sun[...,None]*[54,43,14]
base=Image.fromarray(np.clip(sky,0,255).astype('uint8'))
glow=Image.new('L',(w,h)); pen=ImageDraw.Draw(glow)
def bolt(points,width,level):
    for _ in range(4):
        more=[points[0]]
        for p,q in zip(points,points[1:]):
            mid=(np.array(p)+q)*.5
            mid+=rng.normal(0,np.linalg.norm(np.array(q)-p)*.10,2)
            more.extend([tuple(mid),q])
        points=more
    pen.line(points,fill=level,width=width)
def xy(lon,elev):
    return ((lon+np.pi)/(2*np.pi)*w,(1.57-elev)/3.14*h)
hub=xy(.145,.398)
for line,width,level in [
    ([xy(-.84,.64),xy(-.55,.61),xy(-.31,.53),xy(-.09,.52),hub,xy(.29,.46),xy(.49,.50),xy(.73,.62)],1,215),
    ([hub,xy(.125,.31),xy(.068,.28),xy(.051,.19)],1,252),
    ([xy(.49,.50),xy(.52,.63),xy(.64,.71)],1,185),
    ([xy(-.31,.53),xy(-.36,.59),xy(-.47,.61)],1,135)]:
    bolt(line,width,level)
rgb=np.array(base,dtype=float)
for radius,color in [(9,[73,0,64]),(3,[120,11,99]),(1,[182,48,140]),(0,[228,179,223])]:
    mask=np.asarray(glow.filter(ImageFilter.GaussianBlur(radius)),dtype=float)/255
    rgb+=mask[...,None]*color
Image.fromarray(np.clip(rgb,0,255).astype('uint8')).save(out/'storm.jpg',quality=91)
