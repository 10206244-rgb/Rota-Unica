package br.com.rotaunica;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

/** Bundled map data from Carcara, ODbL 1.0, build 2026-11-22. */
final class OfflineMapPack {
 static final String ASSET="offline/carcara-ES-leve.pmtiles";
 static final long SIZE=124905621L;
 static boolean covers(GeoPoint p){return p!=null&&p.getLatitude()>=-21.36&&p.getLatitude()<=-17.83&&p.getLongitude()>=-41.94&&p.getLongitude()<=-39.50;}
 static boolean online(Context c){try{ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();NetworkCapabilities cap=n==null?null:cm.getNetworkCapabilities(n);return cap!=null&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);}catch(Exception e){return true;}}
 static boolean available(Context c){try(InputStream in=c.getAssets().open(ASSET)){return true;}catch(Exception e){return false;}}
 static synchronized File prepare(Context c)throws IOException{
   File file=new File(c.getFilesDir(),"carcara-ES-20261122.pmtiles");
   if(file.isFile()&&file.length()==SIZE)return file;
   File temp=new File(c.getFilesDir(),"carcara-ES.tmp");temp.delete();
   try(InputStream in=c.getAssets().open(ASSET);OutputStream out=new BufferedOutputStream(new FileOutputStream(temp))){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);}
   if(temp.length()!=SIZE){temp.delete();throw new IOException("Pacote local incompleto");}
   if(!temp.renameTo(file)){temp.delete();throw new IOException("Não foi possível preparar mapa local");}
   return file;
 }
 static String style(Context c,boolean dark)throws Exception{
   File map=prepare(c);String raw;
   try(InputStream in=c.getAssets().open("offline/recursos/styles/"+(dark?"dark":"light")+".json");ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)bytes.write(b,0,n);raw=new String(bytes.toByteArray(),StandardCharsets.UTF_8);}
   JSONObject s=new JSONObject(raw);
   s.put("glyphs",s.getString("glyphs").replace("{recursos}","asset://offline/recursos"));
   s.put("sprite",s.getString("sprite").replace("{recursos}","asset://offline/recursos"));
   s.getJSONObject("sources").getJSONObject("carcara").put("url","pmtiles://file://"+map.getAbsolutePath());
   return s.toString();
 }
}
