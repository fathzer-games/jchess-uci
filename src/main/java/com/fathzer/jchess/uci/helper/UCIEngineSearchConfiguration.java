package com.fathzer.jchess.uci.helper;

import com.fathzer.games.Color;
import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.time.TimeManager;
import com.fathzer.games.clock.CountDownState;
import com.fathzer.jchess.uci.parameters.GoParameters;
import com.fathzer.jchess.uci.parameters.GoParameters.PlayerClockData;
import com.fathzer.jchess.uci.parameters.GoParameters.TimeOptions;

/** A class that configures the engine before executing the go command
 */
public class UCIEngineSearchConfiguration<M, B extends MoveGenerator<M>> {
	public static class EngineConfiguration {
		private long maxTime;
		private int depth;
		
		private EngineConfiguration(IterativeDeepeningEngine<?, ?> engine) {
			maxTime = engine.getDeepeningPolicy().getMaxTime();
			depth = engine.getDeepeningPolicy().getDepth();
		}
	}

	private TimeManager<B> timeManager;
	
	public UCIEngineSearchConfiguration(TimeManager<B> timeManager) {
		this.timeManager = timeManager;
	}
	
	public EngineConfiguration configure(IterativeDeepeningEngine<M, B> engine, GoParameters options, B board) {
		final EngineConfiguration result = new EngineConfiguration(engine);
		final TimeOptions timeOptions = options.getTimeOptions();
		if (options.isPonder() || !options.getMoveToSearch().isEmpty() || options.getMate()>0 || options.getNodes()>0 || timeOptions.isInfinite()) {
			//TODO some options are not supported
		}
		if (options.getDepth()>0) {
			engine.getDeepeningPolicy().setDepth(options.getDepth());
		}
		if (timeOptions.getMoveTimeMs()>0) {
			engine.getDeepeningPolicy().setMaxTime(timeOptions.getMoveTimeMs());
		} else {
			final Color c = board.isWhiteToMove() ? Color.WHITE : Color.BLACK;
			final PlayerClockData engineClock = c==Color.WHITE ? timeOptions.getWhiteClock() : timeOptions.getBlackClock();
			if (engineClock.getRemainingMs()>0) {
				engine.getDeepeningPolicy().setMaxTime(getMaxTime(board, engineClock.getRemainingMs(), engineClock.getIncrementMs(), timeOptions.getMovesToGo()));
			}
		}
		return result;
	}

	public void set(IterativeDeepeningEngine<M, B> engine, EngineConfiguration c) {
		engine.getDeepeningPolicy().setMaxTime(c.maxTime);
		engine.getDeepeningPolicy().setDepth(c.depth);
	}
	
	public long getMaxTime(B board, long remainingMs, long incrementMs, int movesToGo) {
		return timeManager.getMaxTime(board, new CountDownState(remainingMs, incrementMs, movesToGo));
	}
}
