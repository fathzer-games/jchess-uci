package com.fathzer.jchess.uci.helper;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.MoveGenerator.MoveConfidence;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.time.TimeManager;
import com.fathzer.jchess.uci.BestMoveReply;
import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.LongRunningTask;
import com.fathzer.jchess.uci.MoveGeneratorSupplier;
import com.fathzer.jchess.uci.MoveToUCIConverter;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.parameters.GoParameters;

public abstract class AbstractEngine<M, B extends MoveGenerator<M>> implements Engine, MoveGeneratorSupplier<M>, MoveToUCIConverter<M> {
	protected TimeManager<B> timeManager = buildTimeManager();
	protected B board;
	protected IterativeDeepeningEngine<M, B> engine = buildEngine();
	
	protected abstract IterativeDeepeningEngine<M, B> buildEngine();

	protected abstract TimeManager<B> buildTimeManager();

	protected abstract M toMove(UCIMove move);

	@Override
	public void move(UCIMove move) {
		board.makeMove(toMove(move), MoveConfidence.LEGAL);
	}
	
	@Override
	public LongRunningTask<BestMoveReply> go(GoParameters options) {
		return new LongRunningTask<>() {
			@Override
			public BestMoveReply get() {
				final UCIEngineSearchConfiguration<M, B> c = new UCIEngineSearchConfiguration<>(timeManager);
				final UCIEngineSearchConfiguration.EngineConfiguration previous = c.configure(engine, options, board);
				final M move = engine.apply(board);
				c.set(engine, previous);
				return new BestMoveReply(toUCI(move));
			}

			@Override
			public void stop() {
				super.stop();
				engine.interrupt();
			}
		};
	}
	
	@Override
	public MoveGenerator<M> get() {
		return board.fork();
	}
	
	@Override
	public String getBoardAsString() {
		return board==null ? "no position defined" : board.toString();
	}

	public IterativeDeepeningEngine<M, B> getEngine() {
		return engine;
	}
}
