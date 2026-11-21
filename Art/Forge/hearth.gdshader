shader_type spatial;
render_mode unshaded,cull_disabled,depth_draw_never;
uniform bool smoke=false;
uniform float offset=0.;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.54);}
float noise2(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1)),f.x),f.y);}
void fragment(){
	float h=1.-UV.y,t=TIME+offset;
	vec2 p=vec2(UV.x*4.,h*3.-t*(smoke?.48:2.8));
	float n=noise2(p)*.54+noise2(p*2.19)*.31+noise2(p*4.3)*.15;
	float x=abs(UV.x-.5+(n-.5)*(.15+h*.64));
	if(smoke){
		float d=(1.-smoothstep(.08,.46,x))*(1.-smoothstep(.54,1.,h))*smoothstep(0.,.23,h)*smoothstep(.28,.68,n);
		ALBEDO=vec3(.21,.22,.225);ALPHA=d*.37;
	}else{
		float f=1.-smoothstep(.02,.08,.96*h+x*1.91+.27-1.42*n);
		ALBEDO=mix(vec3(1.,.18,.018),vec3(1.,.83,.35),clamp((1.-h)*n*1.9,0.,1.));
		ALPHA=f*smoothstep(.0,.07,h)*(1.-smoothstep(.84,1.,h));
	}
}
