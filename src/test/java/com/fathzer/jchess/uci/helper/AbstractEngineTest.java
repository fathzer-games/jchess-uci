package com.fathzer.jchess.uci.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.Evaluator;
import com.fathzer.games.ai.iterativedeepening.DeepeningPolicy;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningSearch;
import com.fathzer.games.ai.time.TimeManager;
import com.fathzer.games.ai.transposition.TranspositionTable;
import com.fathzer.games.clock.CountDownState;
import com.fathzer.jchess.uci.GoReply;
import com.fathzer.jchess.uci.StoppableTask;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.UsualOptions;
import com.fathzer.jchess.uci.option.ComboOption;
import com.fathzer.jchess.uci.option.Option;
import com.fathzer.jchess.uci.parameters.GoParameters;

class AbstractEngineTest {
	@SuppressWarnings("unchecked")
	private static TimeManager<MoveGenerator<String>> TM = mock(TimeManager.class);

	private static class MyEngine extends AbstractEngine<String, MoveGenerator<String>> {
		MyEngine(IterativeDeepeningEngine<String, MoveGenerator<String>> engine, TimeManager<MoveGenerator<String>> timeManager) {
			super(engine, timeManager);
		}
		@Override
		public UCIMove toUCI(String move) {
			return UCIMove.from(move);
		}
		
		@Override
		public void setStartPosition(String fen) {
			// This is a fake engine, it ignores the position
		}
		
		@Override
		protected String toMove(UCIMove move) {
			return move.toString();
		}

		@Override
		protected TranspositionTable<String, MoveGenerator<String>> buildTranspositionTable(int sizeInMB) {
			return null;
		}
	}
	
	@Test
	void hashTableRelatedTest() {
		assertEquals(-1, new MyEngine(new IterativeDeepeningEngine<>(new DeepeningPolicy(6), null, ()->null), TM).getDefaultHashTableSize());

		final AtomicInteger count = new AtomicInteger();
		final AtomicBoolean newGameCalled = new AtomicBoolean();
		@SuppressWarnings("unchecked")
		TranspositionTable<String, MoveGenerator<String>> initialTt = mock(TranspositionTable.class);
		when(initialTt.getMemorySizeMB()).thenReturn(32);
		final IterativeDeepeningEngine<String, MoveGenerator<String>> iter = new IterativeDeepeningEngine<>(new DeepeningPolicy(6), initialTt, ()->null);

		AbstractEngine<String, MoveGenerator<String>> engine = new MyEngine(iter, TM) {
			@Override
			protected TranspositionTable<String, MoveGenerator<String>> buildTranspositionTable(int sizeInMB) {
				@SuppressWarnings("unchecked")
				TranspositionTable<String, MoveGenerator<String>> tt = mock(TranspositionTable.class);
				when(tt.getMemorySizeMB()).thenReturn(sizeInMB);
				count.incrementAndGet();
				doAnswer(invocation -> {
					newGameCalled.set(true);
					return null;
				}).when(tt).newGame();
				return tt;
			}
		};
		assertEquals(32, engine.getDefaultHashTableSize());
		
		engine.setHashTableSize(16);
		assertEquals(16, engine.getEngine().getTranspositionTable().getMemorySizeMB());
		assertEquals(1, count.get());

		// Check no call to buildTranspositionTable() when size is not changed
		engine.setHashTableSize(16);
		assertEquals(16, engine.getEngine().getTranspositionTable().getMemorySizeMB());
		assertEquals(1, count.get());

		// Check changed size
		engine.setHashTableSize(32);
		assertEquals(32, engine.getEngine().getTranspositionTable().getMemorySizeMB());
		assertEquals(2, count.get());

		// Check tt.newGame is called when engine.clearHashTable() is called
		assertFalse(newGameCalled.get());
		engine.clearHashTable();
		assertTrue(newGameCalled.get());

		// Check null size
		engine.setHashTableSize(0);
		assertNull(engine.getEngine().getTranspositionTable());
		
		// Check clearHashTable called on no hash table
		newGameCalled.set(false);
		engine.clearHashTable();
		assertFalse(newGameCalled.get());
	}
	
	@Test
	void bug20241125() {
		// after a go wtime xxx that ends with an exception, a go command (without time options) continues to use the previous max time
		long maxTime = 36000; 
		DeepeningPolicy policy = new DeepeningPolicy(10);
		policy.setMaxTime(maxTime);
		final AtomicLong lastMaxTime = new AtomicLong();
		IterativeDeepeningEngine<String, MoveGenerator<String>> engine = new IterativeDeepeningEngine<>(policy, null, null) {
			@Override
			protected IterativeDeepeningSearch<String> doSearch(MoveGenerator<String> board, List<String> searchedMoves) {
				lastMaxTime.set(getDeepeningPolicy().getMaxTime());
				throw new RuntimeException();
			}
		};
		engine.setParallelism(1);
		
		final TimeManager<MoveGenerator<String>> tm = new TimeManager<MoveGenerator<String>>() {
			@Override
			public long getMaxTime(MoveGenerator<String> data, CountDownState countDown) {
				return countDown.getRemainingMs();
			}
		};
		
		AbstractEngine<String, MoveGenerator<String>> ae = new MyEngine(engine, tm) {
			@Override
			public String getId() {
				return "fake";
			}
		};
		
		// Check settings changes engine config
		GoParameters params = new GoParameters();
		GoParameters.PARSER.parse(params, new LinkedList<>(Arrays.asList(("movetime 1000").split(" "))));
		StoppableTask<GoReply> task = ae.go(params);
		assertThrows(RuntimeException.class, task::call);
		assertEquals(1000, lastMaxTime.get());
		assertEquals(maxTime, engine.getDeepeningPolicy().getMaxTime());
	}
	
	@Test
	void newGameTest() {
		@SuppressWarnings("unchecked")
		IterativeDeepeningEngine<String, MoveGenerator<String>> mockEngine = mock(IterativeDeepeningEngine.class);
		when(mockEngine.getDeepeningPolicy()).thenReturn(new DeepeningPolicy(10));
		AbstractEngine<String, MoveGenerator<String>> engine = new MyEngine(mockEngine, TM);
		engine.newGame();
		verify(mockEngine, times(1)).newGame();
	}

	@Test
	void getOptionsTest() {
		@SuppressWarnings("unchecked")
		IterativeDeepeningEngine<String, MoveGenerator<String>> mockEngine = mock(IterativeDeepeningEngine.class);
		when(mockEngine.getParallelism()).thenReturn(2);
		when(mockEngine.getDeepeningPolicy()).thenReturn(new DeepeningPolicy(5));
		AbstractEngine<String, MoveGenerator<String>> engine = new MyEngine(mockEngine, TM);
		Map<String, Option<?>> options = engine.getOptions();
		assertEquals(Set.of(UsualOptions.THREADS_NAME, UsualOptions.MULTI_PV_NAME, "depth", "maxtime"), options.keySet());

		@SuppressWarnings("unchecked")
		Evaluator<String, MoveGenerator<String>> evaluator1 = mock(Evaluator.class);
		@SuppressWarnings("unchecked")
		Evaluator<String, MoveGenerator<String>> evaluator2 = mock(Evaluator.class);
		engine.setEvaluators(List.of(new EvaluatorConfiguration<>("eval1", () -> evaluator1), new EvaluatorConfiguration<>("eval2", () -> evaluator2)));
		options = engine.getOptions();
		assertEquals(Set.of(UsualOptions.THREADS_NAME, UsualOptions.MULTI_PV_NAME, "depth", "maxtime", "evaluation"), options.keySet());
		ComboOption evaluators = (ComboOption) options.get("evaluation");
		assertEquals("eval1", evaluators.getValue());
		assertEquals("option name evaluation type combo default eval1 var eval2 var eval1", evaluators.toUCI());

		assertThrows(IllegalArgumentException.class, () -> evaluators.setValue("eval3"));
		// Check that the value is changed with no error
		evaluators.setValue("eval2");
		assertEquals("eval2", evaluators.getValue());
	}

	@Test
	void moveTest() {
		@SuppressWarnings("unchecked")
		MoveGenerator<String> mockBoard = mock(MoveGenerator.class);
		@SuppressWarnings("unchecked")
		IterativeDeepeningEngine<String, MoveGenerator<String>> mockEngine = mock(IterativeDeepeningEngine.class);
		when(mockEngine.getDeepeningPolicy()).thenReturn(new DeepeningPolicy(5));
		AbstractEngine<String, MoveGenerator<String>> engine = new MyEngine(mockEngine, TM);
		engine.board = mockBoard;
		UCIMove move = UCIMove.from("e2e4");
		engine.move(move);
		verify(mockBoard, times(1)).makeMove("e2e4", MoveGenerator.MoveConfidence.LEGAL);
	}

	@Test
	void getMoveGeneratorTest() {
		@SuppressWarnings("unchecked")
		MoveGenerator<String> mockBoard = mock(MoveGenerator.class);
		@SuppressWarnings("unchecked")
		IterativeDeepeningEngine<String, MoveGenerator<String>> mockEngine = mock(IterativeDeepeningEngine.class);
		when(mockEngine.getDeepeningPolicy()).thenReturn(new DeepeningPolicy(5));
		AbstractEngine<String, MoveGenerator<String>> engine = new MyEngine(mockEngine, TM);
		engine.board = mockBoard;
		assertEquals(mockBoard, engine.getMoveGenerator());
	}
}
