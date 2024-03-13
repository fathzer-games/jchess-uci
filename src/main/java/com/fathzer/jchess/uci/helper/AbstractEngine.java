package com.fathzer.jchess.uci.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.MoveGenerator.MoveConfidence;
import com.fathzer.games.ai.evaluation.Evaluator;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.time.TimeManager;
import com.fathzer.games.ai.transposition.TranspositionTable;
import com.fathzer.jchess.uci.BestMoveReply;
import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.LongRunningTask;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.extended.MoveGeneratorSupplier;
import com.fathzer.jchess.uci.extended.MoveToUCIConverter;
import com.fathzer.jchess.uci.option.ComboOption;
import com.fathzer.jchess.uci.option.IntegerSpinOption;
import com.fathzer.jchess.uci.option.LongSpinOption;
import com.fathzer.jchess.uci.option.Option;
import com.fathzer.jchess.uci.parameters.GoParameters;

/** An abstract UCI engine based on an internal IterativeDeepeningEngine.
 * <br>This class helps the implementor by managing the internal engine configuration with go command arguments and some other smaller things.
 * <br>It also implements the following options:<ul>
 * <li>threads: The number of threads used by the engine</li>
 * <li>evaluations: If your subclass defines a set of evaluators using {@link #setEvaluators(List)}, this option sets the evaluator</li>
 * <li>depth: The default (the one used if go option does not specifies any depth) engine's maximum search depth</li>
 * <li>maxtime: The default (the one used if go option does not specifies any time information) time allocated to the search</li>
 * </ul>
 * <br>It significantly reduces the amount and complexity of code to write to get a working uci engine.
 * @param <M> The type of a move
 * @param <B> The type of the IterativeDeepeningEngine underlying move generator
 */
public abstract class AbstractEngine<M, B extends MoveGenerator<M>> implements Engine, MoveGeneratorSupplier<M>, MoveToUCIConverter<M> {
	protected B board;
	protected TimeManager<B> timeManager;
	protected IterativeDeepeningEngine<M, B> engine;
	private Map<String, Supplier<Evaluator<M, B>>> evaluatorBuilders;
	private String defaultEvaluator;
	private int defaultThreads;
	private int defaultDepth;
	private long defaultMaxTime;
	private int ttSizeInMB;
	
	/** Constructor.
	 * @param engine The internal engine to use
	 * @param timeManager The time manager that will decide how much time to allocate to a go search
	 */
	protected AbstractEngine(IterativeDeepeningEngine<M, B> engine, TimeManager<B> timeManager) {
		this.engine = engine;
		this.ttSizeInMB = engine.getTranspositionTable().getMemorySizeMB();
		this.defaultThreads = engine.getParallelism();
		this.defaultDepth = engine.getDeepeningPolicy().getDepth();
		this.defaultMaxTime = engine.getDeepeningPolicy().getMaxTime();
		this.timeManager = timeManager;
		this.evaluatorBuilders = new HashMap<>();
	}
	
	protected void setEvaluators(List<EvaluatorConfiguration<M, B>> evaluators) {
		evaluatorBuilders.clear();
		defaultEvaluator = evaluators.isEmpty() ? null : evaluators.get(0).getName();
		evaluators.forEach(e -> evaluatorBuilders.put(e.getName(), e.getBuilder()));
	}
	
	@Override
	public int getDefaultHashTableSize() {
		return ttSizeInMB;
	}

	@Override
	public void setHashTableSize(int sizeInMB) {
		if (engine.getTranspositionTable().getMemorySizeMB()!=sizeInMB) {
			engine.setTranspositionTable(buildTranspositionTable(sizeInMB));
		}
	}

	protected abstract TranspositionTable<M> buildTranspositionTable(int sizeInMB);
	
	@Override
	public List<Option<?>> getOptions() {
		final List<Option<?>> options = new ArrayList<>();
		if (!evaluatorBuilders.isEmpty()) {
			options.add(new ComboOption("evaluation", this::setEvaluator, defaultEvaluator, evaluatorBuilders.keySet()));
		}
		options.add(new IntegerSpinOption("threads", this.engine::setParallelism, defaultThreads, 1, Runtime.getRuntime().availableProcessors()));
		options.add(new IntegerSpinOption("depth", this.engine.getDeepeningPolicy()::setDepth, defaultDepth, 1, 128));
		options.add(new LongSpinOption("maxtime", this.engine.getDeepeningPolicy()::setMaxTime, defaultMaxTime, 1, Long.MAX_VALUE));
		return options;
	}
	
	private void setEvaluator(String evaluatorName) {
		final Supplier<Evaluator<M, B>> builder = evaluatorBuilders.get(evaluatorName);
		if (builder==null) {
			throw new IllegalArgumentException();
		}
		engine.setEvaluatorSupplier(builder);
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
				final List<M> candidates = options.getMoveToSearch().stream().map(AbstractEngine.this::toMove).toList();
				final M move = engine.getBestMove(board, candidates.isEmpty() ? null : candidates);
				c.set(engine, previous);
				return new BestMoveReply(move==null ? null : toUCI(move));
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
	
	@Override
	public boolean isPositionSet() {
		return board!=null;
	}
}
