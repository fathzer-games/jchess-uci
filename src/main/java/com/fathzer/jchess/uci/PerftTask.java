package com.fathzer.jchess.uci;

import java.util.function.Supplier;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.perft.PerfT;
import com.fathzer.games.perft.PerfTResult;
import com.fathzer.games.util.ContextualizedExecutor;

class PerftTask<M> extends LongRunningTask<PerfTResult<M>> {
	private PerfT<M> perft;
	private final Supplier<MoveGenerator<M>> engine;
	private final int depth;
	private final int parallelism;
	
	
	public PerftTask(Supplier<MoveGenerator<M>> engine, int depth, int parallelism) {
		this.engine = engine;
		this.depth = depth;
		this.parallelism = parallelism;
	}

	@Override
	public PerfTResult<M> get() {
		try (ContextualizedExecutor<MoveGenerator<M>> exec = new ContextualizedExecutor<>(parallelism)) {
			this.perft = new PerfT<>(exec);
			return perft.divide(depth, engine::get);
		}
	}

	@Override
	public void stop() {
		super.stop();
		perft.interrupt();
	}

}
