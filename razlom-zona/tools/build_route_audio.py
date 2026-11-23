"""Original cyclic wind bed, electrical call and contact closure. No sample packs."""
from pathlib import Path
from subprocess import run
import numpy as np
from scipy.signal import butter,sosfilt
rate=22050
rng=np.random.default_rng(1107)
out=Path(__file__).resolve().parents[1]/'assets/sound'
def band(signal,lo,hi):
    return sosfilt(butter(3,[lo,hi],fs=rate,btype='bandpass',output='sos'),signal)
def save(name,x):
    x=np.clip(x,-1,1).astype('<f4')
    run(['ffmpeg','-loglevel','error','-y','-f','f32le','-ar',str(rate),'-ac','1','-i','-','-c:a','libvorbis','-q:a','3',str(out/(name+'.ogg'))],input=x.tobytes(),check=True)
t=np.arange(rate*12)/rate
n=rng.normal(size=len(t))
wind=band(n,65,460)*(.26+.11*np.sin(t*2*np.pi/12))+band(n,950,2600)*(.032+.023*np.sin(t*4*np.pi/12+1))
wind+=.024*np.sin(2*np.pi*74*t+.6*np.sin(t*2*np.pi/12))+.010*np.sin(2*np.pi*223*t)
# Crossfade the boundary before Vorbis loop wrapping.
fade=np.linspace(0,1,rate//2);wind[:len(fade)]=wind[-len(fade):]*(1-fade)+wind[:len(fade)]*fade
save('wind_in_wire',wind[:-len(fade)])
t=np.arange(int(rate*2.8))/rate
radio=np.zeros(len(t))
for start,duration,freq in [(.12,.18,530),(.51,.18,530),(.91,.49,715),(1.76,.12,433)]:
    env=np.clip((t-start)*140,0,1)*np.clip((start+duration-t)*80,0,1)
    radio+=env*.15*(np.sin(2*np.pi*freq*t)+.3*np.sin(2*np.pi*(2*freq+1.7)*t))
radio+=band(rng.normal(size=len(t)),620,2200)*.075*np.sin(t*np.pi/t[-1])**3
save('relay_call',radio)
t=np.arange(int(rate*.38))/rate
save('relay_contact',.17*np.exp(-19*t)*np.sin(2*np.pi*880*t)+.30*np.exp(-92*t)*band(rng.normal(size=len(t)),170,3900))
