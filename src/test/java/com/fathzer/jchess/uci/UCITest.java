package com.fathzer.jchess.uci;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.jchess.uci.util.InstrumentedUCI;
import com.fathzer.jchess.uci.util.InstrumentedEngine;

class UCITest {
	private static InstrumentedUCI uci;
	private static InstrumentedEngine engine;
	
	@BeforeAll
	static void init() {
		engine = new InstrumentedEngine();
		uci = new InstrumentedUCI(engine);
		final Thread uciThread = new Thread(uci);
		uciThread.setDaemon(true);
		uciThread.start();
	}
	
	@BeforeEach
	void clear() {
		uci.post("ucinewgame", 100);
		uci.post("debug on",100);
		uci.clear();
		engine.clear();
	}
	
	@Test
	void unknownCommand() {
		assertFalse(uci.post("cjhjhl",500));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
	}
	
	@Test
	void testDebugMode() {
		uci.debug("test");
		assertEquals(Arrays.asList("info string test"), uci.out());

		// set debug with wrong arg
		uci.out().clear();
		uci.post("debug www", 100);
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());

		// set debug without arg
		uci.out().clear();
		uci.post("debug", 100);
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());

		// set debug off
		uci.out().clear();
		uci.post("debug off",100);
		uci.debug("empty");
		assertTrue(uci.out().isEmpty());
		
		uci.out().clear();
		uci.post("debug www", 100);
		assertTrue(uci.out().isEmpty());
	}
	
	private void assertDebug(String string) {
		assertTrue(string.startsWith("info string "), "expected \""+string+"\" started with \"info string \"");
	}

	private void assertDebug(Collection<String> strings) {
		strings.stream().forEach(this::assertDebug);
	}

	@Test
	void testPositionAndNewGame() {
		final AtomicReference<String> fen = new AtomicReference<>();
	
		engine.setPositionConsumer(f -> fen.set(f));
		assertFalse(uci.isPositionSet());
		
		// No position
		uci.clear();
		assertTrue(uci.post("position", 60000));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertNull(fen.get());
		assertFalse(uci.isPositionSet());

		// Illegal position kind
		uci.clear();
		assertTrue(uci.post("position kjmlkjm", 60000));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertNull(fen.get());
		assertFalse(uci.isPositionSet());

		// Start pos
		uci.clear();
		assertTrue(uci.post("position startpos", 60000));
		assertTrue(uci.out().isEmpty());
		assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fen.get());
		assertTrue(uci.isPositionSet());
		
		// fen
		fen.set(null);
		uci.post("ucinewgame", 100);
		uci.clear();
		assertFalse(uci.isPositionSet());
		assertTrue(uci.post("position fen toto", 60000));
		assertTrue(uci.out().isEmpty());
		assertEquals("toto", fen.get());
		assertTrue(uci.isPositionSet());
		
		// Illegal fen
		uci.post("ucinewgame", 100);
		uci.clear();
		engine.setPositionConsumer(f-> {throw new IllegalArgumentException();});
		assertTrue(uci.post("position startpos", 60000));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertFalse(uci.isPositionSet());
		
		//TODO Test move related things
	}
}
