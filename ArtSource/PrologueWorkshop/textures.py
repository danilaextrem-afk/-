"""Author the two face atlases. Run with Python, Pillow, numpy and scipy."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np
from scipy.ndimage import gaussian_filter
from math import sin, cos, radians
ROOT = Path(__file__).resolve().parent
FONT = Path('/usr/share/fonts/truetype/dejavu')

def label(x, y, s, size, fill, bold=False):
    font = ImageFont.truetype(str(FONT / ('DejaVuSans-Bold.ttf' if bold else 'DejaVuSans.ttf')), size)
    draw.text((x,y),s,font=font,fill=fill,anchor='mm',stroke_width=0)

def noise(size):
    return np.asarray(Image.fromarray(rng.normal(128,38,(size,size)).clip(0,255).astype('uint8')).resize((w,h),Image.Resampling.BICUBIC),dtype='float32')-128

for name in ['Gauge','Entrance']:
    out=ROOT/name/'Maps'
    out.mkdir(parents=True,exist_ok=True)
    gauge=name=='Gauge'
    w,h = (1024,1024) if gauge else (2048,1024)
    rng=np.random.default_rng(562 if gauge else 953)
    img=Image.new('RGB',(w,h),(223,217,187) if gauge else (28,57,79))
    draw=ImageDraw.Draw(img)
    ink=(43,45,41)
    if gauge:
        def polar(deg,r):
            a=radians(deg)
            return (512+r*cos(a),512-r*sin(a))
        for i in range(81):
            angle=225-i*270/80
            color=(135,52,37) if i>=65 else ink
            draw.line([polar(angle,446),polar(angle,401 if i%10==0 else 420 if i%5==0 else 429)],fill=color,width=5 if i%5==0 else 3)
            if i%10==0:
                label(*polar(angle,350),str(i//10),74,color,True)
        label(512,345,'МД · 08',40,ink,True)
        label(512,640,'бар',66,ink,True)
        label(512,735,'КЛ. 1,6',29,ink)
        label(512,829,'№ 04176',25,ink)
    else:
        ivory=(219,219,194)
        draw.rounded_rectangle((71,75,w-71,h-75),75,outline=ivory,width=10)
        draw.line((1090,225,1090,800),fill=ivory,width=7)
        label(590,480,'ПОДЪЕЗД',144,ivory,True)
        label(1475,482,'9',615,ivory,True)
    y,x=np.mgrid[:h,:w]
    low=noise(32); med=noise(170); grain=rng.normal(0,1,(h,w))
    if gauge:
        edge=np.hypot((x-w/2)/(w/2),(y-h/2)/(h/2))
        dirt=np.maximum(edge-.72,0)*48
        wear=np.zeros_like(low,dtype=bool)
    else:
        # Signed distance to the stamped rounded rectangle and its fasteners.
        qx=abs(x-w/2)-(w/2-100); qy=abs(y-h/2)-(h/2-100)
        margin=100-np.hypot(qx.clip(0),qy.clip(0))-np.minimum(np.maximum(qx,qy),0)
        for xx in [.055,.945]:
            for yy in [.15,.85]:
                margin=np.minimum(margin,np.hypot(x-w*xx,y-h*yy)-20)
        wear=margin < (5+low*.39+med*.17)
        dirt=8*np.exp(-np.maximum(margin,0)/27)
    rgb=np.asarray(img,dtype=float)+(.11*low+.07*med+.9*grain-dirt)[:,:,None]
    rgb[wear]=np.stack([91+.51*med,58+.39*med,37+.25*med],-1)[wear]
    rough=(142 if gauge else 83)+med*.35+low*.23+wear*92
    metal=np.where(wear & (med>20),170,0)
    height=gaussian_filter(med*.03+grain*.07-wear*2,.85)
    dy,dx=np.gradient(height)
    n=np.stack([-.12*dx,.12*dy,np.ones_like(dx)],-1); n/=np.linalg.norm(n,axis=2,keepdims=True)
    for file,arr in [('Color',rgb),('Normal',(n*.5+.5)*255),('MetalRough',np.stack([np.full_like(rough,255),rough,metal],-1))]:
        Image.fromarray(arr.clip(0,255).astype('uint8')).save(out/(file+'.png'))
