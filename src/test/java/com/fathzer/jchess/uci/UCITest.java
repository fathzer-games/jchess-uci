package com.fathzer.jchess.uci;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
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
	
	@Test
	void bug20241123() {
		// Exceptions thrown by engine during the go command were not reported by the logger
		uci.post("ucinewgame", 10);
		assertFalse(uci.isPositionSet());
		engine.setPositionConsumer(s -> {});
		assertTrue(uci.post("position fen toto", 10));
		
		engine.setGoFunction(s -> new StoppableTask<>() {
			@Override
			public GoReply call() {
				throw new UnsupportedOperationException("I'm a buggy engine by thread "+Thread.currentThread());
			}

			@Override
			public void stop() {
				// call immediately throws an exception, there's no way to stop it
			}
		});
		try {
			uci.debug = true;
			try {
				System.err.println("We are in thread "+Thread.currentThread());
				uci.post("go", 10);
			} catch (Exception e) {
				e.printStackTrace();
			}
			await().atMost(500, TimeUnit.MILLISECONDS).until(() -> uci.getExceptions().getOrDefault("go", new IllegalArgumentException()).getClass()==UnsupportedOperationException.class);
		} finally {
			uci.debug = false;
		}
	}
	
    @Test
    void testAddCommandWithInvalidInputs() {
        // Test when no consumer is provided
        assertThrows(IllegalArgumentException.class, () -> uci.addCommand(null, "toto"));

        // Test when any command in commands is null or blank
        assertThrows(IllegalArgumentException.class, () -> uci.addCommand(x->{}, null, "alias"));
        assertThrows(IllegalArgumentException.class, () -> uci.addCommand(x->{}, "  ", "alias"));
        assertThrows(IllegalArgumentException.class, () -> uci.addCommand(x->{}, "cmd1", null, "alias"));
        assertThrows(IllegalArgumentException.class, () -> uci.addCommand(x->{}, "cmd1", " ", "alias"));
    }
}
