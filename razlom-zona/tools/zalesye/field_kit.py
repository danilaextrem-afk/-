"""Sewn glove with a stamped field lamp, first-aid pouch, battery and dynamo."""
import math
import numpy as np
from mesh import Model

def oval(m,start,end,rings,tile):
    a,b=np.array(start),np.array(end); forward=b-a; forward/=np.linalg.norm(forward)
    u=np.cross(forward,[0,1,0]); u/=np.linalg.norm(u); v=np.cross(forward,u)
    previous=None
    for t,rx,ry in rings:
        points=[a+(b-a)*t+math.cos(i*math.tau/12)*rx*u+math.sin(i*math.tau/12)*ry*v for i in range(12)]
        if previous is not None:
            for i in range(12):
                j=(i+1)%12;m.face([previous[i],previous[j],points[j],points[i]],tile)
        previous=points

def build():
    m=Model("field_lamp")
    a=(.143,-.165,.32);b=(.006,-.071,.062)
    oval(m,a,b,[(0,.096,.078),(.16,.093,.078),(.40,.079,.071),(.51,.083,.074),(.66,.067,.061),(.8,.072,.060),(1,.055,.047)],"canvas")
    oval(m,(.035,-.090,.125),(.007,-.069,.072),[(0,.070,.064),(.14,.073,.065),(.87,.064,.053),(1,.06,.052)],"leather")
    oval(m,(.012,-.067,.095),(-.009,-.050,-.051),[(0,.045,.035),(.2,.050,.039),(.46,.054,.040),(.76,.047,.038),(1,.040,.029)],"leather")
    for i in range(4):
        z=.061-i*.028
        p=(.013,-.069,z); q=(.045,-.052,z-.001); r=(.049,-.021,z-.003); s=(.024,-.006,z-.009)
        m.spar(p,q,.016,"leather",.016,8); m.spar(q,r,.016,"leather",.014,8); m.spar(r,s,.014,"leather",.010,8)
    m.spar((-.024,-.069,.066),(-.048,-.034,.022),.019,"leather",.018,8)
    m.spar((-.048,-.034,.022),(-.038,-.015,-.011),.018,"leather",.014,8)
    for dx in [-.018,.002,.022]:
        m.spar((.040+dx,-.108,.206),(.007+dx,-.070,.112),.0028,"seam",.0020,4)
    # Heavy barrel, flared head, rolled bezel and alternating knurled grip rings.
    for z0,z1,r0,r1,tile in [(.106,.094,.037,.042,"iron"),(.094,.072,.042,.038,"lampmetal"),
            (.072,-.065,.038,.038,"tar"),(-.065,-.089,.041,.050,"lampmetal"),
            (-.089,-.130,.050,.06,"lampmetal"),(-.130,-.162,.060,.060,"lampmetal"),
            (-.162,-.172,.062,.063,"iron"),(-.172,-.177,.063,.057,"lampmetal"),
            (-.177,-.178,.054,.054,"glass")]:
        m.spar((0,0,z0),(0,0,z1),r0,tile,r1,20)
    for z in [.065,.051,.037,.023,.009,-.005,-.019,-.033,-.047]:
        m.spar((0,0,z),(0,0,z-.005),.040,"lampmetal",sides=16)
    for i in range(12):
        a=i*math.tau/12; dx=.058*math.cos(a);dy=.058*math.sin(a)
        m.spar((dx*.92,dy*.92,-.109),(dx,dy,-.149),.0037,"tar",sides=5)
    m.box((0,.037,-.020),(.023,.012,.034),"iron")
    m.save()
    med=Model("medkit")
    med.box((0,.098,0),(.31,.18,.19),"canvas")
    med.box((0,.17,.026),(.33,.072,.166),"canvas")
    for x in [-.099,.099]:
        med.box((x,.10,.100),(.035,.16,.013),"leather")
        med.box((x,.105,.114),(.042,.037,.013),"brass")
    med.box((0,.13,.116),(.094,.061,.008),"cream")
    med.box((0,.13,.122),(.019,.048,.007),"red")
    med.box((0,.13,.125),(.053,.015,.007),"red")
    med.save()
    batt=Model("battery")
    for x in [-.046,.046]:
        batt.spar((x,.017,0),(x,.16,0),.038,"iron",sides=12)
        batt.spar((x,.049,0),(x,.127,0),.039,"red",sides=12)
        batt.spar((x,.16,0),(x,.174,0),.016,"brass",sides=12)
    batt.save()
    scrap=Model("scrap")
    scrap.spar((-.13,.076,0),(.12,.076,0),.073,"iron",sides=12)
    for x in [-.07,-.04,-.01,.02,.05,.08]:scrap.spar((x,.076,0),(x+.012,.076,0),.080,"brass",sides=12)
    scrap.spar((.13,.076,0),(.18,.076,0),.026,"tar",sides=8)
    scrap.save()
