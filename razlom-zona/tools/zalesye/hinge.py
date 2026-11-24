"""Original Foley: a short, loaded hinge with two uneven metal resonances."""
from pathlib import Path
from subprocess import run
import numpy as np
from scipy.signal import butter, sosfilt
rng=np.random.default_rng(718)
rate=32000
t=np.arange(int(rate*.74))/rate
strain=np.sin(np.pi*np.minimum(1,t/.70))**1.6
freq=231+76*np.sin(t*4.2)+21*np.sin(t*43)+4*np.sin(t*112)
phase=2*np.pi*np.cumsum(freq)/rate
noise=sosfilt(butter(3,[400,3800],fs=rate,btype="bandpass",output="sos"),rng.normal(size=t.size))
signal=strain*(.13*np.sin(phase)+.062*np.sin(2.87*phase)+.035*noise)*(.82+.18*np.sin(t*73))
signal+=.27*np.exp(-((t-.655)/.026)**2)*(np.sin(2*np.pi*t*104)+.15*noise)
run(["ffmpeg","-loglevel","error","-y","-f","f32le","-ar",str(rate),"-ac","1","-i","-","-c:a","libvorbis","-q:a","4",str(Path(__file__).resolve().parents[2]/"assets/zalesye/hinge.ogg")],input=signal.astype("<f4").tobytes(),check=True)
