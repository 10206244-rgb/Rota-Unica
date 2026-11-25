package br.com.rotaunica;
import org.junit.Test;
import static org.junit.Assert.*;
public class NavigationMotionTest {
 @Test public void interpolatesInsteadOfTeleporting(){NavigationMotion n=new NavigationMotion();n.position(0,0,350,1000,false);n.position(0,.001,10,2000,false);assertEquals(0,n.at(2000).lon,1e-8);assertEquals(.0005,n.at(2470).lon,1e-8);assertEquals(0,n.at(2470).bearing,1e-8);assertEquals(.001,n.at(4000).lon,1e-8);}
 @Test public void stopsAtLastKnownPosition(){NavigationMotion n=new NavigationMotion();n.position(0,0,0,1000,false);n.position(0,.001,0,2000,false);assertEquals(.001,n.at(20000).lon,1e-8);}
 @Test public void rejectsBadSpeedAndExpiresOldValue(){NavigationMotion n=new NavigationMotion();n.speedSample(10,1,1000);n.speedSample(100,5,1500);assertEquals(10,n.speed(1500),1e-8);assertTrue(Double.isNaN(n.speed(4501)));}
 @Test public void stationarySpeedSettlesAtZero(){NavigationMotion n=new NavigationMotion();n.speedSample(10,1,1000);n.speedSample(0,1,2000);n.speedSample(0,1,2500);assertEquals(0,n.speed(2500),1e-8);}
 @Test public void rejectsImpossibleJump(){assertFalse(NavigationMotion.plausible(500,1,10,10,5,5));assertTrue(NavigationMotion.plausible(15,1,10,10,5,5));}
}