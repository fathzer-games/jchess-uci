package com.fathzer.jchess.uci.helper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.iterativedeepening.DeepeningPolicy;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.time.TimeManager;
import com.fathzer.games.clock.CountDownState;
import com.fathzer.jchess.uci.parameters.GoParameters;

import org.mockito.junit.jupiter.MockitoExtension;                                             


@ExtendWith(MockitoExtension.class)                                                            
class UCIEngineSearchConfigurationTest {

	@Test
	void test(@Mock MoveGenerator<String> mv) {
		// after a go wtime xxx, a go command (without time options) continues to use the previous max time
		DeepeningPolicy policy = new DeepeningPolicy(10);
		IterativeDeepeningEngine<String, MoveGenerator<String>> engine = new IterativeDeepeningEngine<>(policy, null, null);
		
		final TimeManager<MoveGenerator<String>> tm = new TimeManager<MoveGenerator<String>>() {
			@Override
			public long getMaxTime(MoveGenerator<String> data, CountDownState countDown) {
				return countDown.getRemainingMs();
			}
		};
		
		final UCIEngineSearchConfiguration<String, MoveGenerator<String>> tested = new UCIEngineSearchConfiguration<>(tm);
		final long maxTime = engine.getDeepeningPolicy().getMaxTime();
		final int maxDepth = engine.getDeepeningPolicy().getDepth();
		
		GoParameters params = new GoParameters();
		// No params, no change in engine configuration
		GoParameters.PARSER.parse(params, new LinkedList<>());
		tested.configure(engine, params, mv);
		assertEquals(maxTime, engine.getDeepeningPolicy().getMaxTime());
		assertEquals(maxDepth, engine.getDeepeningPolicy().getDepth());
		
		// Check settings changes engine config
		params = new GoParameters();
		final int depth = maxDepth+6;
		GoParameters.PARSER.parse(params, new LinkedList<>(Arrays.asList(("depth "+depth+" movetime 1000").split(" "))));
		tested.configure(engine, params, mv);
		assertEquals(1000, engine.getDeepeningPolicy().getMaxTime());
		assertEquals(depth, engine.getDeepeningPolicy().getDepth());
	}
}
