package com.fathzer.jchess.uci;

import static com.fathzer.jchess.uci.parameters.GoParameters.PARSER;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import com.fathzer.jchess.uci.parameters.GoParameters;
import com.fathzer.jchess.uci.parameters.GoParameters.TimeOptions;

class GoParametersTest {
	private static Deque<String> toQueue(String str) {
		return new LinkedList<>(Arrays.asList(str.split(" ")));
	}

	@Test
	void basicTest() {
		GoParameters options = new GoParameters();
		assertTrue(PARSER.parse(options, toQueue("wtime 297999 btime 300000 winc 3000 binc 4000")).isEmpty());
		assertEquals(0, options.getDepth());
		assertEquals(0, options.getMate());
		assertEquals(0, options.getNodes());
		assertFalse(options.isPonder());
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
		GoParameters options = new GoParameters();
		assertTrue(PARSER.parse(options, toQueue("depth 5 nodes 400000 mate 3 movetime 50000 movestogo 2")).isEmpty());
		assertEquals(5, options.getDepth());
		assertEquals(3, options.getMate());
		assertEquals(400000, options.getNodes());
		assertFalse(options.isPonder());
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
		GoParameters options = new GoParameters();
		assertTrue(PARSER.parse(options, toQueue("searchmoves e2e4 d2d4 infinite ponder")).isEmpty());
		assertEquals(0, options.getDepth());
		assertEquals(0, options.getMate());
		assertEquals(0, options.getNodes());
		assertTrue(options.isPonder());
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
		final GoParameters params = new GoParameters(); 
		{
		final Deque<String> args = toQueue("depth ponder");
		assertThrows(IllegalArgumentException.class, () -> PARSER.parse(params,args));
		}
		{
		final Deque<String> args = toQueue("depth -1");
		assertThrows(IllegalArgumentException.class, () -> PARSER.parse(params,args));
		}
		{
		final Deque<String> args = toQueue("nodes ponder");
		assertThrows(IllegalArgumentException.class, () -> PARSER.parse(params,args));
		}
		{
		final Deque<String> args = toQueue("nodes -1");
		assertThrows(IllegalArgumentException.class, () -> PARSER.parse(params,args));
		}
	}
}
