package br.com.rotaunica;

/** Pure navigation policies, shared by both map engines. Distances in metres. */
final class NavigationRules {
 static double angleDifference(double a,double b){return Math.abs(((a-b+180)%360+360)%360-180);}
 static boolean bumpOnRoute(double along,double lateral,double total,double start,double course,double direction){
  return Double.isFinite(along)&&Double.isFinite(lateral)&&Double.isFinite(total)&&Double.isFinite(start)&&lateral>=0&&lateral<=18&&along>=start-8&&along<=total+2&&(Double.isNaN(direction)||angleDifference(course,direction)<=65);
 }
 static boolean showBump(boolean driving,double along,double lateral,double total,double start,double course,double direction){
  return driving&&bumpOnRoute(along,lateral,total,start,course,direction)&&along<=start+2500;
 }
 static long recentTimestamp(long published,long now){
  return published>0&&published<=now+60000&&now-published<=900000?published:0;
 }
 static int sedanDirection(double degrees,int steps){
  return ((int)Math.round(degrees/360*steps)%steps+steps)%steps;
 }
}
