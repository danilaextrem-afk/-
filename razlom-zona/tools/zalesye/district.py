"""Generate original architecture, street furniture and instanceable trees for Zalesye."""
import math
import random
import numpy as np
from atlas import paint
from mesh import Model

rng=random.Random(47)

def window(m,x,y,z,w,h,open_pane=False):
    # Recess, deep sill, two casements and a transom. Leave the broken pane open.
    if not open_pane: m.panel(x,y,z-.085,w,h,"glass")
    for dx in [-w/2,w/2]:
        m.box((x+dx,y,z),(.095,h+.20,.15),"cream")
    for dy in [-h/2,h/2]:
        m.box((x,y+dy,z),(w+.14,.105,.16),"cream")
    m.box((x,y-h/2-.10,z+.04),(w+.32,.11,.33),"stone")
    m.box((x,y,z+.015),(.052,h,.085),"paint")
    m.box((x,y+h*.21,z+.02),(w,.055,.092),"paint")
    if open_pane:
        m.face([(x-w*.47,y+h*.47,z-.04),(x-w*.06,y+h*.47,z-.04),
                (x-w*.32,y+h*.26,z-.04)],"glass")
    else:
        m.box((x,y-h*.08,z+.015),(w,.036,.06),"paint")

def roof(m,cx,cz,w,d,eave,rise,damage=False,tile="slate"):
    # Ridge runs across the facade. Each missing strip exposes real rafters below.
    ridge=eave+rise; n=max(6,round(w/.82))
    for side in [-1,1]:
        e=cz+side*d/2
        for i in range(n):
            xl=cx-w/2+w*i/n; xr=xl+w/n-.012
            cut=.48 if damage and i in [1,2,3] and side<0 else 0.
            low=eave+rise*cut; end=e+(cz-e)*cut
            m.face([(xl,low,end),(xr,low,end),(xr,ridge,cz),(xl,ridge,cz)],tile)
            m.spar((xl,eave-.08,e),(xl,ridge-.08,cz),.062,"timber",sides=4)
            if i%2==0:m.spar((xl+.07,low+.02,end),(xl+.07,ridge+.02,cz),.025,"iron",sides=4)
        m.spar((cx-w/2,eave-.02,e),(cx+w/2,eave-.02,e),.085,"tar",sides=4)
    m.spar((cx-w/2,ridge,cz),(cx+w/2,ridge,cz),.095,"iron",sides=6)
    for side in [-1,1]:
        x=cx+side*(w/2-.26)
        m.face([(x,eave,cz-d/2+.25),(x,eave,cz+d/2-.25),(x,ridge-.05,cz)],"timber")

def chimney(m,x,z,y):
    m.box((x,y,z),(.56,1.88,.69),"brick")
    for yy,k in [(y+.71,.62),(y+.92,.73)]:
        m.box((x,yy,z),(k,.12,k+.11),"stone")
    m.box((x,y+1.08,z),(.31,.28,.39),"tar")

def gable(m,cx,z,w,base,peak,projection,tile):
    front=z+projection
    m.face([(cx-w/2,base,front),(cx+w/2,base,front),(cx,peak,front)],tile)
    for side in [-1,1]:
        ex=cx+side*(w/2+.12)
        m.face([(ex,base-.03,front+.17),(ex,base-.03,z-.45),(cx,peak+.10,z-.45),(cx,peak+.10,front+.17)],"slate")
        m.spar((ex,base,front+.16),(cx,peak+.13,front+.16),.11,"cream",sides=4)
    m.box((cx,base,front),(w+.2,.13,.24),"cream")
    return front

def table(m,x,y,z,w,d):
    m.box((x,y,z),(w,.1,d),"timber")
    for dx in [-1,1]:
        for dz in [-1,1]:m.box((x+dx*(w/2-.09),y/2,z+dz*(d/2-.09)),(.085,y,.085),"paint")

def arch(m,x,y,z,r):
    for i in range(13):
        a,b=math.pi*i/13,math.pi*(i+1)/13-.028
        def pt(angle,radius,depth):
            return (x+radius*math.cos(angle),y+radius*math.sin(angle),depth)
        m.face([pt(a,r,z),pt(a,r+.19,z),pt(b,r+.19,z),pt(b,r,z)],"cream" if i%4 else "stone")
        m.face([pt(a,r,z-.16),pt(a,r,z),pt(b,r,z),pt(b,r,z-.16)],"stone")

def post_office():
    m=Model("post_house");box=m.box; z=2.45
    # Wall openings cut through the shell. No full-box collider or threshold step.
    box((0,.008,0),(8.3,.03,5.05),"timber")
    for x in np.arange(-3.86,4.01,.46):
        box((x,.036,0),(.445,.032,4.88),"timber")
    box((0,1.93,-z),(8.25,3.86,.21),"chalk")
    for side in [-1,1]:
        box((4.03*side,1.92,0),(.19,3.84,4.88),"chalk")
        for zz in [-1.6,.15,1.76]:
            bx=4.14*side
            box((bx,1.69,zz),(.09,1.45,.95),"glass")
            for k in [-.52,0,.52]:box((bx+.055*side,1.7,zz+k),(.15,1.62,.069),"cream")
            for yy in [.91,1.87,2.50]:box((bx+.07*side,yy,zz),(.19,.09,1.18),"cream")
    for x,w in [(-3.85,.56),(-2.16,.52),(-.30,.78),(1.32,.57),(3.47,1.22)]:
        box((x,1.83,z),(w,3.66,.31),"chalk")
        box((x,.29,z+.03),(w,.58,.40),"brick")
    for x,w in [(-2.85,1.29),(-1.31,1.23),(.58,.99)]:
        box((x,.52,z),(w,1.04,.29),"chalk")
        box((x,.31,z+.08),(w,.62,.37),"brick")
        box((x,3.20,z),(w,.94,.31),"chalk")
        window(m,x,1.89,z+.11,w-.04,1.60,True)
        # Drip moulding on corbels.
        box((x,2.87,z+.12),(w+.27,.13,.36),"stone")
    box((2.23,3.09,z),(1.32,1.14,.31),"chalk")
    for x in [-4.06,4.05]:
        for k in range(10):
            box((x,.23+k*.35,z+.027),(.42 if k%2==0 else .63,.27,.44),"brick")
    for y,depth,width in [(3.61,.34,8.38),(3.77,.48,8.55),(3.91,.55,8.74)]:
        box((0,y,z-.04),(width,.13,depth),"cream")
    roof(m,0,0,9.03,5.73,3.98,1.90,True)
    chimney(m,-2.67,-.51,5.5)
    # Telegraph dormer with fanlight, shaped zinc apron and collars on the finial.
    zz=gable(m,-1.13,z+.05,3.35,3.97,5.61,.24,"paint")
    m.panel(-1.13,4.40,zz+.013,1.36,.72,"glass")
    for dx in [-.54,-.30,0,.30,.54]:
        m.spar((-1.13,4.09,zz+.04),(-1.13+dx,4.39+.36*(1-abs(dx)/.7),zz+.04),.032,"cream",sides=4)
    m.box((-1.13,4.05,zz+.05),(1.79,.12,.25),"cream")
    m.spar((-1.13,5.62,zz),(-1.13,6.29,zz),.055,"iron",.018)
    # Covered entrance. A slim shed roof leaves the fascia and sign readable.
    for x in [1.34,3.17]:
        m.spar((x,.055,z+1.05),(x,2.63,z+1.05),.073,"iron")
        m.spar((x,2.08,z+1.05),(x,2.59,z+.65),.042,"iron")
    m.face([(1.12,2.62,z+1.29),(3.40,2.62,z+1.29),(3.40,2.93,z),(1.12,2.93,z)],"slate")
    arch(m,2.22,2.32,z+.17,.62)
    m.box((-1.16,3.29,z+.18),(3.78,.66,.21),"tar")
    m.panel(-1.16,3.29,z+.297,3.65,.59,"post")
    m.box((3.44,1.36,z+.19),(.54,.67,.34),"paint")
    m.box((3.44,1.55,z+.368),(.41,.045,.04),"tar")
    # A downspout bends around the cornice and empties into the yard.
    for a,b in [((3.89,3.84,z+.42),(3.89,3.45,z+.26)),((3.89,3.45,z+.26),(3.89,.33,z+.26)),((3.89,.33,z+.26),(3.89,.15,z+.53))]:
        m.spar(a,b,.069,"iron")
    # Shutters pinned back against the wall and splintered glazing in the service hall.
    for x in [-3.77,.00]:
        for j in range(4):
            box((x+.07*j,1.88,z+.19),(.064,1.54,.065),"paint")
        for yy in [1.27,2.45]:
            box((x+.11,yy,z+.238),(.32,.082,.054),"iron")
    for x in [-2.91,-1.34]:
        m.panel(x+.16,1.71,z-.04,.46,.78,"glass")
    # Timber ceiling, cornices and joists enclose the interior volume.
    box((0,3.76,0),(8.04,.09,4.87),"timber")
    for xx in [-3.63,-2.32,-.99,.36,1.68,3.05]:
        box((xx,3.64,0),(.13,.19,4.82),"timber")
    # Public counter at the rear and sorting rack; envelopes left in the slots.
    table(m,-1.02,.94,-.68,4.05,.83)
    m.box((-1.02,.46,-.31),(3.96,.82,.11),"paint")
    for i in range(7):
        m.box((-2.75+i*.57,.47,-.239),(.045,.78,.06),"cream")
    for i in range(7):
        m.box((-3.0+i*.56,1.86,-2.14),(.055,1.75,.43),"timber")
    for i in range(5):m.box((-1.31,1.02+i*.42,-2.14),(3.43,.06,.48),"timber")
    for i in range(17):
        x=-2.73+.56*rng.randrange(6);y=1.08+.42*rng.randrange(4)
        m.box((x,y,-1.99),(.32,.05+rng.random()*.08,.24),"paper")
    m.panel(-1.03,2.96,-2.31,1.6,.34,"letters")
    m.panel(2.1,2.48,-2.32,.66,.66,"clock")
    for dx in [-.20,.13,.49]:
        box((-1.1+dx,1.008,-.44),(.31,.02,.19),"paper")
    box((-1.4,1.04,-.94),(.32,.12,.24),"iron")
    # Cotton privacy curtain at the sorting room, with creased staggered hems.
    for i in range(11):
        x=-3.78+i*.105; hem=.38+.12*math.sin(i*1.6)
        m.face([(x,hem,-.39),(x+.105,hem+.04,-.39+.08*(-1)**i),
                (x+.105,2.62,-.39+.08*(-1)**i),(x,2.62,-.39)],"cloth")
    table(m,2.80,.47,-1.23,1.58,.43)
    # Shaded pendant and spun enamel outside lamp, filaments supplied by gameplay.
    for x,y,zz in [(-1,2.48,-.60),(1.06,2.33,3.0)]:
        m.spar((x,y+.03,zz),(x,3.66,zz),.016,"tar")
        m.spar((x,y+.25,zz),(x,y+.06,zz),.072,"paint",.235,12)
    m.save()

def postal_door():
    m=Model("postal_door")
    for x in [.064,1.17]:m.box((x,1.18,0),(.12,2.33,.095),"paint")
    for y in [.10,.80,1.36,2.28]:m.box((.61,y,0),(1.17,.12,.10),"paint")
    for i in range(6):m.box((.17+.18*i,.68,-.013),(.175,1.21,.061),"timber")
    for x in [.42,.80]:m.box((x,1.85,0),(.045,.85,.08),"cream")
    m.box((.61,1.85,0),(1.05,.045,.08),"cream")
    m.box((1.06,1.12,.088),(.18,.039,.09),"brass")
    m.save()

def cottage():
    m=Model("widow_house"); w,d=5.95,4.62; z=d/2
    for k in range(14):
        yy=.21+k*.199
        m.box((0,yy,0),(w+(.15 if k%2 else 0),.19,d+(.15 if k%2==0 else 0)),"timber")
    m.box((0,.15,0),(w+.11,.30,d+.13),"stone")
    for x in [-1.91,-.42,1.50]:
        window(m,x,1.56,z+.12,.92,1.17)
        for dx in [-.67,.67]:
            m.box((x+dx,1.59,z+.09),(.32,1.29,.09),"paint")
            for yy in [1.08,2.08]:m.box((x+dx,yy,z+.14),(.32,.065,.06),"cream")
        m.box((x,2.24,z+.13),(1.35,.15,.19),"cream")
        for i in range(6):
            xx=x-.58+i*.23
            m.face([(xx,2.17,z+.24),(xx+.11,2.03,z+.24),(xx+.20,2.17,z+.24)],"cream")
    roof(m,0,0,w+.66,d+.60,2.97,1.87,True)
    gable(m,-1.25,z,2.02,2.97,4.21,.38,"paint")
    m.panel(-1.25,3.30,z+.398,.65,.47,"glass")
    chimney(m,1.78,-.89,4.43)
    # Lean-to woodshed with a missing end, stacked split firewood.
    for x in [w/2+.22,w/2+1.68]:
        m.box((x,1.08,z-.54),(.13,2.16,.14),"timber")
    m.face([(w/2,2.70,z-.2),(w/2+1.95,2.14,z-.2),(w/2+1.95,2.14,-z),(w/2,2.70,-z)],"slate")
    for row in range(4):
        for col in range(5-row//2):
            x=w/2+.20+col*.23+row%2*.115
            m.spar((x,.18+row*.18,-.53),(x,.18+row*.18,.32+rng.random()*.16),.121,"timber",sides=6)
    m.save()

def tower():
    m=Model("water_tower"); n=12
    # Tapered brick pier, oversailing octagonal tank house, shallow copper cap.
    def belt(y0,r0,y1,r1,tile):
        for i in range(n):
            a,b=(np.array([i,i+1])+.5)*math.tau/n
            m.face([(r0*math.cos(b),y0,r0*math.sin(b)),(r0*math.cos(a),y0,r0*math.sin(a)),
                    (r1*math.cos(a),y1,r1*math.sin(a)),(r1*math.cos(b),y1,r1*math.sin(b))],tile)
    for k in range(6):belt(k,1.63-.04*k,k+1,1.63-.04*(k+1),"brick")
    for y,r in [(.32,1.70),(4.9,1.53),(5.55,1.55),(6.03,1.75),(6.26,2.08)]:
        belt(y,r,y+.16,r+.06,"stone")
    belt(6.13,1.59,6.70,2.22,"timber")
    for k in range(5):belt(6.70+k*.49,2.22,7.18+k*.49,2.22,"paint")
    for i in range(n):
        a=(i+.5)*math.tau/n; x,z=2.23*math.cos(a),2.23*math.sin(a)
        m.spar((x,6.74,z),(x,9.14,z),.062,"tar",sides=4)
    belt(9.14,2.47,9.32,2.54,"iron");belt(9.32,2.54,10.64,.27,"slate")
    m.spar((0,10.50,0),(0,11.38,0),.064,"iron",.023)
    for x in [-.23,.23]:m.spar((x,.16,1.72),(x,6.46,1.72),.035,"iron")
    for k in range(20):m.spar((-.24,.36+k*.30,1.72),(.24,.36+k*.30,1.72),.023,"iron")
    m.save()

def bus_stop():
    m=Model("bus_shelter")
    # Rolled barrel vault and open sides; cast concrete back panel with an enamel station plate.
    for i in range(9):
        a,b=np.array([i,i+1])*math.pi/9
        def archpoint(x,t):return (x,2.51+.39*math.sin(t),.99*math.cos(t))
        m.face([archpoint(-1.67,a),archpoint(1.67,a),archpoint(1.67,b),archpoint(-1.67,b)],"slate")
    for x in [-1.52,1.52]:
        for z in [-.81,.81]:m.spar((x,0,z),(x,2.57,z),.053,"paint")
    m.box((0,1.28,-.87),(3.17,2.46,.13),"chalk")
    m.box((0,.51,-.44),(2.49,.095,.47),"timber")
    for x in [-1.08,1.08]:m.box((x,.25,-.45),(.18,.5,.40),"stone")
    m.panel(0,1.89,-.79,2.33,.57,"stop")
    m.save()

def tree(name,kind):
    m=Model(name)
    h={"spruce":8.8,"alder":7.8,"birch":9.8}[kind]
    bark="birch" if kind=="birch" else "pine"
    trunk=[np.array([.11*math.sin(i*.85),h*i/5,.12*math.cos(i*.86)]) for i in range(6)]
    for i,(a,b) in enumerate(zip(trunk,trunk[1:])):
        m.spar(a,b,.217-.038*i,bark,.183-.034*i,7)
    for i in range(6):
        a=i*math.tau/6
        m.spar((0,.35,0),(.66*math.cos(a),.015,.66*math.sin(a)),.13,bark,.035,5)
    if kind=="spruce":
        for level in range(12):
            f=level/13; height=1.04+f*7.7; size=(1-f)**.65*2.12+.18
            for j in range(7):
                a=level*2.27+j*math.tau/7+rng.uniform(-.20,.20)
                direction=np.array([math.cos(a),0,math.sin(a)]); cross=np.array([-direction[2],0,direction[0]])
                base=np.array([.08,height,0]); end=base+direction*size*rng.uniform(.82,1.11)
                end[1]-=.23+.32*(1-f)
                if j%2==0:m.spar(base,end,.047*(1-f)+.01,bark,.012,5)
                for s in [-1,1]:
                    tip=end+direction*.13; flank=cross*s*size*.34
                    m.face([tip,tip+flank-[0,.21,0],base+flank*.21+[0,.41,0],base+[0,.45,0]],"needles","canopy")
    else:
        for i in range(20):
            height=h*(.28+.032*i)
            a=i*2.399+rng.uniform(-.30,.30)
            direction=np.array([math.cos(a),0,math.sin(a)])
            length=(2.31 if kind=="alder" else 1.95)*(1-.49*i/20)*rng.uniform(.82,1.21)
            base=np.array([.08,height,.04]); elbow=base+direction*length*.56+[0,.59,0]; end=base+direction*length+[0,1.03,0]
            m.spar(base,elbow,.094*(1-i/25),bark,.042,5); m.spar(elbow,end,.043,bark,.019,5)
            for j in range(5):
                aa=a+j*1.94; vec=np.array([math.cos(aa),0,math.sin(aa)])
                top=end+vec*rng.uniform(.24,.79); top[1]+=rng.uniform(-.15,.70)
                m.spar(end,top,.024,bark,.009,4)
                wide=np.array([-vec[2],0,vec[0]])*rng.uniform(.50,.86)
                m.face([top-wide-[0,.60,0],top+wide-[0,.60,0],top+wide*.79+[0,.62,0],top-wide*.79+[0,.62,0]],"leaves","canopy")
    m.save()

def verge():
    m=Model("verge")
    for i in range(10):
        a=rng.uniform(0,math.tau); width=rng.uniform(.17,.34); h=rng.uniform(.20,.54)
        base=np.array([rng.uniform(-.69,.69),.03,rng.uniform(-.43,.43)])
        across=np.array([math.cos(a),0,math.sin(a)])*width
        m.face([base-across,base+across,base+across+[.06,h,0],base-across+[.06,h,0]],"weeds","canopy")
    m.save()

if __name__=="__main__":
    from field_kit import build
    from roadside import build as build_roadside
    paint()
    build()
    build_roadside()
    post_office(); postal_door(); cottage(); tower(); bus_stop(); verge()
    for species in ["spruce","alder","birch"]:tree(species,species)
