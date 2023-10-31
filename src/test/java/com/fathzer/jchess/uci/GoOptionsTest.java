package com.fathzer.jchess.uci;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fathzer.jchess.uci.GoOptions.TimeOptions;

class GoOptionsTest {

	@Test
	void basicTest() {
		GoOptions options = new GoOptions(Arrays.asList("wtime 297999 btime 300000 winc 3000 binc 4000".split(" ")));
		assertEquals(0, options.getDepth());
		assertEquals(0, options.getMate());
		assertEquals(0, options.getNodes());
		assertFalse(options.isPonder());
		assertTrue(options.getIgnoredOptions().isEmpty());
		assertTrue(options.getMoveToSearch().isEmpty());
		TimeOptions timeOptions = options.getTimeOptions();
		assertEquals(0, timeOptions.getMovesToGo());
		assertEquals(0, timeOptions.getMoveTimeMs());
		assertFalse(timeOptions.isInfinite());
		assertEquals(297999, timeOptions.getWhiteClock().getRemainingMs());
		assertEquals(3000, timeOptions.getWhiteClock().getIncrementMs());
		assertEquals(300000, timeOptions.getBlackClock().getRemainingMs());
		assertEquals(4000, timeOptions.getBlackClock().getIncrementMs());
	}
	
	@Test
	void anotherTest() {
		GoOptions options = new GoOptions(Arrays.asList("depth 5 nodes 400000 mate 3 movetime 50000 movestogo 2".split(" ")));
		assertEquals(5, options.getDepth());
		assertEquals(3, options.getMate());
		assertEquals(400000, options.getNodes());
		assertFalse(options.isPonder());
		assertTrue(options.getIgnoredOptions().isEmpty());
		assertTrue(options.getMoveToSearch().isEmpty());
		TimeOptions timeOptions = options.getTimeOptions();
		assertEquals(2, timeOptions.getMovesToGo());
		assertEquals(50000, timeOptions.getMoveTimeMs());
		assertFalse(timeOptions.isInfinite());
		assertEquals(0, timeOptions.getWhiteClock().getRemainingMs());
		assertEquals(0, timeOptions.getWhiteClock().getIncrementMs());
		assertEquals(0, timeOptions.getBlackClock().getRemainingMs());
		assertEquals(0, timeOptions.getBlackClock().getIncrementMs());
	}
	
	@Test
	void movesTest() {
		GoOptions options = new GoOptions(Arrays.asList("searchmoves e2e4 d2d4 infinite ponder".split(" ")));
		assertEquals(0, options.getDepth());
		assertEquals(0, options.getMate());
		assertEquals(0, options.getNodes());
		assertTrue(options.isPonder());
		assertTrue(options.getIgnoredOptions().isEmpty());
		assertEquals(2, options.getMoveToSearch().size());
		UCIMove mv = options.getMoveToSearch().get(0);
		assertEquals("e2", mv.getFrom());
		assertEquals("e4", mv.getTo());
		mv = options.getMoveToSearch().get(1);
		assertEquals("d2", mv.getFrom());
		assertEquals("d4", mv.getTo());
		TimeOptions timeOptions = options.getTimeOptions();
		assertEquals(0, timeOptions.getMovesToGo());
		assertEquals(0, timeOptions.getMoveTimeMs());
		assertTrue(timeOptions.isInfinite());
		assertEquals(0, timeOptions.getWhiteClock().getRemainingMs());
		assertEquals(0, timeOptions.getWhiteClock().getIncrementMs());
		assertEquals(0, timeOptions.getBlackClock().getRemainingMs());
		assertEquals(0, timeOptions.getBlackClock().getIncrementMs());
	}

	@Test
	void illegalValues() {
		assertThrows(IllegalArgumentException.class, () -> new GoOptions(Arrays.asList("depth ponder".split(" "))));
		assertThrows(IllegalArgumentException.class, () -> new GoOptions(Arrays.asList("depth -1".split(" "))));
		assertThrows(IllegalArgumentException.class, () -> new GoOptions(Arrays.asList("nodes ponder".split(" "))));
		assertThrows(IllegalArgumentException.class, () -> new GoOptions(Arrays.asList("nodes -1".split(" "))));
	}
}
