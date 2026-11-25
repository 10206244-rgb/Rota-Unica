package br.com.rotaunica;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;
import org.osmdroid.util.GeoPoint;

/** Local read-only national index. All copying and queries run on a search worker. */
final class NationalHealthIndex {
 private static SQLiteDatabase db;
 private static synchronized SQLiteDatabase open(Context c)throws Exception{
  if(db!=null&&db.isOpen())return db;
  File dst=new File(c.getFilesDir(),"cnes-"+c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionCode+".sqlite");
  if(!dst.isFile()){File tmp=new File(dst.getPath()+".tmp");try(InputStream in=c.getAssets().open("national/cnes.sqlite");OutputStream out=new BufferedOutputStream(new FileOutputStream(tmp))){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);}catch(Exception e){tmp.delete();throw e;}if(!tmp.renameTo(dst))throw new IOException("Cannot install national index");}
  db=SQLiteDatabase.openDatabase(dst.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
  return db;
 }
 static ArrayList<MainActivity.Place> search(Context c,GeoPoint p,String q,String cat,boolean all)throws Exception{
  ArrayList<MainActivity.Place> out=new ArrayList<>();SQLiteDatabase d=open(c);
  String term=all?"":PlaceCategories.namedQuery(q,cat);String normalized=AddressRank.normalize(term);
  StringBuilder fts=new StringBuilder();for(String token:normalized.split(" ")){if(token.isEmpty())continue;if(fts.length()>0)fts.append(" ");fts.append(token).append("*");}
  String filter=cat.equals("hospital")?" AND category='hospital'":"";
  if(p!=null){int y=(int)Math.floor((p.getLatitude()+90)*4),x=(int)Math.floor((p.getLongitude()+180)*4);StringBuilder cells=new StringBuilder();for(int i=-2;i<=2;i++)for(int j=-2;j<=2;j++){if(cells.length()>0)cells.append(",");cells.append((y+i)*1440+x+j);}
   String where="cell IN ("+cells+")"+filter;ArrayList<String> args=new ArrayList<>();
   if(fts.length()>0){where+=" AND rowid IN (SELECT docid FROM names WHERE names MATCH ?)";args.add(fts.toString());}
   String sort="((lat-?)*(lat-?)+(lon-?)*(lon-?))";args.add(""+p.getLatitude());args.add(""+p.getLatitude());args.add(""+p.getLongitude());args.add(""+p.getLongitude());
   read(d,"SELECT name,detail,category,lat,lon FROM places WHERE "+where+" ORDER BY "+sort+" LIMIT 120",args.toArray(new String[0]),out);
  }
  if(out.size()<10&&fts.length()>0)read(d,"SELECT name,detail,category,lat,lon FROM places WHERE rowid IN (SELECT docid FROM names WHERE names MATCH ?)"+filter+" LIMIT 100",new String[]{fts.toString()},out);
  return out;
 }
 private static void read(SQLiteDatabase d,String sql,String[] args,ArrayList<MainActivity.Place> out){try(Cursor r=d.rawQuery(sql,args)){while(r.moveToNext()){MainActivity.Place p=new MainActivity.Place(r.getString(0),r.getString(1),r.getDouble(3),r.getDouble(4));p.category=r.getString(2);out.add(p);}}}
}
