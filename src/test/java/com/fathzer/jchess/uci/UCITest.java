package com.fathzer.jchess.uci;
import static org.junit.jupiter.api.Assertions.*;

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
		uci.clear();
		engine.clear();
	}

	@Test
	void test() {
		final AtomicReference<String> fen = new AtomicReference<>();
	
		engine.setPositionConsumer(f -> fen.set(f));
		assertFalse(uci.post("cjhjhl",500));
		assertFalse(uci.getDebug().isEmpty());
		assertNull(fen.get());
		
		uci.clear();
		assertTrue(uci.post("position", 60000));
		assertFalse(uci.getDebug().isEmpty());
		assertNull(fen.get());

		uci.clear();
		assertTrue(uci.post("position kjmlkjm", 60000));
		assertFalse(uci.getDebug().isEmpty());
		assertNull(fen.get());

		uci.clear();
		assertTrue(uci.post("position startpos", 60000));
		assertTrue(uci.getDebug().isEmpty());
		assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fen.get());
		
		fen.set(null);
		assertTrue(uci.post("position fen toto", 60000));
		assertTrue(uci.getDebug().isEmpty());
		assertEquals("toto", fen.get());
		
	}
}
