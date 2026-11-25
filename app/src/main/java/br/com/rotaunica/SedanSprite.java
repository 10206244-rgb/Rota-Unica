package br.com.rotaunica;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.content.res.Resources;
import java.util.*;

/** Original low-poly blue sedan, projected from a mesh into direction-specific map sprites. */
final class SedanSprite {
 static final int STEPS=24;
 static final class Face { double[][] vertices; int color; Face(int color,double[]... v){this.color=color;vertices=v;} }
 static double[] v(double x,double y,double z){return new double[]{x,y,z};}
 static void box(List<Face> f,double x,double y,double z,double w,double l,double h,int top,int side,int back){
 double a=x-w/2,b=x+w/2,c=y-l/2,d=y+l/2,e=z+h;
 double[] A=v(a,c,z),B=v(b,c,z),C=v(b,d,z),D=v(a,d,z),E=v(a,c,e),F=v(b,c,e),G=v(b,d,e),H=v(a,d,e);
 f.add(new Face(top,E,F,G,H));f.add(new Face(side,A,D,H,E));f.add(new Face(side,B,C,G,F));f.add(new Face(back,A,B,F,E));f.add(new Face(side,D,C,G,H));
 }
 static BitmapDrawable draw(Resources res,int direction,boolean perspective){
 int size=112;Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(3);
 double yaw=2*Math.PI*direction/STEPS,cs=Math.cos(yaw),sn=Math.sin(yaw),pitch=Math.toRadians(perspective?48:15),cp=Math.cos(pitch),sp=Math.sin(pitch),scale=19.0;
 p.setColor(0x38000000);c.drawOval(19,45,93,91,p);
 ArrayList<Face> faces=new ArrayList<>();
 for(double x:new double[]{-1.04,1.04})for(double y:new double[]{-1.42,1.42})box(faces,x,y,.10,.28,.68,.53,0xff28333c,0xff10191f,0xff121c23);
 box(faces,0,0,.25,2.05,4.15,.55,0xff298bf0,0xff125aa9,0xff114987);
 box(faces,0,.04,.80,1.75,3.92,.16,0xff5eaaff,0xff1767b8,0xff1556a0);
 double[] a=v(-.84,-1.05,.96),d=v(.84,-1.05,.96),e=v(.84,1.03,.96),g=v(-.84,1.03,.96);
 double[] A=v(-.68,-.62,1.60),D=v(.68,-.62,1.60),E=v(.68,.62,1.60),G=v(-.68,.62,1.60);
 faces.add(new Face(0xff8dc6ff,A,D,E,G));faces.add(new Face(0xff193a52,a,d,D,A));faces.add(new Face(0xff2d536c,g,e,E,G));faces.add(new Face(0xff264960,a,g,G,A));faces.add(new Face(0xff264960,d,e,E,D));
 box(faces,-.63,2.035,.54,.43,.08,.17,0xffe8f8ff,0xffd4edff,0xffd4edff);box(faces,.63,2.035,.54,.43,.08,.17,0xffe8f8ff,0xffd4edff,0xffd4edff);
 box(faces,-.66,-2.04,.54,.42,.09,.15,0xffff6868,0xffbe2535,0xffe94545);box(faces,.66,-2.04,.54,.42,.09,.15,0xffff6868,0xffbe2535,0xffe94545);
 box(faces,0,-2.085,.46,.48,.04,.12,0xffedf3f5,0xffc5d0d5,0xffe4edf0);
 faces.sort(Comparator.comparingDouble(f->{double depth=0;for(double[] q:f.vertices)depth+=-(q[1]*cs-q[0]*sn)*sp+q[2]*cp;return depth/f.vertices.length;}));
 for(Face f:faces){Path path=new Path();boolean first=true;for(double[] q:f.vertices){double x=q[0]*cs+q[1]*sn,along=q[1]*cs-q[0]*sn;float px=(float)(56+x*scale),py=(float)(65-along*cp*scale-q[2]*sp*scale);if(first){path.moveTo(px,py);first=false;}else path.lineTo(px,py);}path.close();p.setStyle(Paint.Style.FILL);p.setColor(f.color);c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.65f);p.setColor(0x55213c51);c.drawPath(path,p);}
 p.setStyle(Paint.Style.FILL);b.setDensity(160);return new BitmapDrawable(res,b);
 }
 static int direction(double degrees){return NavigationRules.sedanDirection(degrees,STEPS);}
}
