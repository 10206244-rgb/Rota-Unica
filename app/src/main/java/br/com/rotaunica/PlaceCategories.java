package br.com.rotaunica;
import java.util.*;
/** Search intent and tag mappings; no provider credentials required. */
final class PlaceCategories {
 static final String[] IDS={"government","health","hospital","school","terminal","mall","library","police","fire_station","social","park","airport","fuel","restaurant","bus_stop","pharmacy","supermarket","parking","bank","post_office","university","sports"};
 static final String[][] TAGS={
 {"amenity=townhall","office=government","amenity=courthouse"},
 {"amenity=clinic","amenity=doctors","amenity=hospital","healthcare=clinic","healthcare=health_centre","healthcare=doctor","healthcare=hospital"},
 {"amenity=hospital","healthcare=hospital"},
 {"amenity=school","amenity=kindergarten","amenity=college"},
 {"amenity=bus_station","public_transport=station","railway=station","amenity=ferry_terminal"},
 {"shop=mall","shop=department_store"},
 {"amenity=library"},{"amenity=police"},{"amenity=fire_station"},{"amenity=social_facility","amenity=community_centre"},
 {"leisure=park","leisure=garden"},{"aeroway=aerodrome","aeroway=terminal"},{"amenity=fuel"},{"amenity=restaurant","amenity=fast_food"},
 {"highway=bus_stop","public_transport=platform"},{"amenity=pharmacy"},{"shop=supermarket"},{"amenity=parking"},
 {"amenity=bank"},{"amenity=post_office"},{"amenity=university"},{"leisure=sports_centre","leisure=stadium"}};
 static final String[] LABELS={"Órgão público","Unidade de saúde","Hospital","Escola","Terminal de transporte","Shopping","Biblioteca","Delegacia","Corpo de Bombeiros","Assistência social","Parque","Aeroporto","Posto de combustível","Restaurante","Ponto de ônibus","Farmácia","Supermercado","Estacionamento","Banco","Correios","Universidade","Centro esportivo"};
 static String[] tags(String id){int i=Arrays.asList(IDS).indexOf(id);return i<0?new String[0]:TAGS[i].clone();}
 static String label(String id){int i=Arrays.asList(IDS).indexOf(id);return i<0?"Local":LABELS[i];}
 static String detect(String query){String q=AddressRank.normalize(query);if(q.matches("^(rua|r|avenida|av|travessa|rodovia|estrada)\\b.*"))return null;
 if(q.matches(".*\\b(prefeitura|secretaria|forum|camara municipal|inss|detran|orgao publico)\\b.*"))return "government";
 if(q.matches(".*\\b(ubs|upa|urs|uaps|caps|unidade de saude|posto de saude|centro de saude|unidade basica|unidade regional)\\b.*"))return "health";
 if(q.matches(".*\\b(hospital|hospitais)\\b.*"))return "hospital";
 if(q.matches(".*\\b(terminal|rodoviaria|estacao de trem|estacao ferroviaria)\\b.*"))return "terminal";
 if(q.matches(".*\\b(shopping|shoppings|centro comercial)\\b.*"))return "mall";
 if(q.matches(".*\\b(escola|escolas|colegio|cmei|emef|creche)\\b.*"))return "school";
 if(q.matches(".*\\b(universidade|faculdade|ufes)\\b.*"))return "university";
 if(q.matches(".*\\b(delegacia|policia)\\b.*"))return "police";
 if(q.contains("bombeiro"))return "fire_station";
 if(q.matches(".*\\b(cras|creas|assistencia social|conselho tutelar)\\b.*"))return "social";
 if(q.contains("biblioteca"))return "library";if(q.contains("aeroporto"))return "airport";
 if(q.matches(".*\\b(parque|praca)\\b.*"))return "park";if(q.contains("correios"))return "post_office";
 if(q.contains("farmacia"))return "pharmacy";if(q.contains("supermercado"))return "supermarket";
 if(q.contains("restaurante"))return "restaurant";if(q.contains("posto de combustivel"))return "fuel";return null;}
 static boolean matches(String category,String key,String value){if(category==null)return false;for(String tag:tags(category))if(tag.equals(key+"="+value))return true;return false;}
 static boolean major(String key,String value){for(String id:new String[]{"government","health","hospital","school","terminal","mall","library","police","fire_station","social","park","airport","university"})if(matches(id,key,value))return true;return false;}
 static String namedQuery(String q,String category){String n=AddressRank.normalize(q);if(category==null)return n;String regex;
 switch(category){case "health":regex="\\b(unidade basica de saude|unidade regional de saude|unidade de saude|posto de saude|centro de saude|ubs|upa|urs|uaps|caps)\\b";break;case "government":regex="\\b(prefeitura|secretaria|camara municipal|orgao publico)\\b";break;case "terminal":regex="\\b(terminal|rodoviaria|estacao de trem|estacao ferroviaria)\\b";break;case "mall":regex="\\b(shoppings?|centro comercial)\\b";break;case "school":regex="\\b(escolas?|colegio|cmei|emef|creche)\\b";break;case "hospital":regex="\\b(hospital|hospitais)\\b";break;default:regex="\\b"+AddressRank.normalize(label(category))+"\\b";}
 return n.replaceAll(regex," ").replaceAll("\\b(de|da|do|das|dos|municipal|perto|proximo|proxima|mim)\\b"," ").replaceAll("\\s+"," ").trim();}
 static boolean matchName(String q,String category,String name,String detail){String n=namedQuery(q,category);return n.isEmpty()||AddressRank.prefixMatch(n,name+" "+detail);}
}
