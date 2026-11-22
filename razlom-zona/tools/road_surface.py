"""Generate one continuous pixel surface with embedded puddle coverage in alpha."""
from pathlib import Path
import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter
rng=np.random.default_rng(739)
h,w=1536,320
y,x=np.mgrid[0:h,0:w]
def field(s):
    f=gaussian_filter(rng.normal(size=(h,w)),s,mode='reflect')
    return (f-f.mean())/f.std()
wide=field((24,10));grit=field((1.6,1.2));rough=field((5,3));grain=rng.normal(size=(h,w))
bend=5*np.sin(y/155)+3*np.sin(y/42)
ruts=np.minimum(abs(x-119-bend),abs(x-194-bend))
edge=np.minimum(x,w-x)
grass=np.clip((73-edge+12*wide+4*rough)/24,0,1)
rut=np.exp(-(ruts/(10+3*np.sin(y/34)))**2)
tread=np.where(((y+(x%10)*.5)%17)<3.5,1,0)*rut
stone=(grit>1.06)*np.maximum(grit-.65,0)*20
dirt=wide*4+rough*4+grain*4+stone-6*tread
ground=np.array([76,69,61])+dirt[...,None]*[1,.93,.85]
leaves=np.array([67,63,43])+(rough*8+grit*9+grain*5)[...,None]
rgb=ground*(1-grass[...,None])+leaves*grass[...,None]
wet=np.clip((rut*.89+.29*wide+.11*rough+.04*grit-.60)*4.1,0,1)
rgb*=(1-wet[...,None]*.40)
rgba=np.dstack([np.clip(rgb,0,255),wet*255]).astype('uint8')
Image.fromarray(rgba).save(Path(__file__).resolve().parents[1]/'assets/village/road.png')
