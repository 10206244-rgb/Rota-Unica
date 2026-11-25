package br.com.rotaunica;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaceCategoriesTest {
 @Test public void publicBuilding(){assertEquals("government",PlaceCategories.detect("Prefeitura Municipal"));}
 @Test public void publicHealth(){assertEquals("health",PlaceCategories.detect("UBS Jardim América"));}
 @Test public void terminal(){assertEquals("terminal",PlaceCategories.detect("Terminal de Campo Grande"));}
 @Test public void mall(){assertEquals("mall",PlaceCategories.detect("Shopping Vitória"));}
 @Test public void roadNamedAfterHospital(){assertNull(PlaceCategories.detect("Rua Hospital São Lucas"));}
 @Test public void globalCategories(){assertEquals("government",PlaceCategories.detect("City Hall"));assertEquals("terminal",PlaceCategories.detect("Bus station"));assertEquals("school",PlaceCategories.detect("Escuela"));}
 @Test public void tagMappings(){assertTrue(PlaceCategories.matches("mall","shop","mall"));assertTrue(PlaceCategories.matches("terminal","amenity","bus_station"));assertTrue(PlaceCategories.matches("health","healthcare","clinic"));}
 @Test public void genericTermMatchesNearbyNamedPlace(){assertTrue(PlaceCategories.matchName("shopping","mall","Boulevard",""));}
 @Test public void specificNameMustMatch(){assertFalse(PlaceCategories.matchName("UBS Jardim América","health","UBS Centro","Vila Velha"));}
}