"""Paint the Zalesye district atlas. Unit tiles carry material, double tiles carry lettering."""
from pathlib import Path
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy.ndimage import gaussian_filter

OUT = Path(__file__).resolve().parents[2] / "assets/zalesye"
SIZE = 128
COLS, ROWS = 8, 4
rects = {}
rng = np.random.default_rng(712)
pen_rng = random.Random(712)
Y, X = np.mgrid[:SIZE, :SIZE]


def patch(scale):
    field = gaussian_filter(rng.standard_normal(X.shape), scale, mode="wrap")
    return field / field.std()


def paint():
    atlas = Image.new("RGBA", (COLS*SIZE, ROWS*SIZE))
    def put(name, slot, im, span=1):
        x, y = slot % COLS, slot // COLS
        rects[name] = (x*SIZE + 1, y*SIZE + 1, span*SIZE-2, SIZE-2)
        atlas.paste(im, (x*SIZE, y*SIZE))

    def colored(base, detail):
        return Image.fromarray(np.clip(np.array(base) + detail[...,None], 0, 255).astype("uint8")).convert("RGBA")

    for i,(name,base) in enumerate([
        ("brick",(139,83,60)), ("chalk",(175,166,138)), ("timber",(106,85,59)),
        ("paint",(72,106,97)), ("cream",(186,179,150)), ("slate",(73,83,84)),
        ("iron",(65,65,61)), ("tar",(51,49,44)), ("stone",(102,105,95)),
        ("pine",(80,65,49)), ("birch",(154,155,135)), ("brass",(148,117,67)),
        ("glass",(57,75,76)), ("paper",(190,174,141)), ("red",(120,66,49)), ("cloth",(125,123,111))]):
        detail = 3*patch(.48)+3*patch(3.2)+5*patch(11)
        if name in ("timber","pine","paint","cream","birch"):
            detail += patch((1.0,19))*7 + np.cos(Y*(.82+.035*np.sin(X/45))+.95*patch((3,16)))*3
            if name in ("paint","cream"):
                detail -= (patch((1.2,7))>1.5)*28
            if name=="birch":
                detail -= (patch((.65,5))>1.35)*70
        if name=="brick":
            row=(Y//12); col=(X+row%2*16)//32
            detail += rng.uniform(-15,14,(12,6))[row,col]
            mortar=(Y%12<=1)|((X+row%2*16)%32<2)
            tile=colored(base,detail)
            a=np.array(tile); a[mortar,:3]=np.clip(np.array([132,122,101])+detail[mortar,None]*.55,0,255)
            tile=Image.fromarray(a)
        elif name=="slate":
            detail += 9*np.cos(X*np.pi/8) + 3*patch((20,1)) - 10*(Y%62<2)
            tile=colored(base,detail)
        elif name=="chalk":
            chips=patch(5)+.48*patch(1.3)
            tile=colored(base,detail-12*np.maximum(chips-.2,0)); a=np.array(tile)
            mask=chips>1.70; row=Y//10; mortar=(Y%10<2)|((X+row%2*13)%26<2)
            a[mask,:3]=np.clip(np.array([143,96,73])+detail[mask,None],0,255)
            a[mask&mortar,:3]=[131,123,106]; tile=Image.fromarray(a)
        elif name=="glass":
            tile=colored(base,7*patch((11,3))+10*(1-Y/SIZE))
        else:
            tile=colored(base,detail)
        put(name,i,tile)

    for slot,name,lines,background in [
        (16,"post",["ПОЧТА","З А Л Е С Ь Е    •    1 9 4 8"],"#284241"),
        (18,"stop",["ЗАЛЕСЬЕ","АВТОСТАНЦИЯ   /   07"],"#43504b"),
        (20,"letters",["ИСХОДЯЩИЕ","УЧАСТОК   4"],"#716950"),
        (22,"pump",["ВОДОКАЧКА","СТАНЦИЯ    №  3"],"#5b473d")]:
        im=Image.new("RGBA",(256,128),background);d=ImageDraw.Draw(im)
        d.rounded_rectangle((4,5,251,122),radius=4,outline="#b4a27c",width=3)
        for j,line in enumerate(lines):
            font=ImageFont.truetype("DejaVuSerif-Bold.ttf" if j==0 else "DejaVuSans.ttf", (41 if name=="post" else 28) if j==0 else 13)
            d.text((128,52 if j==0 else 93),line,font=font,fill="#e5d6b3",anchor="mm")
        for _ in range(140):
            x,y=pen_rng.randrange(256),pen_rng.randrange(128)
            d.line((x,y,x+pen_rng.randint(1,4),y),fill="#635a4a",width=1)
        if name=="post":
            im=Image.new("RGBA",(768,128),"#263d3d"); dd=ImageDraw.Draw(im)
            dd.rounded_rectangle((4,5,763,122),radius=6,outline="#c0af86",width=3)
            dd.text((384,47),"П О Ч Т А",font=ImageFont.truetype("DejaVuSerif-Bold.ttf",63),anchor="mm",fill="#ecdcc1")
            dd.text((384,96),"З А Л Е С Ь Е     •     О Т Д Е Л Е Н И Е   0 7",font=ImageFont.truetype("DejaVuSans.ttf",17),anchor="mm",fill="#c9c7ac")
            im=im.resize((256,128),Image.Resampling.LANCZOS)
        put(name,slot,im,2)
    # Needled fans and clumps contain contiguous pigment, with gaps near the tips.
    for slot,name,colors in [(24,"needles",["#263c39","#344840","#415748","#536457"]),
                              (25,"leaves",["#45534a","#566954","#70805e","#879476"]),
                              (26,"weeds",["#484737","#615d45","#73704e","#96916c"])]:
        im=Image.new("RGBA",(SIZE,SIZE)); d=ImageDraw.Draw(im)
        if name=="needles":
            d.line((65,3,64,119),fill=colors[1],width=4)
            for j in range(7,110,3):
                for side in [-1,1]:
                    reach=(1-j/130)*pen_rng.uniform(35,62)
                    end=(int(64+side*reach),int(j+reach*.62))
                    d.line((64,j,*end),fill=pen_rng.choice(colors),width=4)
                    for k in range(2,9):
                        u=k/9; x=64+(end[0]-64)*u; y=j+(end[1]-j)*u
                        d.line((x,y,x+side*3,y+pen_rng.randint(3,13)),fill=pen_rng.choice(colors),width=2)
        elif name=="leaves":
            for _ in range(440):
                x,y=pen_rng.gauss(64,22),pen_rng.gauss(66,19)
                d.polygon([(x,y-4),(x+4,y-2),(x+3,y+3),(x-1,y+5),(x-3,y)],fill=pen_rng.choice(colors))
        else:
            for _ in range(47):
                x=pen_rng.gauss(63,16); xt=x+pen_rng.uniform(-40,40); yt=pen_rng.uniform(13,103)
                d.polygon([(x-1,127),(xt,yt),(x+2,127)],fill=pen_rng.choice(colors))
                if yt<40: d.line((xt,yt,xt+2,yt+11),fill=colors[-1],width=3)
        put(name,slot,im)
    clock=Image.new("RGBA",(128,128),"#303934"); d=ImageDraw.Draw(clock)
    d.ellipse((5,5,123,123),fill="#b9b096",outline="#544c3d",width=5)
    for i in range(12):
        a=i*np.pi/6
        d.line((64+44*np.sin(a),64-44*np.cos(a),64+51*np.sin(a),64-51*np.cos(a)),fill="#3c3933",width=3)
    d.line((47,82,64,64,96,45),fill="#342f2d",width=4)
    put("clock",27,clock)
    for slot,name,color in [(28,"canvas",(62,66,54)),(29,"leather",(52,46,38)),(30,"seam",(102,94,72)),(31,"lampmetal",(56,62,58))]:
        grain=patch(.6)*4+patch(8)*5
        if name=="canvas":
            grain+=2.7*np.cos(X*np.pi)+2.6*np.cos(Y*np.pi)
        elif name=="lampmetal":
            grain+=6*patch((9,1))-(patch((11,.58))>1.55)*22
        put(name,slot,colored(color,grain))
    atlas.save(OUT/"district.png",optimize=True)
    return atlas.size
