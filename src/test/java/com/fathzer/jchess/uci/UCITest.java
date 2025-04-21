package com.fathzer.jchess.uci;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.jchess.uci.util.InstrumentedUCI;
import com.fathzer.jchess.uci.parameters.GoParameters;

class UCITest {
	private InstrumentedUCI uci;
	private Engine engine;

	protected static InstrumentedUCI startUCI(Engine engine) {
		final InstrumentedUCI uci = new InstrumentedUCI(engine);
		final Thread uciThread = new Thread(uci);
		uciThread.setDaemon(true);
		uciThread.start();
		return uci;
	}
	
	@BeforeEach
	void setup() {
		engine = mock(Engine.class);
		when(engine.getId()).thenReturn("InstrumentedEngine");
		uci = startUCI(engine);
		uci.post("ucinewgame", 100);
		uci.post("debug on",100);
	}
	
	@AfterEach
	void tearDown() {
		uci.post("q", 100);
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
		final StringBuilder position = new StringBuilder();
		configureEnginePositionMethods(position);

		assertFalse(uci.isPositionSet());

		// Start pos
		uci.clear();
		assertTrue(uci.post("position startpos", 60000));
		assertTrue(uci.out().isEmpty());
		assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", position.toString());
		assertTrue(uci.isPositionSet());
		
		// fen
		position.setLength(0);
		uci.post("ucinewgame", 100);
		uci.clear();
		assertFalse(uci.isPositionSet());
		assertTrue(uci.post("position fen toto", 60000));
		assertTrue(uci.out().isEmpty());
		assertEquals("toto", position.toString());
		assertTrue(uci.isPositionSet());
		
		// fen with moves
		uci.post("ucinewgame", 100);
		uci.clear();
		position.setLength(0);
		uci.post("position fen x moves a2a3 e7e5", 60000);
		assertTrue(uci.out().isEmpty());
		assertEquals("x a2a3 e7e5", position.toString());
		assertTrue(uci.isPositionSet());
	}
	
	private void configureEnginePositionMethods(StringBuilder position) {
	    doAnswer(invocation -> {
	        String fen = invocation.getArgument(0, String.class);
			position.setLength(0);
	        position.append(fen);
	        return null;
	    }).when(engine).setStartPosition(anyString());
	    
		doAnswer(invocation -> {
			UCIMove m = invocation.getArgument(0, UCIMove.class);
			if (position.isEmpty()) {
				throw new IllegalStateException();
			}
			position.append(' ');
			position.append(m.toString());
			return null;
		}).when(engine).move(any(UCIMove.class));
	}
	
	@Test
	void testPositionWithInvalidArguments() {
		final StringBuilder position = new StringBuilder();
		configureEnginePositionMethods(position);
		// No position
		uci.clear();
		assertTrue(uci.post("position", 60000));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertFalse(uci.isPositionSet());

		// Illegal position kind
		uci.clear();
		assertTrue(uci.post("position kjmlkjm", 60000));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertFalse(uci.isPositionSet());

		// Illegal fen
		uci.clear();
		doThrow(new IllegalArgumentException("Invalid FEN")).when(engine).setStartPosition(anyString());
		assertTrue(uci.post("position startpos", 60000));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertFalse(uci.isPositionSet());

		// Illegal move
		uci.clear();
		configureEnginePositionMethods(position);
		doThrow(new IllegalArgumentException("Invalid move")).when(engine).move(any(UCIMove.class));
		assertTrue(uci.post("position fen x moves a b c", 60000));
		assertEquals(3,uci.out().size());
	}

	@Test
	void bug20241123() {
		// Exceptions thrown by engine during the go command were not reported by the logger
		uci.post("ucinewgame", 10);
		assertFalse(uci.isPositionSet());
		assertTrue(uci.post("position fen toto", 10));
		
		when(engine.go(any(GoParameters.class))).thenReturn(new StoppableTask<>() {
			@Override
			public GoReply call() {
				throw new UnsupportedOperationException("I'm a buggy engine");
			}

			@Override
			public void stop() {
				// call immediately throws an exception, there's no way to stop it
			}
		});
		uci.post("go", 100);
		await().atMost(500, TimeUnit.MILLISECONDS).until(() -> uci.getExceptions().getOrDefault("go", new IllegalArgumentException()).getClass()==UnsupportedOperationException.class);
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
    
    @Test
	void testAddRemoveSetEngine() {
		// Test remove when engine is not found
		assertNull(uci.removeEngine("toto"));
		// Test add when engine with same id is already added
		Engine sameIdEngine = mock(Engine.class);
		final String id = engine.getId();
		when(sameIdEngine.getId()).thenReturn(id);
		assertThrows(IllegalArgumentException.class, () -> uci.add(sameIdEngine));
		// Blank id
		when(sameIdEngine.getId()).thenReturn("  ");
		assertThrows(IllegalArgumentException.class, () -> uci.add(sameIdEngine));
		

		// Test add when engine is found
		Engine otherEngine = mock(Engine.class);
		when(otherEngine.getId()).thenReturn("titi");
		uci.add(otherEngine);
		uci.doEngine(new LinkedList<>());
		assertEquals(Arrays.asList("engine "+id,"engine titi"), uci.out());
		
		uci.clear();
		uci.post("position startpos", 100);
		assertTrue(uci.isPositionSet());
		// set engine to titi
		uci.post("engine titi", 100);
		assertFalse(uci.isPositionSet());

		// Remove current engine
		assertThrows(IllegalStateException.class, () -> uci.removeEngine("titi"));
		
		// Remove previous engine
		assertNotNull(uci.removeEngine(id));
		assertDebug(uci.out().get(0));
		String outMessage = uci.out().size()==1 ? null : uci.out().get(1);
		assertEquals("engine titi ok", outMessage);
		
		// List engines
		uci.clear();
		uci.doEngine(new LinkedList<>());
		assertEquals(Arrays.asList("engine titi"), uci.out());

		uci.post("position startpos", 100);
		assertTrue(uci.isPositionSet());

		// Sets current engine
		uci.clear();
		uci.post("engine titi", 100);
		assertDebug(uci.out());
		
		// Sets unknown engine
		uci.clear();
		uci.post("engine toto", 100);
		assertDebug(uci.out());
		assertTrue(uci.isPositionSet());
	}
    
    @Test
    void testUCI() {
    	when(engine.isChess960Supported()).thenReturn(true);
    	when(engine.getDefaultHashTableSize()).thenCallRealMethod();
    	when(engine.getOptions()).thenCallRealMethod();
    	
    	uci.post("uci", 10);
    	assertEquals(Arrays.asList("id name "+engine.getId(), "option name UCI_Chess960 type check default false", "uciok"), uci.out());
    	uci.clear();
    	
    	uci.post("isready", 10);
    	assertEquals(Arrays.asList("readyok"), uci.out());
    	
    	// Try with author and no options
    	Engine other = mock(Engine.class);
    	when(other.getId()).thenReturn("other");
    	when(other.getAuthor()).thenReturn("me");
    	InstrumentedUCI myUCI = startUCI(other);
    	try {
	    	myUCI.post("uci", 10);
	    	assertEquals(Arrays.asList("id name "+other.getId(), "id author me", "uciok"), myUCI.out());
    	} finally {
    		myUCI.post("q", 100);
    	}
    }
    
    @Test
    void testSetOption() {
    	when(engine.isChess960Supported()).thenReturn(true);
    	when(engine.getOptions()).thenCallRealMethod();
    	uci.post("setoption", 10);
    	assertFalse(uci.out().isEmpty());
    	
    	uci.clear();
    	uci.post("setoption name "+UsualOptions.CHESS960_NAME, 10);
    	assertFalse(uci.out().isEmpty());
    	
    	uci.clear();
    	uci.post("setoption nome "+UsualOptions.CHESS960_NAME+" value true", 10);
    	assertFalse(uci.out().isEmpty());
    	
    	uci.clear();
    	uci.post("setoption name "+UsualOptions.CHESS960_NAME+" volue true", 10);
    	assertFalse(uci.out().isEmpty());

		uci.clear();
    	uci.post("setoption name "+UsualOptions.CHESS960_NAME+" value true", 10);
    	assertEquals(Collections.emptyList(), uci.out());
    }
}
