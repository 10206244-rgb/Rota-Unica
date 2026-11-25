package br.com.rotaunica;
import org.junit.Test;
import static org.junit.Assert.*;
public class AddressRankTest {
 @Test public void accentInsensitive(){assertEquals("antao",AddressRank.normalize("Antão"));}
 @Test public void articlesAndStreetPrefix(){assertEquals("antao mesquita",AddressRank.core("Rua Antão de Mesquita"));}
 @Test public void partialStreet(){assertTrue(AddressRank.prefixMatch("Antão","Rua Antão Mesquita"));}
 @Test public void multiplePartialWords(){assertTrue(AddressRank.prefixMatch("Ant Mesq","Rua Antão de Mesquita"));}
 @Test public void unrelatedRoad(){assertFalse(AddressRank.prefixMatch("Antão Mesquita","Rua Antônio Carlos"));}
 @Test public void closestSameName(){assertTrue(AddressRank.score("Antão","Rua Antão Mesquita","Serra",true,2)>AddressRank.score("Antão","Rua Antão Mesquita","Outra cidade",true,250));}
 @Test public void exactNameBeatsNearbyMismatch(){assertTrue(AddressRank.score("Antão","Rua Antão de Mesquita","Serra",true,4)>AddressRank.score("Antão","Rua São José","Serra",true,0.2));}
 @Test public void sqlPatternWithAccentsAndOptionalWords(){assertEquals("%_NT__%M_SQ__T_%",AddressRank.sqlPattern("Rua Antão de Mesquita"));}
 @Test public void sqlEscapesPunctuation(){assertFalse(AddressRank.sqlPattern("a' OR 1=1").contains("'"));}
 @Test public void doesNotRepeatPrefix(){assertEquals("Rua Antão",AddressRank.streetName("Rua Antão","Rua"));}
 @Test public void addsStreetPrefix(){assertEquals("Rua Antão",AddressRank.streetName("Antão","Rua"));}
 @Test public void emptyTermNotAStreetMatch(){assertFalse(AddressRank.prefixMatch("Rua","Rua Antão"));}
}