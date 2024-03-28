package com.fathzer.jchess.uci;
import static org.junit.jupiter.api.Assertions.*;

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
	}

	@Test
	void test() {
//		assertFalse(uci.post("cjhjhl",500));
//		assertFalse(uci.getDebug().isEmpty());
		
		clear();
		assertTrue(uci.post("position", 60000));
		System.out.println(uci.getOutput());
	}
}
