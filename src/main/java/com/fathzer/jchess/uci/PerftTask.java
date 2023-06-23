package com.fathzer.jchess.uci;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.perft.Divide;
import com.fathzer.games.perft.PerfT;
import com.fathzer.games.perft.PerfTResult;
import com.fathzer.games.util.ContextualizedExecutor;

class PerftTask<M> extends LongRunningTask<PerfTResult<UCIMove>> {
	private PerfT<M> perft;
	private final UCIMoveGeneratorProvider<M> engine;
	private final int depth;
	private final int parallelism;
	
	
	public PerftTask(UCIMoveGeneratorProvider<M> engine, int depth, int parallelism) {
		this.engine = engine;
		this.depth = depth;
		this.parallelism = parallelism;
	}

	@Override
	public PerfTResult<UCIMove> get() {
		try (ContextualizedExecutor<MoveGenerator<M>> exec = new ContextualizedExecutor<>(parallelism)) {
			this.perft = new PerfT<>(exec);
			Supplier<MoveGenerator<M>> supplier = engine::getMoveGenerator;
			final PerfTResult<M> result = perft.divide(depth, supplier);
			final UCIMoveGenerator<M> uciMoveGenerator = (UCIMoveGenerator<M>)supplier.get();
			final Function<Divide<M>, Divide<UCIMove>> mapper = d -> new Divide<>(uciMoveGenerator.toUCI(d.getMove()), d.getCount());
			return new PerfTResult<>(result.getNbMovesMade(), result.getNbMovesFound(), result.getDivides().stream().map(mapper).collect(Collectors.toList()), result.isInterrupted());
		}
	}

	@Override
	public void stop() {
		super.stop();
		perft.interrupt();
	}

}
