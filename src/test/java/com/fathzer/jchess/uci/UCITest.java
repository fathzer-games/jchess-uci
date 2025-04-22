package com.fathzer.jchess.uci;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.core.DurationFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
		uci.post("ucinewgame");
		uci.post("debug on");
	}
	
	@AfterEach
	void tearDown() {
		uci.post("q");
	}
	
	@Test
	void unknownCommand() {
		assertFalse(uci.post("cjhjhl"));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
	}
	
	@Test
	void testDebugMode() {
		uci.debug("test");
		assertEquals(Arrays.asList("info string test"), uci.out());

		// set debug with wrong arg
		uci.out().clear();
		uci.post("debug www");
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());

		// set debug without arg
		uci.out().clear();
		uci.post("debug");
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());

		// set debug off
		uci.out().clear();
		uci.post("debug off");
		uci.debug("empty");
		assertTrue(uci.out().isEmpty());
		
		uci.out().clear();
		uci.post("debug www");
		assertTrue(uci.out().isEmpty());
	}
	
	private void assertDebug(String string) {
		assertTrue(isDebug(string), "expected \""+string+"\" started with \"info string \"");
	}

	private boolean isDebug(String string) {
		return string.startsWith("info string ");
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
		assertTrue(uci.post("position startpos"));
		assertTrue(uci.out().isEmpty());
		assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", position.toString());
		assertTrue(uci.isPositionSet());
		
		// fen
		position.setLength(0);
		uci.post("ucinewgame");
		uci.clear();
		assertFalse(uci.isPositionSet());
		assertTrue(uci.post("position fen toto"));
		assertTrue(uci.out().isEmpty());
		assertEquals("toto", position.toString());
		assertTrue(uci.isPositionSet());
		
		// fen with moves
		uci.post("ucinewgame");
		uci.clear();
		position.setLength(0);
		uci.post("position fen x moves a2a3 e7e5");
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
		assertTrue(uci.post("position"));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertFalse(uci.isPositionSet());

		// Illegal position kind
		uci.clear();
		assertTrue(uci.post("position kjmlkjm"));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertFalse(uci.isPositionSet());

		// Illegal fen
		uci.clear();
		doThrow(new IllegalArgumentException("Invalid FEN")).when(engine).setStartPosition(anyString());
		assertTrue(uci.post("position startpos"));
		assertFalse(uci.out().isEmpty());
		assertDebug(uci.out());
		assertFalse(uci.isPositionSet());

		// Illegal move
		uci.clear();
		configureEnginePositionMethods(position);
		doThrow(new IllegalArgumentException("Invalid move")).when(engine).move(any(UCIMove.class));
		assertTrue(uci.post("position fen x moves a b c"));
		assertEquals(3,uci.out().size());
	}

	@Test
	void bug20241123() {
		// Exceptions thrown by engine during the go command were not reported by the logger
		uci.post("ucinewgame");
		assertFalse(uci.isPositionSet());
		assertTrue(uci.post("position fen toto"));
		
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
		uci.post("go");
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
		uci.post("position startpos");
		assertTrue(uci.isPositionSet());
		// set engine to titi
		uci.post("engine titi");
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

		uci.post("position startpos");
		assertTrue(uci.isPositionSet());

		// Sets current engine
		uci.clear();
		uci.post("engine titi");
		assertDebug(uci.out());
		
		// Sets unknown engine
		uci.clear();
		uci.post("engine toto");
		assertDebug(uci.out());
		assertTrue(uci.isPositionSet());
	}
	
	@Test
	void testUCI() {
		when(engine.isChess960Supported()).thenReturn(true);
		when(engine.getDefaultHashTableSize()).thenCallRealMethod();
		when(engine.getOptions()).thenCallRealMethod();
		
		uci.post("uci");
		assertEquals(Arrays.asList("id name "+engine.getId(), "option name UCI_Chess960 type check default false", "uciok"), uci.out());
		uci.clear();
		
		uci.post("isready");
		assertEquals(Arrays.asList("readyok"), uci.out());
		
		// Try with author and no options
		Engine other = mock(Engine.class);
		when(other.getId()).thenReturn("other");
		when(other.getAuthor()).thenReturn("me");
		InstrumentedUCI myUCI = startUCI(other);
		try {
			myUCI.post("uci");
			assertEquals(Arrays.asList("id name "+other.getId(), "id author me", "uciok"), myUCI.out());
		} finally {
			myUCI.post("q");
		}
	}
	
	@Test
	void testSetOption() {
		when(engine.isChess960Supported()).thenReturn(true);
		when(engine.getOptions()).thenCallRealMethod();
		uci.post("setoption");
		assertFalse(uci.out().isEmpty());
		
		uci.clear();
		uci.post("setoption name "+UsualOptions.CHESS960_NAME);
		assertFalse(uci.out().isEmpty());
		
		uci.clear();
		uci.post("setoption nome "+UsualOptions.CHESS960_NAME+" value true");
		assertFalse(uci.out().isEmpty());
		
		uci.clear();
		uci.post("setoption name "+UsualOptions.CHESS960_NAME+" volue true");
		assertFalse(uci.out().isEmpty());

		uci.clear();
		uci.post("setoption name "+UsualOptions.CHESS960_NAME+" value true");
		assertEquals(Collections.emptyList(), uci.out());
	}
	
	private static class InstrumentedGoTask implements StoppableTask<GoReply> {
		private GoParameters arg;
		private AtomicBoolean called = new AtomicBoolean();
		private GoReply reply;
		private AtomicBoolean stopped = new AtomicBoolean();
		
		InstrumentedGoTask(GoReply reply) {
			this.reply = reply;
		}

		@Override
		public GoReply call() throws Exception {
			called.set(true);
			stopped.set(false);
			synchronized (this) {
				wait(100);
			}
			return reply;
		}

		@Override
		public void stop() {
			stopped.set(false);
			synchronized (this) {
				notifyAll();
			}
		}
		
		void clear() {
			arg = null;
			called.set(false);
		}
	}
	
	@Test
	void testGo() {
		GoReply reply = new GoReply(UCIMove.from("e2e4"));
		InstrumentedGoTask task = new InstrumentedGoTask(reply);
		when(engine.go(any(GoParameters.class))).thenAnswer(new Answer<StoppableTask<GoReply>>() {
		    @Override
		    public StoppableTask<GoReply> answer(InvocationOnMock invocation) throws Throwable {
		        // Capture the argument
		        task.arg = invocation.getArgument(0);
		        return task;
		    }
		    });

		// --------- Case 1: No position defined ---------
		uci.post("go");
		assertDebug(uci.out());
		assertFalse(task.called.get());
		assertNull(task.arg);

		// Set position
		uci.post("position startpos");

		// --------- Case 2: Position set with invalid params ---------
		Duration timeout = DurationFactory.of(60, TimeUnit.SECONDS);
		uci.clear();
		task.clear();
		uci.post("go xtime acx");
		await().atMost(timeout).until(()->!uci.isBackgroundRunning());
		assertTrue(uci.getExceptions().isEmpty());
		// Something is in debug output
		assertDebug(uci.out().get(0));
		// Best move is returned
		assertEquals("bestmove e2e4", uci.out().get(1));

/*
		// --------- Case 3: Engine already working ---------
		doReturn(Optional.of(params)).when(goCommand).parse(any(), any(), any());
		when(engine.go(params)).thenReturn(task);
		doReturn(false).when(goCommand).doBackground(any(), any(), any());

		goCommand.doGo(tokens);
		verify(goCommand).debug("Engine is already working");

		clearInvocations(goCommand);
*/

		// --------- Case 4: processGo with full info and valid parameters ---------
		uci.clear();
		task.clear();
		
		final GoReply.Info info = new GoReply.Info(4);
		reply.setInfo(info);
		info.setExtraMoves(Arrays.asList(UCIMove.from("d2d4"), UCIMove.from("b1c3")));

		uci.post("go wtime 100");
		await().atMost(timeout).until(()->!uci.isBackgroundRunning());
		assertTrue(uci.getExceptions().isEmpty());
		List<String> out = uci.out();
		assertEquals("bestmove e2e4", out.remove(out.size()-1));
		System.out.println(uci.out());
		
		// --------- Case 5: processGo throws an exception ---------
		//TODO
	}
}
