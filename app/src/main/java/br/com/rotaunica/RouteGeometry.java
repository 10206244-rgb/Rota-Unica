package br.com.rotaunica;

/** Immutable geometry index. Distances are metres, bearings clockwise from north. */
final class RouteGeometry {
    final double[] lat,lon,along;
    RouteGeometry(double[] latitude,double[] longitude) {
        if(latitude.length!=longitude.length)throw new IllegalArgumentException("coordinates");
        lat=latitude.clone();lon=longitude.clone();along=new double[lat.length];
        for(int i=1;i<lat.length;i++)along[i]=along[i-1]+distance(lat[i-1],lon[i-1],lat[i],lon[i]);
    }
    static double wrap(double d){return ((d+540)%360+360)%360-180;}
    static double distance(double a,double b,double c,double d){
        double u=Math.toRadians(c-a),v=Math.toRadians(wrap(d-b));
        double h=Math.sin(u/2)*Math.sin(u/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(v/2)*Math.sin(v/2);
        return 12742000*Math.asin(Math.sqrt(Math.max(0,Math.min(1,h))));
    }
    double[] project(double y,double x){
        if(lat.length<2)return new double[]{0,Double.POSITIVE_INFINITY,0,0};
        double scale=Math.max(.00001,Math.cos(Math.toRadians(y))),best=Double.POSITIVE_INFINITY,offset=0,bearing=0;
        for(int i=1;i<lat.length;i++){
            double ax=wrap(lon[i-1]-x)*scale,ay=lat[i-1]-y,bx=ax+wrap(lon[i]-lon[i-1])*scale,by=lat[i]-y;
            double dx=bx-ax,dy=by-ay,den=dx*dx+dy*dy;
            if(den<1e-20)continue;
            double t=Math.max(0,Math.min(1,-(ax*dx+ay*dy)/den));
            double qx=ax+t*dx,qy=ay+t*dy,sq=qx*qx+qy*qy;
            if(sq<best){best=sq;offset=along[i-1]+t*(along[i]-along[i-1]);bearing=(Math.toDegrees(Math.atan2(dx,dy))+360)%360;}
        }
        return new double[]{offset,Math.sqrt(best)*111195.08,along[along.length-1],bearing};
    }
}
