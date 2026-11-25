package br.com.rotaunica;
import java.text.Normalizer;
import java.util.*;
final class AddressRank {
 static String normalize(String x){return Normalizer.normalize(x==null?"":x,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
 static String core(String x){return normalize(x).replaceAll("\\b(rua|r|avenida|av|travessa|tv|rodovia|estrada|alameda|beco|de|da|do|das|dos)\\b"," ").replaceAll("\\s+"," ").trim();}
 static boolean prefixMatch(String q,String n){String a=core(q),b=core(n);if(a.isEmpty())return false;for(String t:a.split(" ")){boolean hit=false;for(String w:b.split(" "))if(w.startsWith(t)){hit=true;break;}if(!hit)return false;}return true;}
 static double score(String q,String name,String detail,boolean street,double distanceKm){String a=core(q),b=core(name);double score=b.equals(a)?125:b.startsWith(a)?95:prefixMatch(q,name)?80:prefixMatch(q,name+" "+detail)?38:0; if(street)score+=12; if(Double.isFinite(distanceKm))score+=Math.max(0,45-12*Math.log1p(Math.max(0,distanceKm)));return score;}
 static String sqlPattern(String q){String x=core(q).toUpperCase(Locale.ROOT);if(x.isEmpty())return "";StringBuilder b=new StringBuilder("%");for(String w:x.split(" ")){for(char c:w.toCharArray()){if("AEIOUC".indexOf(c)>=0)b.append('_');else b.append(c);}b.append('%');}return b.toString();}
 static String streetName(String raw,String type){String n=raw==null?"":raw.trim();if(n.isEmpty())return n;String a=normalize(n);if(a.matches("^(rua|r|avenida|av|travessa|rodovia|estrada|alameda|beco|praca|largo)\\b.*"))return n;return (type==null||type.isEmpty()?"":type+" ")+n;}
}