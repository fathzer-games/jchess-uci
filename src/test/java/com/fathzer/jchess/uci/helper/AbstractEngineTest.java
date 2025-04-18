package com.fathzer.jchess.uci.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.iterativedeepening.DeepeningPolicy;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningSearch;
import com.fathzer.games.ai.time.TimeManager;
import com.fathzer.games.ai.transposition.TranspositionTable;
import com.fathzer.games.clock.CountDownState;
import com.fathzer.jchess.uci.GoReply;
import com.fathzer.jchess.uci.StoppableTask;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.parameters.GoParameters;

class AbstractEngineTest {
	@Test
	void hashTableRelatedTest() {
		final IterativeDeepeningEngine<String, MoveGenerator<String>> iter = new IterativeDeepeningEngine<>(new DeepeningPolicy(6), null, ()->null);
		final AtomicInteger count = new AtomicInteger();
		final AtomicBoolean newGameCalled = new AtomicBoolean();

		@SuppressWarnings("unchecked")
		AbstractEngine<String, MoveGenerator<String>> engine = new AbstractEngine<String, MoveGenerator<String>>(iter, mock(TimeManager.class)) {
			@Override
			public UCIMove toUCI(String move) {
				return UCIMove.from(move);
			}
			
			@Override
			public void setStartPosition(String fen) {
				// Nothing to do
			}
			
			@Override
			protected String toMove(UCIMove move) {
				return move.toString();
			}
			
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
		assertEquals(0, engine.getDefaultHashTableSize());
		
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
		
		AbstractEngine<String, MoveGenerator<String>> ae = new AbstractEngine<>(engine, tm) {
			@Override
			public String getId() {
				return "fake";
			}

			@Override
			public void setStartPosition(String fen) {
				// This is a fake engine, it ignores the position
			}

			@Override
			public UCIMove toUCI(String move) {
				return UCIMove.from(move);
			}

			@Override
			protected TranspositionTable<String, MoveGenerator<String>> buildTranspositionTable(int sizeInMB) {
				return null;
			}

			@Override
			protected String toMove(UCIMove move) {
				return move.toString();
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
}
