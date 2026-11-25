package br.com.rotaunica;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import org.json.*;
import org.maplibre.android.MapLibre;
import org.maplibre.android.maps.*;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.expressions.Expression;
import static org.maplibre.android.style.layers.PropertyFactory.*;
import org.maplibre.geojson.Feature;
import org.osmdroid.util.GeoPoint;
import java.util.*;

/** Native pitched vector camera. The old raster map remains a non-inclined fallback. */
final class VectorNavigation {
    final MainActivity a;
    final MapView view;
    MapLibreMap map;
    Style style;
    boolean ready, dark, destroyed;
    int generation;
    final Set<String> installed = new HashSet<>();
    final String EMPTY = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    List<MainActivity.Place> places = Collections.emptyList();
    String category;
    VectorNavigation(MainActivity a, FrameLayout root, Bundle state) {
        this.a=a;
        MapLibre.getInstance(a);
        view=new MapView(a,new MapLibreMapOptions().textureMode(true));
        view.setVisibility(View.INVISIBLE);
        root.addView(view,1,new FrameLayout.LayoutParams(-1,-1));
        view.onCreate(state);
        view.addOnDidFailLoadingMapListener(error -> fallback());
        view.getMapAsync(m -> {
            if(destroyed)return;
            map=m;
            map.getUiSettings().setCompassEnabled(false);
            map.addOnCameraMoveStartedListener(reason -> {
                if(reason==MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE && a.driving)a.followEnabled=false;
            });
            map.addOnMapClickListener(p -> {
                if(!ready)return false;
                try{
                    List<Feature> hits=map.queryRenderedFeatures(map.getProjection().toScreenLocation(p),"ru-car","ru-points","ru-dest","ru-alt");
                    if(!hits.isEmpty()){
                        Feature f=hits.get(0);
                        if(f.hasProperty("route")&&!a.driving){
                            int i=f.getNumberProperty("route").intValue();
                            if(i>=0&&i<a.routes.size()){a.selected=i;a.stepIndex=1;a.drawRoutes();a.renderRoute();return true;}
                        }
                        if(f.hasProperty("label")){a.toast(f.getStringProperty("label"));return true;}
                    }
                }catch(Exception ignored){}
                return false;
            });
            loadStyle(a.dark);
        });
    }
    void fallback(){if(destroyed)return;ready=false;style=null;view.setVisibility(View.INVISIBLE);a.map.setVisibility(View.VISIBLE);}
    boolean active(){return ready&&!destroyed;}
    void setTheme(boolean value){if(map!=null&&(style==null||dark!=value))loadStyle(value);}
    void loadStyle(boolean value){
        dark=value;final int token=++generation;
        ready=false;style=null;installed.clear();
        a.map.setVisibility(View.VISIBLE);view.setVisibility(View.INVISIBLE);
        map.setStyle(new Style.Builder().fromUri(value?"https://tiles.openfreemap.org/styles/fiord":"https://tiles.openfreemap.org/styles/liberty"),s->{
            if(destroyed||token!=generation)return;
            try{
                style=s;
                for(String id:new String[]{"alt","route","jams","points","dest","car"})s.addSource(new GeoJsonSource("ru-"+id,EMPTY));
                s.addLayer(new LineLayer("ru-alt","ru-alt").withProperties(lineColor(0xff88949b),lineWidth(5f),lineCap("round"),lineJoin("round")));
                s.addLayer(new LineLayer("ru-route-border","ru-route").withProperties(lineColor(0xff075e49),lineWidth(10f),lineCap("round"),lineJoin("round")));
                s.addLayer(new LineLayer("ru-route","ru-route").withProperties(lineColor(0xff10b981),lineWidth(7f),lineCap("round"),lineJoin("round")));
                s.addLayer(new LineLayer("ru-jams","ru-jams").withProperties(lineColor(Expression.get("color")),lineWidth(5f),lineCap("round"),lineJoin("round")));
                s.addLayer(new SymbolLayer("ru-points","ru-points").withProperties(iconImage(Expression.get("icon")),iconSize(.85f),iconAllowOverlap(false),iconIgnorePlacement(false),iconPitchAlignment("viewport")));
                s.addLayer(new SymbolLayer("ru-dest","ru-dest").withProperties(iconImage(Expression.get("icon")),iconSize(1f),iconAnchor("bottom"),iconAllowOverlap(true),iconPitchAlignment("viewport")));
                s.addLayer(new SymbolLayer("ru-car","ru-car").withProperties(iconImage("ru-car-icon"),iconSize(1.15f),iconRotate(Expression.get("heading")),iconRotationAlignment("map"),iconPitchAlignment("viewport"),iconAllowOverlap(true),iconIgnorePlacement(true)));
                image("ru-car-icon",a.mapIcon("car",0xff087e65));
                ready=true;routes();events();traffic();position();
                if(a.driving&&a.loc!=null)camera(a.gp(a.loc),a.heading,a.tiltEnabled,true);
                else if(!a.routes.isEmpty())fit(a.routes.get(a.selected).points);
                else if(a.loc!=null)camera(a.gp(a.loc),0,false,false);
                else map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-15.5,-49),4));
                view.setVisibility(View.VISIBLE);a.map.setVisibility(View.INVISIBLE);
            }catch(Exception ex){fallback();}
        });
    }
    void image(String name,Drawable d){
        if(style==null||installed.contains(name))return;
        int w=Math.max(1,d.getIntrinsicWidth()),h=Math.max(1,d.getIntrinsicHeight());
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);d.setBounds(0,0,w,h);d.draw(c);
        style.addImage(name,b);installed.add(name);
    }
    JSONObject props(Object... pairs)throws JSONException{JSONObject p=new JSONObject();for(int i=0;i<pairs.length;i+=2)p.put((String)pairs[i],pairs[i+1]);return p;}
    JSONArray coords(GeoPoint p)throws JSONException{return new JSONArray().put(p.getLongitude()).put(p.getLatitude());}
    JSONObject feature(String type,Object geometry,JSONObject p)throws JSONException{return new JSONObject().put("type","Feature").put("geometry",new JSONObject().put("type",type).put("coordinates",geometry)).put("properties",p);}
    JSONObject line(List<GeoPoint> list,JSONObject p)throws JSONException{JSONArray c=new JSONArray();for(GeoPoint g:list)c.put(coords(g));return feature("LineString",c,p);}
    void source(String id,JSONArray f){if(!active())return;try{GeoJsonSource src=style.getSourceAs("ru-"+id);if(src!=null)src.setGeoJson(new JSONObject().put("type","FeatureCollection").put("features",f).toString());}catch(Exception ignored){}}
    void routes(){if(!active())return;JSONArray alt=new JSONArray(),chosen=new JSONArray(),dest=new JSONArray();try{
        for(int i=0;i<a.routes.size();i++){MainActivity.Route r=a.routes.get(i);if(r.points.size()<2)continue;if(i==a.selected)chosen.put(line(r.points,props("route",i)));else if(!a.driving)alt.put(line(r.points,props("route",i)));}
        image("ru-dest-icon",a.mapIcon("pin",0xff087e65));if(a.destination!=null)dest.put(feature("Point",coords(a.destination.p),props("icon","ru-dest-icon","label",a.destination.name)));
        for(MainActivity.Place stop:a.stops)dest.put(feature("Point",coords(stop.p),props("icon","ru-dest-icon","label",stop.name)));
    }catch(Exception ignored){}source("alt",alt);source("route",chosen);source("dest",dest);}
    void events(){if(!active())return;JSONArray all=new JSONArray();try{
        ArrayList<MainActivity.Event> data=new ArrayList<>(a.fixed);data.addAll(a.live);long now=System.currentTimeMillis();GeoPoint here=a.loc==null?null:a.gp(a.loc);
        for(MainActivity.Event e:data){if(e.expires<now||here==null||here.distanceToAsDouble(e.p)>22000)continue;
            String kind=e.kind;String id;Drawable d;
            if(kind.equals("radar")){if(e.limit>0){int n=(int)e.limit;id="radar-"+n;d=a.symbol(String.valueOf(n),0xffb91c1c);}else{id=e.label.contains("sinal")?"redcamera":"camera";d=a.mapIcon(id,0xffb91c1c);}}
            else if(kind.equals("semaforo")){id="traffic";d=a.trafficIcon();}
            else if(kind.equals("lombada")){id="bump";d=a.mapIcon("bump",0xffd97706);}
            else if(kind.equals("historico")){id="historical";d=a.mapIcon("camera",0xff64748b);}
            else{id="hazard";d=a.mapIcon("hazard",0xffd97706);}
            image(id,d);all.put(feature("Point",coords(e.p),props("icon",id,"label",e.label+(e.limit>0?" · "+(int)e.limit+" km/h":"")+" · "+e.source)));
        }
        String k="fuel".equals(category)?"fuel":"bus_stop".equals(category)?"bus":"pin";String id="poi-"+k;image(id,a.mapIcon(k,0xff087e65));for(MainActivity.Place p:places)all.put(feature("Point",coords(p.p),props("icon",id,"label",p.name+" · "+p.detail)));
    }catch(Exception ignored){}source("points",all);}
    void pois(List<MainActivity.Place> p,String c){places=c==null?Collections.emptyList():new ArrayList<>(p);category=c;events();}
    void traffic(){if(!active())return;JSONArray f=new JSONArray();try{for(MainActivity.Jam j:a.jams){if(j.expires<System.currentTimeMillis()||j.points.size()<2)continue;String color=j.level>=5?"#991b1b":j.level>=4?"#ef4444":j.level==3?"#f97316":"#eab308";f.put(line(j.points,props("color",color)));}}catch(Exception ignored){}source("jams",f);}
    void position(){if(!active())return;JSONArray f=new JSONArray();try{if(a.loc!=null)f.put(feature("Point",coords(a.gp(a.loc)),props("heading",a.driving?a.heading:0,"label","Sua localização")));}catch(Exception ignored){}source("car",f);}
    void camera(GeoPoint p,float bearing,boolean tilt,boolean drive){if(!active())return;position();if(drive&&!a.followEnabled)return;
        double[] padding=drive?new double[]{0,a.dp(110),0,a.dp(320)}:new double[]{0,0,0,0};
        CameraPosition c=new CameraPosition.Builder().target(new LatLng(p.getLatitude(),p.getLongitude())).zoom(drive?17.4:15).bearing(drive?bearing:0).tilt(drive&&tilt?48:0).padding(padding).build();
        map.easeCamera(CameraUpdateFactory.newCameraPosition(c),550);
    }
    void fit(List<GeoPoint> points){if(!active()||points.size()<2)return;try{LatLngBounds.Builder b=new LatLngBounds.Builder();for(GeoPoint p:points)b.include(new LatLng(p.getLatitude(),p.getLongitude()));map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(map.getCameraPosition()).bearing(0).tilt(0).padding(new double[]{0,0,0,0}).build()));map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(),a.dp(35),a.dp(105),a.dp(35),a.dp(260)),650);}catch(Exception ignored){}}
    void start(){view.onStart();}
    void resume(){view.onResume();}
    void pause(){view.onPause();}
    void stop(){view.onStop();}
    void save(Bundle state){view.onSaveInstanceState(state);}
    void lowMemory(){view.onLowMemory();}
    void destroy(){destroyed=true;view.onDestroy();}
}
