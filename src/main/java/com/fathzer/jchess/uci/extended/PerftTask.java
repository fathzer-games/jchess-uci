package com.fathzer.jchess.uci.extended;

import java.util.function.Supplier;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.perft.PerfT;
import com.fathzer.games.perft.PerfTResult;
import com.fathzer.games.util.exec.ContextualizedExecutor;
import com.fathzer.jchess.uci.LongRunningTask;
import com.fathzer.jchess.uci.parameters.PerfTParameters;

class PerftTask<M> extends LongRunningTask<PerfTResult<M>> {
	private PerfT<M> perft;
	private final Supplier<MoveGenerator<M>> engine;
	private final PerfTParameters params;
	
	
	public PerftTask(Supplier<MoveGenerator<M>> engine, PerfTParameters params) {
		this.engine = engine;
		this.params = params;
	}

	@Override
	public PerfTResult<M> get() {
		try (ContextualizedExecutor<MoveGenerator<M>> exec = new ContextualizedExecutor<>(params.getParallelism())) {
			this.perft = new PerfT<>(exec);
			if (params.isLegal()) {
				this.perft.setLegalMoves(true);
				if (!params.isPlayLeaves()) {
					this.perft.setPlayLeaves(false);
				}
			}
			return perft.divide(params.getDepth(), engine.get());
		}
	}

	@Override
	public void stop() {
		super.stop();
		perft.interrupt();
	}

}
