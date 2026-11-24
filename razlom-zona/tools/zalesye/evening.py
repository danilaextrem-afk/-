"""Bake a seamless, original broken cloud deck on the sphere for the GLES sky."""
from pathlib import Path
import numpy as np
from scipy.ndimage import map_coordinates, gaussian_filter
from PIL import Image

rng=np.random.default_rng(338)
h,w=512,1024
y,x=np.mgrid[0:h,0:w]
theta=(x+.5)/w*np.pi*2-np.pi
elev=(.5-(y+.5)/h)*np.pi
dx=np.cos(elev)*np.cos(theta); dy=np.sin(elev); dz=np.cos(elev)*np.sin(theta)
def field(k):
    volume=gaussian_filter(rng.normal(size=(48,48,48)),1.0,mode="wrap")
    return map_coordinates(volume/volume.std(),[20+dx*k,20+dy*k*2.7,20+dz*k],order=1,mode="wrap")
def smooth(a,b,x):
    u=np.clip((x-a)/(b-a),0,1); return u*u*(3-2*u)
def mix(a,b,t):return a*(1-t[...,None])+b*t[...,None]
deck=field(4)*.55+field(11)*.29+field(25)*.14+field(51)*.07
gap=np.exp(-((dy-.11)/.145)**2)*smooth(-.08,.93,dx)
density=smooth(-.67,.69,deck+.34-gap*1.25)
horizon=np.exp(-abs(dy-.015)*3.9)
sky=mix(np.array([41,56,76]),np.array([151,148,148]),horizon)
ember=gap*(.57+.37*smooth(-.6,.5,deck))
sky=mix(sky,np.array([236,175,119]),ember)
height=smooth(-.75,.75,deck)
cloud=mix(np.array([75,87,103]),np.array([34,43,59]),height)
cloud=mix(cloud,np.array([163,127,111]),horizon*gap*.64)
sky=mix(sky,cloud,density*.91)
sky=mix(sky,np.array([109,123,127]),smooth(.015,.14,-dy))
a=np.asarray(np.clip(sky,0,255),dtype=np.uint8)
Image.fromarray(a).save(Path(__file__).resolve().parents[2]/"assets/zalesye/evening.jpg",quality=93)
