package com.fathzer.jchess.uci.helper;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.MoveGenerator.MoveConfidence;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.time.TimeManager;
import com.fathzer.jchess.uci.BestMoveReply;
import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.LongRunningTask;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.extended.MoveGeneratorSupplier;
import com.fathzer.jchess.uci.extended.MoveToUCIConverter;
import com.fathzer.jchess.uci.parameters.GoParameters;

/** An abstract UCI engine based on an internal IterativeDeepeningEngine.
 * <br>This class helps the implementor by managing the internal engine configuration with go command arguments and some other smaller things.
 * <br>It significantly reduce the amount and complexity of code to write to get a working uci engine.
 * @param <M> The type of a move
 * @param <B> The type of the IterativeDeepeningEngine underlying move generator
 */
public abstract class AbstractEngine<M, B extends MoveGenerator<M>> implements Engine, MoveGeneratorSupplier<M>, MoveToUCIConverter<M> {
	protected B board;
	protected TimeManager<B> timeManager;
	protected IterativeDeepeningEngine<M, B> engine;
	
	/** Constructor.
	 * @param engine The internal engine to use
	 * @param timeManager The time manager that will decide how much time to allocate to a go search
	 */
	protected AbstractEngine(IterativeDeepeningEngine<M, B> engine, TimeManager<B> timeManager) {
		this.engine = engine;
		this.timeManager = timeManager;
	}

	/** Converts an UCI move to an internal move.
	 * @param move The move to convert
	 * @return an internal representation of the move
	 */
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
	
	@SuppressWarnings("unchecked")
	@Override
	public B get() {
		return (B) board.fork();
	}

	/** Gets the internal engine.
	 * @return an engine
	 */
	public IterativeDeepeningEngine<M, B> getEngine() {
		return engine;
	}
}
