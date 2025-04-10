package com.fathzer.jchess.uci.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fathzer.games.HashProvider;
import com.fathzer.games.MoveGenerator;
import com.fathzer.games.MoveGenerator.MoveConfidence;
import com.fathzer.games.ai.evaluation.EvaluatedMove;
import com.fathzer.games.ai.evaluation.Evaluation;
import com.fathzer.games.ai.evaluation.Evaluation.Type;
import com.fathzer.games.ai.evaluation.Evaluator;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.iterativedeepening.SearchHistory;
import com.fathzer.games.ai.time.TimeManager;
import com.fathzer.games.ai.transposition.TranspositionTable;
import com.fathzer.jchess.uci.GoReply;
import com.fathzer.jchess.uci.GoReply.CpScore;
import com.fathzer.jchess.uci.GoReply.Info;
import com.fathzer.jchess.uci.GoReply.MateScore;
import com.fathzer.jchess.uci.GoReply.Score;
import com.fathzer.jchess.uci.StoppableTask;
import com.fathzer.jchess.uci.ClassicalOptions;
import com.fathzer.jchess.uci.Engine;
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
	/** The current board state */
	protected B board;
	/** The time manager */
	protected TimeManager<B> timeManager;
	/** The iterative engine */
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
		final TranspositionTable<M, B> transpositionTable = engine.getTranspositionTable();
		this.ttSizeInMB = transpositionTable==null ? -1 : transpositionTable.getMemorySizeMB();
		this.defaultThreads = engine.getParallelism();
		this.defaultDepth = engine.getDeepeningPolicy().getDepth();
		this.defaultMaxTime = engine.getDeepeningPolicy().getMaxTime();
		this.timeManager = timeManager;
		this.evaluatorBuilders = new HashMap<>();
	}
	
	/** Sets the engine's evaluators.
	 * <br>Calling this method in your engine's constructor will add an <i>evaluation</i> combo option to your engine's default uci options list.
	 * @param evaluators The evaluators to set
	 */
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
		final TranspositionTable<M, B> transpositionTable = engine.getTranspositionTable();
		final int currentSize = transpositionTable==null ? -1 : transpositionTable.getMemorySizeMB();
		if (currentSize!=sizeInMB) {
			engine.setTranspositionTable(sizeInMB<0 ? null : buildTranspositionTable(sizeInMB));
		}
	}

	/** Builds the transposition table.
	 * @param sizeInMB the size of the table in MB
	 * @return the transposition table
	 */
	protected abstract TranspositionTable<M, B> buildTranspositionTable(int sizeInMB);
	
	@Override
	public void newGame() {
		engine.newGame();
	}

	@Override
	public List<Option<?>> getOptions() {
		final List<Option<?>> options = new ArrayList<>();
		if (!evaluatorBuilders.isEmpty()) {
			options.add(new ComboOption("evaluation", this::setEvaluator, defaultEvaluator, evaluatorBuilders.keySet()));
		}
		options.add(ClassicalOptions.threads(this.engine::setParallelism, defaultThreads));
		options.add(ClassicalOptions.multiPV(this.engine.getDeepeningPolicy()::setSize));
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
	public StoppableTask<GoReply> go(GoParameters options) {
		return new StoppableTask<>() {
			@Override
			public GoReply call() {
				final UCIEngineSearchConfiguration<M, B> c = new UCIEngineSearchConfiguration<>(timeManager);
				final UCIEngineSearchConfiguration.EngineConfiguration previous = c.configure(engine, options, board);
				try {
					final List<M> candidates = options.getMoveToSearch().stream().map(AbstractEngine.this::toMove).toList();
					final SearchHistory<M> search = engine.getBestMoves(board, candidates.isEmpty() ? null : candidates);
					if (search.isEmpty()) {
						return new GoReply(null);
					}
					final EvaluatedMove<M> move = getSelected(board, search);
					final GoReply goReply = new GoReply(toUCI(move.getMove()));
					final Info info = new Info(search.getLastDepth());
					final TranspositionTable<M, B> tt = engine.getTranspositionTable();
					final int entryCount = tt.getEntryCount();
					if (entryCount>0) {
						info.setHashFull((int)(1000L*entryCount/tt.getSize()));
					}
					final List<EvaluatedMove<M>> bestMoves = search.getAccurateMoves();
					final Map<String, Optional<Score>> scores = bestMoves.stream().collect(Collectors.toMap(em -> toUCI(em.getMove()).toString(), em -> toScore(em.getEvaluation())));
					info.setScoreBuilder(m -> scores.get(m.toString()));
					final Map<UCIMove, Optional<List<UCIMove>>> pvs = bestMoves.stream().map(EvaluatedMove::getMove).collect(Collectors.toMap(m->toUCI(m), em -> getPV(tt, board, em, info.getDepth())));
					info.setPvBuilder(pvs::get);
					info.setExtraMoves(bestMoves.stream().filter(em -> !move.getMove().equals(em.getMove())).limit(engine.getDeepeningPolicy().getSize()-1L).map(em->toUCI(em.getMove())).toList());
					goReply.setInfo(info);
					return goReply;
				} finally {
					c.set(engine, previous);
				}
			}
			
			@SuppressWarnings("unchecked")
			private <V extends MoveGenerator<M> & HashProvider> Optional<List<UCIMove>> getPV(TranspositionTable<M, B> tt, B board, M move, int depth) {
				return Optional.of(tt.collectPV((V)board, move, depth).stream().map(m -> toUCI(m)).toList());
			}

			@Override
			public void stop() {
				engine.interrupt();
			}
		};
	}
	
	/** Gets the selected move using a search history.
	 * @param board The board state
	 * @param history The search history
	 * @return the selected move (the one that will be returned by the <i>go</i> command)
	 */
	protected EvaluatedMove<M> getSelected(B board, SearchHistory<M> history) {
		return history.getAccurateMoves().get(0);
	}
	
	private Optional<Score> toScore(Evaluation evaluation) {
		final Type type = evaluation.getType();
		if (type==Type.UNKNOWN) {
			return Optional.empty();
		}
		final Score score;
		if (type==Type.EVAL) {
			score = new CpScore(evaluation.getScore());
		} else if (type==Type.WIN) {
			score = new MateScore(evaluation.getCountToEnd());
		} else if (type==Type.LOOSE) {
			score = new MateScore(-evaluation.getCountToEnd());
		} else {
			throw new IllegalArgumentException("Type "+type+" is not supported");
		}
		return Optional.of(score); 
	}
	
	/** 
	 * {@inheritDoc}
	 * <br>The returned instance is directly the current board state (it is not forked)
	 */
	@Override
	public B getMoveGenerator() {
		return board;
	}

	/** Gets the internal engine.
	 * @return an engine
	 */
	public IterativeDeepeningEngine<M, B> getEngine() {
		return engine;
	}
}
