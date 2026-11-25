package br.com.rotaunica;

/** Visual interpolation only. Routing and safety decisions continue to use accepted GPS fixes. */
final class NavigationMotion {
 static final class Pose {
  final double lat,lon,bearing;
  Pose(double lat,double lon,double bearing){this.lat=lat;this.lon=lon;this.bearing=bearing;}
 }
 private Pose start,end;
 private long began,duration,lastFix;
 private double speed=Double.NaN;
 private long speedAt;
 private int stopped;
 static double wrap(double a){return ((a+180)%360+360)%360-180;}
 static double angle(double a,double b,double t){return (a+wrap(b-a)*t+360)%360;}
 static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
 Pose at(long now){
  if(end==null)return null;
  double t=duration<=0?1:clamp((double)(now-began)/duration,0,1);
  return new Pose(start.lat+(end.lat-start.lat)*t,wrap(start.lon+wrap(end.lon-start.lon)*t),angle(start.bearing,end.bearing,t));
 }
 void position(double lat,double lon,double bearing,long now,boolean reset){
  Pose target=new Pose(lat,lon,bearing);
  if(end==null||reset||now-lastFix>5000){start=end=target;began=now;duration=0;}
  else{start=at(now);end=target;began=now;duration=(long)clamp((now-lastFix)*.94,250,1400);}
  lastFix=now;
 }
 void heading(double bearing,long now){
  if(end==null)return;
  Pose p=at(now);
  // Do not restart the location interpolation when the compass changes.
  start=new Pose(start.lat,start.lon,bearing);end=new Pose(end.lat,end.lon,bearing);
 }
 void speedSample(double measured,double uncertainty,long now){
  if(!Double.isFinite(measured)||measured<0||measured>100||!Double.isFinite(uncertainty)||uncertainty>3.0)return;
  double v=measured;
  if(v<.45&&uncertainty<=1.5){stopped++;if(stopped>=2)v=0;}else stopped=0;
  if(!Double.isFinite(speed)||now-speedAt>3500||v==0&&stopped>=2)speed=v;
  else{double dt=clamp((now-speedAt)/1000.0,.05,2);double alpha=1-Math.exp(-dt/.36);speed+=alpha*(v-speed);}
  speedAt=now;
 }
 double speed(long now){return speedAt==0||now-speedAt>3500?Double.NaN:speed;}
 static boolean plausible(double distance,double seconds,double beforeSpeed,double afterSpeed,double beforeAccuracy,double afterAccuracy){
  if(seconds<=0)return false;
  double v=Math.max(Double.isFinite(beforeSpeed)?beforeSpeed:0,Double.isFinite(afterSpeed)?afterSpeed:0);
  double error=Math.max(3,beforeAccuracy)+Math.max(3,afterAccuracy);
  double bound=Math.max(45,Math.min(105,v+12))*seconds+error;
  return distance<=bound;
 }
}
