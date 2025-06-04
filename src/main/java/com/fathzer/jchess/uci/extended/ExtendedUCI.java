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
import com.fathzer.games.perft.FromPositionMoveGeneratorBuilder;
import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.StoppableTask;
import com.fathzer.jchess.uci.UCI;
import com.fathzer.jchess.uci.parameters.PerfStatsParameters;
import com.fathzer.jchess.uci.parameters.PerfTParameters;

/** An extended UCI that adds commands to the standard UCI.
 * <br>It adds the following commands:
 * <ul>
 * <li><b>perft</b>: Performs a <a href="https://www.chessprogramming.org/Perft">perft</a> test on the current position (requires the {@link Engine} to implement the {@link MoveGeneratorSupplier} interface)
 *   <br>see {@link PerfTParameters} to know which parameters are available.
 * </li>
 * <li><b>test</b>: Performs a performance test on positions returned by {@link ExtendedUCI#readTestData()} method.
 *   <br>It requires the {@link Engine} to implement the {@link FromPositionMoveGeneratorBuilder} interface) and to override the {@link ExtendedUCI#readTestData()} method.
 *   <br>see {@link PerfStatsParameters} to know which parameters are available.
 * </li>
 * <li><b>d</b>: Displays the current position (requires the {@link Engine} to implement the {@link Displayable} interface).
 *   <br>The <i>fen</i> option can be used to display the position in FEN format.
 * </li>
 * </ul>
 */
 public class ExtendedUCI extends UCI {
	private static final String PERFT_COMMAND = "perft";
	private static final String TEST_COMMAND = "test";
	
	private static final String NO_POSITION_DEFINED = "No position defined";

	/** Constructor
	 * @param defaultEngine The default engine
	 */
	public ExtendedUCI(Engine defaultEngine) {
		super(defaultEngine);
		addCommand(this::doPerft, PERFT_COMMAND);
		addCommand(this::doPerfStat,TEST_COMMAND);
		addCommand(this::doDisplay, "d");
	}
	
	/** Performs the display command (<b>d</b>).
	 * @param tokens The tokens of the command excluding the command name (it contains only the options).
	 */
	protected void doDisplay(Deque<String> tokens) {
		if (!isPositionSet()) {
			debug(NO_POSITION_DEFINED);
			return;
		}
		if (engine instanceof Displayable displayable) {
			final String result;
			if (tokens.isEmpty()) {
				result = displayable.getBoardAsString();
			} else if (tokens.size()==1 && "fen".equals(tokens.peek())) {
				result = ((Displayable)engine).getFEN();
			} else {
				debug("Unknown display options "+Arrays.asList(tokens));
				return;
			}
			out(result);
		} else {
			debug("position display is not supported by this engine");
		}
	}

	/** Performs the perft command (<b>perft</b>).
	 * @param tokens The tokens of the command excluding the command name (it contains only the options).
	 */
	protected void doPerft(Deque<String> tokens) {
		if (!isPositionSet()) {
			debug(NO_POSITION_DEFINED);
			return;
		}
		if (engine instanceof MoveGeneratorSupplier) {
			final Optional<PerfTParameters> params = parse(PerfTParameters::new, PerfTParameters.PARSER, tokens);
			if (params.isPresent()) {
				launchPerfT(params.get());
			}
		} else {
			debug("perft is not supported by this engine");
		}
	}

	private <M> void launchPerfT(final PerfTParameters params) {
		@SuppressWarnings("unchecked")
		final StoppableTask<PerfTResult<M>> task = new PerftTask<>(((MoveGeneratorSupplier<M>)engine)::getMoveGenerator, params);
		if (!doBackground(() -> doPerft(task, params), task::stop, e -> err(PERFT_COMMAND,e))) {
			debug("Engine is already working");
		}
}

	private <M> void doPerft(StoppableTask<PerfTResult<M>> task, PerfTParameters params) throws Exception {
		final long start = System.currentTimeMillis(); 
		final PerfTResult<M> result = task.call();

		final long duration = System.currentTimeMillis() - start;
		if (result.isInterrupted()) {
			out("perft process has been interrupted");
		} else {
			result.getDivides().stream().forEach(d -> out (toString(d.getMove())+": "+d.getNbLeaves()));
			final long sum = result.getNbLeaves();
			out("perft "+f(sum)+" leaves in "+f(duration)+"ms ("+f(sum*1000/duration)+" leaves/s) (using "+params.getParallelism()+" thread(s))");
			out("perft "+f(result.getNbMovesFound())+" "+(params.isLegal()?"":"peudo-")+"legal moves generated ("+f(result.getNbMovesFound()*1000/duration)+" mv/s). " + 
				f(result.getNbMovesMade())+" moves made ("+f(result.getNbMovesMade()*1000/duration)+" mv/s)");
		}
	}
	
	@SuppressWarnings("unchecked")
	private <M> String toString(M move) {
		return (engine instanceof MoveToUCIConverter) ? ((MoveToUCIConverter<M>)engine).toUCI(move).toString() : move.toString();
	}
	
	/** Performs the performance test command (<b>test</b>).
	 * @param tokens The tokens of the command excluding the command name (it contains only the options).
	 */
	protected void doPerfStat(Deque<String> tokens) {
		if (! (engine instanceof FromPositionMoveGeneratorBuilder)) {
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
			doPerfStat(testData, (FromPositionMoveGeneratorBuilder<?,?>)engine, params.get());
		}
	}

	private <M, B extends MoveGenerator<M>> void doPerfStat(Collection<PerfTTestData> testData, FromPositionMoveGeneratorBuilder<M, B> engine, PerfStatsParameters params) {
		final MoveGeneratorChecker test = new MoveGeneratorChecker(testData);
		test.setErrorManager(e-> err(TEST_COMMAND, e));
		test.setCountErrorManager(e -> out("Error for "+e.startPosition()+" expected "+e.expectedCount()+" got "+e.actualCount()));
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
		}, test::interrupt, e -> err(TEST_COMMAND, e));
	}
	
	/** Returns the data set to use with the performance test command (<b>test</b>).
	 * <br>The default implementation returns an empty list
	 * @return The test data
	 */
	protected Collection<PerfTTestData> readTestData() {
		return Collections.emptyList();
	}

	private static String f(long num) {
		return NumberFormat.getInstance().format(num);
	}
}
