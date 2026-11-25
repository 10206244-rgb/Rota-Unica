package br.com.rotaunica;

import org.junit.Test;
import static org.junit.Assert.*;

public class RouteGeometryTest {
 @Test public void projectsMidpointAndDistance() {
   RouteGeometry g=new RouteGeometry(new double[]{0,0},new double[]{0,.01});
   double[] r=g.project(.001,.005);
   assertEquals(556,r[0],3);assertEquals(111,r[1],2);assertEquals(1112,r[2],3);assertEquals(90,r[3],.01);
 }
 @Test public void clampsBeforeAndAfterSegment() {
   RouteGeometry g=new RouteGeometry(new double[]{0,.01},new double[]{0,0});
   assertEquals(0,g.project(-.005,0)[0],.01);
   assertEquals(g.along[1],g.project(.015,0)[0],.01);
 }
 @Test public void crossesAntimeridianWithoutLongDetour() {
   RouteGeometry g=new RouteGeometry(new double[]{0,0},new double[]{179.99,-179.99});
   assertEquals(2224,g.along[1],4);assertEquals(1112,g.project(0,180)[0],3);
 }
 @Test public void handlesMissingGeometry() {
   RouteGeometry g=new RouteGeometry(new double[0],new double[0]);
   assertTrue(Double.isInfinite(g.project(0,0)[1]));
 }
}
