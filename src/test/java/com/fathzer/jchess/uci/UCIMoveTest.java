package com.fathzer.jchess.uci;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UCIMoveTest {

    @Test
    void testConstructorWithoutPromotion() {
        UCIMove move = new UCIMove("e2", "e4");
        assertEquals("e2", move.getFrom());
        assertEquals("e4", move.getTo());
        assertNull(move.getPromotion());
    }

    @Test
    void testConstructorWithPromotion() {
        UCIMove move = new UCIMove("e7", "e8", "q");
        assertEquals("e7", move.getFrom());
        assertEquals("e8", move.getTo());
        assertEquals("q", move.getPromotion());
    }

    @Test
    void testConstructorNullFromOrToThrows() {
        assertThrows(IllegalArgumentException.class, () -> new UCIMove(null, "e4"));
        assertThrows(IllegalArgumentException.class, () -> new UCIMove("e2", null));
        assertThrows(IllegalArgumentException.class, () -> new UCIMove(null, null, null));
    }

    @Test
    void testFromStaticMethodNormalMove() {
        UCIMove move = UCIMove.from("e2e4");
        assertEquals("e2", move.getFrom());
        assertEquals("e4", move.getTo());
        assertNull(move.getPromotion());
    }

    @Test
    void testFromStaticMethodPromotionMove() {
        UCIMove move = UCIMove.from("e7e8q");
        assertEquals("e7", move.getFrom());
        assertEquals("e8", move.getTo());
        assertEquals("q", move.getPromotion());
    }

    @Test
    void testFromStaticMethodInvalidInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> UCIMove.from("e2"));      // too short
        assertThrows(IllegalArgumentException.class, () -> UCIMove.from("e2e4q2"));  // too long
        assertThrows(IllegalArgumentException.class, () -> UCIMove.from(""));        // empty
        assertThrows(IllegalArgumentException.class, () -> UCIMove.from(null));      // null
    }

    @Test
    void testToString() {
        assertEquals("e2e4", new UCIMove("e2", "e4").toString());
        assertEquals("e7e8q", new UCIMove("e7", "e8", "q").toString());
    }

    @Test
    void testEqualsAndHashCode() {
        UCIMove move1 = new UCIMove("e2", "e4");
        UCIMove move2 = new UCIMove("e2", "e4", null);
        UCIMove move3 = UCIMove.from("e2e4");
        UCIMove move4 = new UCIMove("e7", "e8", "q");
        UCIMove move5 = UCIMove.from("e7e8q");

        // Reflexive
        assertEquals(move1, move1);
        // Symmetric and null
        assertEquals(move1, move2);
        assertEquals(move2, move1);
        assertNotEquals(move1, move4);
        assertFalse(move1.equals(null));
        assertFalse(move1.equals("some string"));

        // Transitive
        assertEquals(move1, move3);
        assertEquals(move1.hashCode(), move3.hashCode());

        // Promotion moves
        assertEquals(move4, move5);
        assertEquals(move4.hashCode(), move5.hashCode());
    }
}