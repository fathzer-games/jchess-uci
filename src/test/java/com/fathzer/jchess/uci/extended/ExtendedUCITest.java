package com.fathzer.jchess.uci.extended;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.perft.FromPositionMoveGeneratorBuilder;
import com.fathzer.games.perft.PerfTTestData;
import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.util.InstrumentedUCI;

class ExtendedUCITest {
	private void exec(Engine engine, Consumer<InstrumentedUCI> test) {
		try (InstrumentedUCI uci = InstrumentedUCI.start(engine)) {
			try {
				uci.post("debug on");
				test.accept(uci);
			} finally {
				uci.post("q");
			}
		}
	}

	@Test
	void testDisplay() {
		final Displayable engine = mock(Displayable.class, withSettings().extraInterfaces(Engine.class));
		when(engine.getFEN()).thenReturn("fen");
		when(engine.getBoardAsString()).thenReturn("board");
		exec((Engine)engine, uci -> {
			// Test no position set
			uci.post("d");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug();
			
			// Test with position
			uci.clear();
			uci.post("position startpos");
			uci.post("d");
			assertEquals(Collections.singletonList("board"), uci.out());
			uci.clear();
			
			// Test with board representation
			uci.post("d fen");
			assertEquals(Collections.singletonList("fen"), uci.out());
			uci.clear();
			
			// Test with illegal argument
			uci.post("d foo");
			uci.assertDebug();
			uci.clear();
			uci.post("d fen fen");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug();
		});
		// Test with non displayable engine
		final Engine nonDisplayable = mock(Engine.class);
		exec(nonDisplayable, uci -> {
			uci.post("position startpos");
			uci.post("d");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug();
		});
	}
	
	@Test
	void testPerfT() {
		@SuppressWarnings("unchecked")
		final MoveGenerator<String> mv = mock(MoveGenerator.class);
		@SuppressWarnings("unchecked")
		final MoveGeneratorSupplier<String> engine = mock(MoveGeneratorSupplier.class, withSettings().extraInterfaces(Engine.class));
		when(engine.getMoveGenerator()).thenReturn(mv);
		when(mv.fork()).thenReturn(mv);
		when(mv.getMoves()).thenReturn(Arrays.asList("a","b"));
		when(mv.makeMove(anyString(), any(MoveGenerator.MoveConfidence.class))).thenReturn(true);
		exec((Engine)engine, uci -> {
			// Test no position set
			uci.post("perft 2");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug();
			uci.clear();

			// Set position
			uci.post("position startpos");
			
			// Test with illegal argument
			uci.post("perft");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug();
			uci.clear();

			uci.post("perft x");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug();
			uci.clear();

			// Test with everything fine
			uci.post("perft 2");
			await().atMost(Duration.ofSeconds(1)).until(uci::isBackgroundCompleted);
			assertEquals(Map.of(), uci.getExceptions());
			final String pseudo = uci.out().remove(uci.out().size()-1);
			assertTrue(pseudo.startsWith("perft 6"), "Perft pseudo-legal generated moves not starting with perft 6: "+pseudo);
			final String leaves = uci.out().remove(uci.out().size()-1);
			assertTrue(leaves.startsWith("perft 4"), "Perft leaves not starting with perft 4: "+leaves);
			assertEquals(Arrays.asList("a: 2", "b: 2"), uci.out());
		});		
		// Test with engine that does not implement MoveGeneratorSupplier
		final Engine basicEngine = mock(Engine.class);
		exec(basicEngine, uci -> {
			uci.post("position startpos");
			uci.post("perft 2");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug();
		});
	}

	@Test
	void testPerfStat() {
		// Test with engine that does not implement FromPositionMoveGeneratorBuilder
		Engine engine = mock(Engine.class);
		exec(engine, uci -> {
			uci.post("test");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug();
		});
		
		// Test with engine that implements FromPositionMoveGeneratorBuilder
		engine = mock(Engine.class, withSettings().extraInterfaces(FromPositionMoveGeneratorBuilder.class));
		exec(engine, uci -> {
			// No data available
			uci.post("test 4");
			assertFalse(uci.out().isEmpty());
			uci.assertDebug(uci.out().get(uci.out().size()-1));
			uci.clear();

			// Data available, but Perft throws an exception (the board is null)
			PerfTTestData perfTTestData = new PerfTTestData("startpos", "x");
			perfTTestData.add(2);
			uci.setTestData(Arrays.asList(perfTTestData));
			uci.post("test 1");
			await().atMost(Duration.ofSeconds(1)).until(uci::isBackgroundCompleted);
			assertTrue(uci.getExceptions().containsKey("test"));
		});
	}
}
