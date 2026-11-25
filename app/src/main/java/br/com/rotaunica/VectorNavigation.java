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
    boolean ready, dark, destroyed,offlineMode,offlineFailed,offlinePreparing; long lastConnectivityCheck=0; final java.util.concurrent.ExecutorService mapFiles=java.util.concurrent.Executors.newSingleThreadExecutor();
    int generation; long lastCameraAt=0; double lastZoom=17.2,lastTilt=0; float lastBearing=0; GeoPoint lastTarget; boolean lastDrive=false; android.os.Handler ui=new android.os.Handler(android.os.Looper.getMainLooper());
    final Set<String> installed = new HashSet<>(); long lastEventDraw=0; int lastSprite=-1; boolean lastSpritePitch=false;
    final String EMPTY = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    List<MainActivity.Place> places = Collections.emptyList();
    String category;
    VectorNavigation(MainActivity a, FrameLayout root, Bundle state) {
        this.a=a;
        MapLibre.getInstance(a);
        view=new MapView(a,new MapLibreMapOptions().textureMode(true));
        view.setAlpha(0f);
        root.addView(view,1,new FrameLayout.LayoutParams(-1,-1));
        view.onCreate(state);
        view.addOnDidFailLoadingMapListener(error -> fallback());
        view.getMapAsync(m -> {
            if(destroyed)return;
            map=m;
            map.getUiSettings().setCompassEnabled(false);map.setMaxPitchPreference(60);map.setPrefetchZoomDelta(3);map.getUiSettings().setRotateGesturesEnabled(true);map.getUiSettings().setTiltGesturesEnabled(true);
            map.addOnCameraMoveStartedListener(reason -> {
                if(reason==MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE && a.driving){a.followEnabled=false;lastTarget=null;}
            });
            map.addOnCameraIdleListener(()->{if(active())position();});map.addOnMapClickListener(p -> {
                if(!ready)return false;
                try{
                    List<Feature> hits=map.queryRenderedFeatures(map.getProjection().toScreenLocation(p),"ru-car","ru-radars","ru-bumps","ru-points","ru-dest","ru-alt");
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
    void fallback(){if(destroyed)return;ready=false;style=null;view.setVisibility(View.INVISIBLE);a.map.setVisibility(View.VISIBLE);a.map.setRotationX(0);a.map.setScaleX(1);a.map.setScaleY(1);if(offlineMode){offlineMode=false;offlineFailed=true;}else if(!offlineFailed&&!offlinePreparing&&a.loc!=null&&OfflineMapPack.covers(a.gp(a.loc))&&OfflineMapPack.available(a)){loadOffline(a.dark);return;}a.drawEvents();a.drawJams();if(a.loc!=null&&a.driving)a.followCamera(a.loc);}
    boolean active(){return ready&&!destroyed;}
    void setTheme(boolean value){if(map!=null&&(style==null||dark!=value)){if(offlineMode&&!offlineFailed)loadOffline(value);else loadStyle(value);}}
    void loadStyle(boolean value){offlineMode=false;offlinePreparing=false;loadBuilder(value,new Style.Builder().fromUri(value?"https://tiles.openfreemap.org/styles/fiord":"https://tiles.openfreemap.org/styles/liberty"));}
    void loadOffline(boolean value){if(map==null||destroyed||offlinePreparing)return;offlinePreparing=true;ready=false;int token=++generation;mapFiles.execute(()->{try{String json=OfflineMapPack.style(a,value);ui.post(()->{if(destroyed||token!=generation)return;offlinePreparing=false;offlineMode=true;loadBuilder(value,new Style.Builder().fromJson(json));});}catch(Exception e){ui.post(()->{if(destroyed||token!=generation)return;offlinePreparing=false;offlineFailed=true;fallback();});}});}
    void checkConnectivity(){if(map==null||destroyed||a.loc==null||offlinePreparing)return;long now=android.os.SystemClock.elapsedRealtime();if(now-lastConnectivityCheck<15000)return;lastConnectivityCheck=now;boolean connected=OfflineMapPack.online(a);boolean in=OfflineMapPack.covers(a.gp(a.loc));if(!connected&&in&&!offlineMode&&!offlineFailed&&OfflineMapPack.available(a))loadOffline(a.dark);else if(connected&&offlineMode){offlineFailed=false;loadStyle(a.dark);}else if(!in&&offlineMode)loadStyle(a.dark);}
    void loadBuilder(boolean value,Style.Builder builder){
        dark=value;final int token=++generation;
        ready=false;style=null;installed.clear();lastTarget=null;lastCameraAt=0;
        a.map.setVisibility(View.VISIBLE);view.setAlpha(0f);view.setVisibility(View.VISIBLE);
        ui.postDelayed(()->{if(!destroyed&&token==generation&&!ready){generation++;fallback();}},18000);map.setStyle(builder,s->{
            if(destroyed||token!=generation)return;
            try{
                style=s;hideUnrequestedPlaces();configureBuildings();lastSprite=-1;
                for(String id:new String[]{"alt","route","jams","points","radars","bumps","dest","car"})s.addSource(new GeoJsonSource("ru-"+id,EMPTY));
                s.addLayer(new LineLayer("ru-alt","ru-alt").withProperties(lineColor(0xff88949b),lineWidth(5f),lineCap("round"),lineJoin("round")));
                s.addLayer(new LineLayer("ru-route-border","ru-route").withProperties(lineColor(0xff075e49),lineWidth(13f),lineCap("round"),lineJoin("round")));
                s.addLayer(new LineLayer("ru-route","ru-route").withProperties(lineColor(0xff10b981),lineWidth(9f),lineCap("round"),lineJoin("round")));
                s.addLayer(new LineLayer("ru-jam-border","ru-jams").withProperties(lineColor(value?0xff111820:0xfff8faf9),lineWidth(8f),lineOffset(8f),lineCap("round"),lineJoin("round")));s.addLayer(new LineLayer("ru-jams","ru-jams").withProperties(lineColor(Expression.get("color")),lineWidth(5f),lineOffset(8f),lineCap("round"),lineJoin("round")));
                s.addLayer(new SymbolLayer("ru-points","ru-points").withProperties(iconImage(Expression.get("icon")),iconSize(.9f),iconAllowOverlap(true),iconIgnorePlacement(false),iconPitchAlignment("viewport")));
                s.addLayer(new SymbolLayer("ru-radars","ru-radars").withProperties(iconImage(Expression.get("icon")),iconSize(.85f),iconAllowOverlap(true),iconIgnorePlacement(false),iconPitchAlignment("viewport")));s.addLayer(new SymbolLayer("ru-bumps","ru-bumps").withProperties(iconImage(Expression.get("icon")),iconSize(.78f),iconAllowOverlap(false),iconIgnorePlacement(false),iconPitchAlignment("viewport")));s.addLayer(new SymbolLayer("ru-dest","ru-dest").withProperties(iconImage(Expression.get("icon")),iconSize(1f),iconAnchor("bottom"),iconAllowOverlap(true),iconPitchAlignment("viewport")));
                s.addLayer(new SymbolLayer("ru-car","ru-car").withProperties(iconImage(Expression.get("icon")),iconSize(.82f),iconRotationAlignment("viewport"),iconPitchAlignment("viewport"),iconAllowOverlap(true),iconIgnorePlacement(true)));
                image("sedan-0-3d",SedanSprite.draw(a.getResources(),0,true));
                ready=true;routes();events();traffic();position();
                if(a.driving&&a.loc!=null)camera(a.navigationPoint(),a.heading,a.tiltEnabled,true);
                else if(!a.routes.isEmpty())fit(a.routes.get(a.selected).points);
                else if(a.loc!=null)camera(a.gp(a.loc),0,false,false);
                else map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-15.5,-49),4));
                view.setAlpha(1f);view.setVisibility(View.VISIBLE);a.map.setVisibility(View.INVISIBLE);
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
    void hideUnrequestedPlaces(){if(style==null)return;for(org.maplibre.android.style.layers.Layer layer:style.getLayers()){String id=layer.getId().toLowerCase(java.util.Locale.ROOT);if(id.contains("poi")||id.contains("restaurant")||id.contains("shop")||id.contains("amenity")){try{layer.setProperties(visibility("none"));}catch(Exception ignored){}}}}
    void routes(){if(!active())return;JSONArray alt=new JSONArray(),chosen=new JSONArray(),dest=new JSONArray();try{
        for(int i=0;i<a.routes.size();i++){MainActivity.Route r=a.routes.get(i);if(r.points.size()<2)continue;if(i==a.selected){List<GeoPoint> remaining=a.remainingLine(r);if(remaining.size()>1)chosen.put(line(remaining,props("route",i)));}else if(!a.driving)alt.put(line(r.points,props("route",i)));}
        image("ru-dest-icon",a.mapIcon("pin",0xff087e65));if(a.destination!=null)dest.put(feature("Point",coords(a.destination.p),props("icon","ru-dest-icon","label",a.destination.name)));
        for(MainActivity.Place stop:a.stops)dest.put(feature("Point",coords(stop.p),props("icon","ru-dest-icon","label",stop.name)));
    }catch(Exception ignored){}source("alt",alt);source("route",chosen);source("dest",dest);events();}
    void routeProgress(){if(!active()||a.routes.isEmpty())return;try{List<GeoPoint> p=a.remainingLine(a.routes.get(a.selected));JSONArray f=new JSONArray();if(p.size()>1)f.put(line(p,props("route",a.selected)));source("route",f);if(android.os.SystemClock.elapsedRealtime()-lastEventDraw>2200)events();}catch(Exception ignored){}}
    void events(){if(!active())return;lastEventDraw=android.os.SystemClock.elapsedRealtime();JSONArray all=new JSONArray(),radars=new JSONArray(),bumps=new JSONArray();try{
        ArrayList<MainActivity.Event> data=a.roadEvents();long now=System.currentTimeMillis();GeoPoint here=a.loc==null?null:a.gp(a.loc);MainActivity.Route activeRoute=a.driving&&!a.routes.isEmpty()?a.routes.get(a.selected):null;double from=activeRoute!=null&&here!=null?Math.max(0,a.project(here,activeRoute.points)[0]-100):0;
        for(MainActivity.Event e:data){if(e.expires<now||here==null||here.distanceToAsDouble(e.p)>40000)continue;if(e.kind.equals("lombada")){if(activeRoute==null)continue;double[] q=a.eventPosition(e,activeRoute);double start=a.project(here,activeRoute.points)[0];if(!NavigationRules.showBump(a.driving,q[0],q[1],q[2],start,q[3],e.direction))continue;}else if(activeRoute!=null&&!a.matchesRoute(e,activeRoute,from)&&here.distanceToAsDouble(e.p)>700)continue;
            String kind=e.kind;String id;Drawable d;
            if(kind.equals("radar")){if(e.limit>0){int n=(int)e.limit;id="radar-"+n;d=a.symbol(String.valueOf(n),0xffb91c1c);}else{id=e.label.contains("sinal")?"redcamera":"camera";d=a.mapIcon(id,0xffb91c1c);}}
            else if(kind.equals("semaforo")){id="traffic";d=a.trafficIcon();}
            else if(kind.equals("lombada")){id="bump";d=a.mapIcon("bump",0xffd97706);}else if(kind.equals("video")){id="video";d=a.mapIcon("video",0xff256d85);}
            else if(kind.equals("historico")){id="historical";d=a.mapIcon("camera",0xff64748b);}else if(kind.equals("transito")){id="jam-alert";d=a.mapIcon("jam",0xffdc5726);}
            else{id="hazard";d=a.mapIcon("hazard",0xffd97706);}
            image(id,d);(kind.equals("lombada")?bumps:kind.equals("radar")||kind.equals("historico")||kind.equals("video")?radars:all).put(feature("Point",coords(e.p),props("icon",id,"label",e.label+(e.limit>0?" · "+(int)e.limit+" km/h":"")+" · "+e.source)));
        }
        String k="fuel".equals(category)?"fuel":"bus_stop".equals(category)?"bus":"pin";String id="poi-"+k;image(id,a.mapIcon(k,0xff087e65));if(!a.driving)for(MainActivity.Place p:places)all.put(feature("Point",coords(p.p),props("icon",id,"label",p.name+" · "+p.detail)));
    }catch(Exception ignored){}source("points",all);source("radars",radars);source("bumps",bumps);}
    void pois(List<MainActivity.Place> p,String c){places=c==null?Collections.emptyList():new ArrayList<>(p);category=c;events();}
    void traffic(){if(!active())return;JSONArray f=new JSONArray();try{for(MainActivity.Jam j:a.jams){if(j.expires<System.currentTimeMillis()||j.points.size()<2||j.level<2)continue;String color=j.level>=5?"#991b1b":j.level>=4?"#ef4444":j.level==3?"#f97316":"#eab308";f.put(line(j.points,props("color",color)));}}catch(Exception ignored){}source("jams",f);}
    void position(){if(!active())return;JSONArray f=new JSONArray();try{if(a.loc!=null){double bearing=a.driving?a.heading:a.loc.hasBearing()?a.loc.getBearing():0;double relative=bearing-(map==null?0:map.getCameraPosition().bearing);boolean pitched=a.driving&&a.tiltEnabled;int direction=SedanSprite.direction(relative);String icon="sedan-"+direction+(pitched?"-3d":"-2d");if(!installed.contains(icon))image(icon,SedanSprite.draw(a.getResources(),direction,pitched));f.put(feature("Point",coords(a.navigationPoint()),props("icon",icon,"label","Sua localização")));}}catch(Exception ignored){}source("car",f);}
    void configureBuildings(){if(style==null)return;try{boolean found=false;for(org.maplibre.android.style.layers.Layer l:style.getLayers()){if(l instanceof org.maplibre.android.style.layers.FillExtrusionLayer){l.setProperties(fillExtrusionOpacity(.78f));found=true;}}if(!found&&style.getSource("openmaptiles")!=null){org.maplibre.android.style.layers.FillExtrusionLayer l=new org.maplibre.android.style.layers.FillExtrusionLayer("ru-buildings","openmaptiles");l.setSourceLayer("building");l.setMinZoom(15.5f);l.setFilter(Expression.all(Expression.has("render_height"),Expression.has("render_min_height")));l.setProperties(fillExtrusionColor(dark?0xff43515a:0xffd7e4df),fillExtrusionHeight(Expression.get("render_height")),fillExtrusionBase(Expression.get("render_min_height")),fillExtrusionOpacity(.78f));style.addLayer(l);}}catch(Exception ignored){}}
    void camera(GeoPoint p,float bearing,boolean tilt,boolean drive){
        if(!active())return;position();if(drive&&!a.followEnabled)return;
        long now=android.os.SystemClock.elapsedRealtime();
        double kmh=a.loc!=null&&a.loc.hasSpeed()?a.loc.getSpeed()*3.6:0;
        boolean initial=lastTarget==null||drive!=lastDrive;
        double desiredZoom=drive?(kmh>85?16.6:kmh>55?16.95:kmh>25?17.3:17.65):15.0;
        double desiredTilt=drive&&tilt?(kmh>75?49:58):0;
        if(initial){lastZoom=desiredZoom;lastTilt=desiredTilt;lastBearing=drive?bearing:0;}
        else{lastZoom+=(desiredZoom-lastZoom)*.22;lastTilt+=(desiredTilt-lastTilt)*.30;lastBearing=(lastBearing+360+(float)(((bearing-lastBearing+540)%360-180)*.76))%360;}
        if(!drive)lastBearing=0;
        if(!initial&&now-lastCameraAt<180)return;
        if(!initial&&lastTarget.distanceToAsDouble(p)<1.4&&Math.abs(((bearing-lastBearing+540)%360)-180)<1.5&&Math.abs(lastTilt-desiredTilt)<.7&&Math.abs(lastZoom-desiredZoom)<.04)return;
        lastTarget=p;lastCameraAt=now;lastDrive=drive;
        int h=view.getHeight();double top=drive?Math.min(a.dp(380),h*.48):0;
        double bottom=drive?Math.min(a.dp(65),h*.08):0;
        CameraPosition c=new CameraPosition.Builder().target(new LatLng(p.getLatitude(),p.getLongitude())).zoom(lastZoom).bearing(lastBearing).tilt(lastTilt).padding(new double[]{0,top,0,bottom}).build();
        if(initial)map.moveCamera(CameraUpdateFactory.newCameraPosition(c));else map.easeCamera(CameraUpdateFactory.newCameraPosition(c),220);
    }
    void fit(List<GeoPoint> points){lastTarget=null;if(!active()||points.size()<2)return;try{LatLngBounds.Builder b=new LatLngBounds.Builder();for(GeoPoint p:points)b.include(new LatLng(p.getLatitude(),p.getLongitude()));map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(map.getCameraPosition()).bearing(0).tilt(0).padding(new double[]{0,0,0,0}).build()));map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(),a.dp(35),a.dp(105),a.dp(35),a.dp(260)),650);}catch(Exception ignored){}}
    void start(){view.onStart();}
    void resume(){view.onResume();}
    void pause(){view.onPause();}
    void stop(){view.onStop();}
    void save(Bundle state){view.onSaveInstanceState(state);}
    void lowMemory(){view.onLowMemory();}
    void destroy(){destroyed=true;ui.removeCallbacksAndMessages(null);mapFiles.shutdownNow();view.onDestroy();}
}
