"""Drainage, hand-laid kerbs and quiet pools in the low wheel ruts."""
import math
import random
import numpy as np
from mesh import Model

def build():
    r=random.Random(891)
    shore=Model("drainage"); water=Model("standing_water")
    def stone(x,y,z,w,h,d):
        rings=[]
        for ring_height,scale in [(0,.88),(.25,1),(1,.8)]:
            ring=[]
            for j in range(7):
                a=j*math.tau/7; k=r.uniform(.83,1.12)*scale
                ring.append((x+k*w*math.cos(a),y+h*ring_height,z+k*d*math.sin(a)))
            rings.append(ring)
        for lower,upper in zip(rings,rings[1:]):
            for j in range(7):shore.face([lower[(j+1)%7],lower[j],upper[j],upper[(j+1)%7]],"stone")
        for j in range(7):shore.face([rings[-1][j],(x,y+h+.006,z),rings[-1][(j+1)%7]],"stone")
    # Raised broken kerb protects the bus stop. Leave the entrances flush.
    for side,a,b in [(1,-21,-15),(1,-9.7,-7.5),(-1,-12,-7.8),(-1,-25,-20)]:
        z=a
        while z<b:
            length=r.uniform(.45,.95);x=side*(3.03+.07*math.sin(z))
            stone(x,.04,z,.13,r.uniform(.10,.19),length/2)
            z+=length+.03
    for i in range(89):
        z=r.uniform(-44,3);x=r.choice([-1,1])*r.uniform(2.5,3.4)
        s=r.uniform(.034,.115)
        stone(x,.069,z,s,r.uniform(.023,.065),s*r.uniform(.7,1.6))
    # Fractured footway leading over the gutter to the post-office door.
    for i in range(3):
        for j in range(5):
            stone(4.35+j*.47,.045,-4.5+i*.42+j*.15,.25,.046,.22)
    for x,z,width,length in [(-1.12,-2.0,.53,2.9),(1.18,-5.3,.54,3.5),
            (-1.01,-9.1,.47,2.1),(-.22,-13,.72,2.4),(.9,-20,.64,3.0),
            (-1.06,-26,.55,3.3),(1.14,-35,.46,2.11),(-1.24,-40,.55,2.5)]:
        pts=[]
        for i in range(32):
            a=i*math.tau/32; rad=.91+.10*math.sin(a*3+.8)+r.uniform(-.07,.07)
            pts.append((x+math.cos(a)*width*rad+.095*math.sin(a*2),.080,z+math.sin(a)*length*rad))
        for i in range(len(pts)):
            water.face([pts[i],(x,.080,z),pts[(i+1)%len(pts)]],"glass")
    shore.save();water.save()
