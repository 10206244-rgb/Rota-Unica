package br.com.rotaunica;
import org.junit.Test;
import static org.junit.Assert.*;
public class NavigationRulesTest {
 @Test public void hiddenWithoutNavigation(){assertFalse(NavigationRules.showBump(false,100,0,3000,0,0,Double.NaN));}
 @Test public void showsOnRoute(){assertTrue(NavigationRules.showBump(true,100,2,3000,0,0,Double.NaN));}
 @Test public void rejectsAdjacentRoad(){assertFalse(NavigationRules.showBump(true,100,24,3000,0,0,Double.NaN));}
 @Test public void hidesPassedBump(){assertFalse(NavigationRules.showBump(true,100,2,3000,120,0,Double.NaN));}
 @Test public void hidesDistantBump(){assertFalse(NavigationRules.showBump(true,2600,2,3000,0,0,Double.NaN));}
 @Test public void rejectsOppositeDirection(){assertFalse(NavigationRules.showBump(true,100,2,3000,0,0,180));}
 @Test public void wrapsNorth(){assertTrue(NavigationRules.showBump(true,100,2,3000,0,359,1));}
 @Test public void rejectsExpiredTraffic(){assertEquals(0,NavigationRules.recentTimestamp(1000000,1900001));}
 @Test public void acceptsRecentTraffic(){assertEquals(1000000,NavigationRules.recentTimestamp(1000000,1100000));}
 @Test public void rejectsFutureTraffic(){assertEquals(0,NavigationRules.recentTimestamp(1200000,1100000));}
 @Test public void sedanRotatesEast(){assertEquals(6,NavigationRules.sedanDirection(90,24));}
 @Test public void sedanWrapsNegative(){assertEquals(23,NavigationRules.sedanDirection(-15,24));}
}