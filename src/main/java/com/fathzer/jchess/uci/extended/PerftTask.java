package com.fathzer.jchess.uci.extended;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.perft.PerfTBuilder;
import com.fathzer.games.perft.PerfTResult;
import com.fathzer.games.perft.PerfT;
import com.fathzer.jchess.uci.StoppableTask;
import com.fathzer.jchess.uci.parameters.PerfTParameters;

class PerftTask<M> implements StoppableTask<PerfTResult<M>> {
	private PerfT<M> perft;
	private final Supplier<MoveGenerator<M>> engine;
	private final PerfTParameters params;
	
	
	public PerftTask(Supplier<MoveGenerator<M>> engine, PerfTParameters params) {
		this.engine = engine;
		this.params = params;
	}

	@Override
	public PerfTResult<M> call() {
		final PerfTBuilder<M> builder = new PerfTBuilder<>();
		if (params.isLegal()) {
			builder.setLegalMoves(true);
			if (!params.isPlayLeaves()) {
				builder.setPlayLeaves(false);
			}
		}
		final ForkJoinPool exec = new ForkJoinPool(params.getParallelism());
		try {
			builder.setExecutor(exec);
			this.perft = builder.build(engine.get(), params.getDepth());
			return perft.get();
		} finally {
			exec.shutdown();
		}
	} 

	@Override
	public void stop() {
		perft.interrupt();
	}

}
