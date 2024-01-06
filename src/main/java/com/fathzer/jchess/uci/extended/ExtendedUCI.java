package com.fathzer.jchess.uci.extended;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.perft.MoveGeneratorChecker;
import com.fathzer.games.perft.PerfTResult;
import com.fathzer.games.perft.PerfTTestData;
import com.fathzer.games.perft.TestableMoveGeneratorBuilder;
import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.LongRunningTask;
import com.fathzer.jchess.uci.UCI;
import com.fathzer.jchess.uci.parameters.PerfStatsParameters;
import com.fathzer.jchess.uci.parameters.PerfTParameters;

public class ExtendedUCI extends UCI {
	private static final String NO_POSITION_DEFINED = "No position defined";

	public ExtendedUCI(Engine defaultEngine) {
		super(defaultEngine);
		addCommand(this::doPerft, "perft");
		addCommand(this::doPerfStat,"test");
		addCommand(this::doDisplay, "d");
	}
	
	protected void doDisplay(Deque<String> tokens) {
		if (!isPositionSet) {
			debug(NO_POSITION_DEFINED);
			return;
		}
		if (! (engine instanceof Displayable)) {
			debug("position display is not supported by this engine");
		}
		final String result;
		if (tokens.isEmpty()) {
			result = ((Displayable)getEngine()).getBoardAsString();
		} else if (tokens.size()==1 && "fen".equals(tokens.peek())) {
			result = ((Displayable)getEngine()).getFEN();
		} else {
			debug("Unknown display options "+Arrays.asList(tokens));
			return;
		}
		out(result);
	}

	protected <M> void doPerft(Deque<String> tokens) {
		if (!isPositionSet) {
			debug(NO_POSITION_DEFINED);
			return;
		}
		if (! (engine instanceof MoveGeneratorSupplier)) {
			debug("perft is not supported by this engine");
			return;
		}
		final Optional<PerfTParameters> params = parse(PerfTParameters::new, PerfTParameters.PARSER, tokens);
		if (params.isPresent()) {
			@SuppressWarnings("unchecked")
			final LongRunningTask<PerfTResult<M>> task = new PerftTask<>((MoveGeneratorSupplier<M>)engine, params.get());
			doBackground(() -> doPerft(task, params.get()), task::stop);
		}
	}

	private <M> void doPerft(LongRunningTask<PerfTResult<M>> task, PerfTParameters params) {
		final long start = System.currentTimeMillis(); 
		final PerfTResult<M> result = task.get();

		final long duration = System.currentTimeMillis() - start;
		if (result.isInterrupted()) {
			out("perft process has been interrupted");
		} else {
			result.getDivides().stream().forEach(d -> out (toString(d.getMove())+": "+d.getCount()));
			final long sum = result.getNbLeaves();
			out("perft "+f(sum)+" leaves in "+f(duration)+"ms ("+f(sum*1000/duration)+" leaves/s) (using "+params.getParallelism()+" thread(s))");
			out("perft "+f(result.getNbMovesFound())+" "+(params.isLegal()?"":"peudo-")+"legal moves generated ("+f(result.getNbMovesFound()*1000/duration)+" mv/s). " + 
				f(result.getNbMovesMade())+" moves made ("+f(result.getNbMovesMade()*1000/duration)+" mv/s)");
		}
	}
	
	@SuppressWarnings("unchecked")
	private <M> String toString(M move) {
		return (getEngine() instanceof MoveToUCIConverter) ? ((MoveToUCIConverter<M>)engine).toUCI(move).toString() : move.toString();
	}
	
	protected void doPerfStat(Deque<String> tokens) {
		if (! (getEngine() instanceof TestableMoveGeneratorBuilder)) {
			debug("test is not supported by this engine");
			return;
		}
		final Optional<PerfStatsParameters> params = parse(PerfStatsParameters::new, PerfStatsParameters.PARSER, tokens);
		if (params.isPresent()) {
			final Collection<PerfTTestData> testData = readTestData();
			if (testData.isEmpty()) {
				out("No test data available");
				debug("You may override readTestData to read some data");
				return;
			}
			doPerfStat(testData, (TestableMoveGeneratorBuilder<?,?>)getEngine(), params.get());
		}
	}

	private <M, B extends MoveGenerator<M>> void doPerfStat(Collection<PerfTTestData> testData, TestableMoveGeneratorBuilder<M, B> engine, PerfStatsParameters params) {
		final MoveGeneratorChecker test = new MoveGeneratorChecker(testData);
		test.setErrorManager(e-> out(e,0));
		test.setCountErrorManager(e -> out("Error for "+e.getStartPosition()+" expected "+e.getExpectedCount()+" got "+e.getActualCount()));
		final TimerTask task = new TimerTask() {
			@Override
			public void run() {
				doStop(null);
			}
		};
		doBackground(() -> {
			final Timer timer = new Timer();
			timer.schedule(task, 1000L*params.getCutTime());
			try {
				final long start = System.currentTimeMillis();
				long sum = test.run(engine, params.getDepth(), params.isLegal() , params.isPlayLeaves(), params.getParallelism());
				final long duration = System.currentTimeMillis() - start;
				out("perf: "+f(sum)+(params.isLegal()?" ":" pseudo-")+"legal moves in "+f(duration)+"ms ("+f(sum*1000/duration)+" mv/s) (using "+params.getParallelism()+" thread(s) and "+(params.isPlayLeaves()||!params.isLegal()?"":"not ")+"playing leave moves)");
			} finally {
				timer.cancel();
			}
		}, test::cancel);
	}
	
	protected Collection<PerfTTestData> readTestData() {
		return Collections.emptyList();
	}

	private static String f(long num) {
		return NumberFormat.getInstance().format(num);
	}
}
