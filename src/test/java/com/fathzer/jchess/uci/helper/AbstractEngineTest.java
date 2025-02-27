package com.fathzer.jchess.uci.helper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
		assertThrows(RuntimeException.class, () -> task.call());
		assertEquals(1000, lastMaxTime.get());
		assertEquals(maxTime, engine.getDeepeningPolicy().getMaxTime());
	}
}
