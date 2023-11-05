package com.fathzer.jchess.uci;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fathzer.games.perft.TestableMoveGeneratorSupplier;
import com.fathzer.games.perft.MoveGeneratorChecker;
import com.fathzer.games.perft.PerfTResult;
import com.fathzer.games.perft.PerfTTestData;
import com.fathzer.jchess.uci.option.CheckOption;
import com.fathzer.jchess.uci.option.Option;

/** A class that implements a subset of the <a href="http://wbec-ridderkerk.nl/html/UCIProtocol.html">UCI protocol</a>.
 * <br>It does not support all UCI commands and contains some extensions. Please have a look at the project's <a href="https://github.com/fathzer-games/jchess-uci/">README</a> file.
 * @see Engine
 */
public class UCI implements Runnable {
	private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));
	private static final String MOVES = "moves";
	private static final String ENGINE_CMD = "engine";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnn");
	
	private Engine engine;
	private final Map<String, Consumer<String[]>> executors = new HashMap<>();
	private final Map<String, Engine> engines = new HashMap<>();
	
	private final BackgroundTaskManager backTasks = new BackgroundTaskManager(e -> out(e, 0));
	private final Option<Boolean> chess960Option = new CheckOption("UCI_Chess960", b -> {if (engine!=null) engine.setChess960(b);}, false);
	private boolean debug = Boolean.getBoolean("logToFile");
	private boolean debugUCI = Boolean.getBoolean("debugUCI");
	private Map<String, Option<?>> options;
	
	public UCI(Engine defaultEngine) {
		engines.put(defaultEngine.getId(), defaultEngine);
		this.engine = defaultEngine;
		buildOptionsTable(defaultEngine.getOptions());
		addCommand(this::doUCI, "uci");
		addCommand(this::doDebug, "debug");
		addCommand(this::doSetOption, "setoption");
		addCommand(this::doIsReady, "isready");
		addCommand(this::doNewGame, "ucinewgame", "ng");
		addCommand(this::doPosition, "position");
		addCommand(this::doGo, "go");
		addCommand(this::doStop, "stop");
		addCommand(this::doDisplay, "d");
		addCommand(this::doPerft, "perft");
		addCommand(this::doEngine,ENGINE_CMD);
		addCommand(this::doPerfStat,"test");
		if (System.console()!=null) {
			log(false, "Input from System.console()");
		} else {
			log(false, "Input from System.in");
		}
	}
	
	public void add(Engine engine) {
		if (engines.containsKey(engine.getId())) {
			throw new IllegalArgumentException("There's already an engine with id "+engine.getId());
		}
		engines.put(engine.getId(), engine);
	}

	protected void addCommand(Consumer<String[]> method, String... commands) {
		Arrays.stream(commands).forEach(c -> executors.put(c, method));
	}
	
	protected void doDebug(String[] tokens) {
		if (tokens.length==1) {
			if ("on".equals(tokens[0])) {
				debugUCI = true;
			} else if ("off".equals(tokens[0])) {
				debugUCI = false;
			} else {
				debug("Wrong argument "+tokens[0]);
			}
		} else {
			debug("Expected 1 argument to this command");
		}
	}

	protected void doUCI(String[] tokens) {
		out("id name "+engine.getId());
		final String author = engine.getAuthor();
		if (author!=null) {
			out("id author "+author);
		}
		boolean hasChess960 = false;
		for (Option<?> option : options.values()) {
			if (chess960Option.getName().equals(option.getName())) {
				hasChess960 = true;
			}
			out(option.toUCI());
		}
		if (engine.isChess960Supported() && !hasChess960) {
			out(chess960Option.toUCI());
		}
		out("uciok");
	}
	
	private String processOption(String[] tokens) {
		if (tokens.length<2) {
			return "Missing name prefix or option name";
		}
		if (!"name".equals(tokens[0])) {
			return "setoption command should start with name";
		}
		// Be aware that option name can be contained by more than 1 token
		final String name = Arrays.stream(tokens).skip(1).takeWhile(t->!"value".equals(t)).collect(Collectors.joining(" "));
		final String value = Arrays.stream(tokens).dropWhile(t->!"value".equals(t)).skip(1).collect(Collectors.joining(" "));
		if (name.isEmpty()) {
			return "Option name is empty";
		}
		final Option<?> option = options.get(name);
		if (option==null) {
			return "Unknown option";
		}
		try {
			option.setValue(value.isEmpty()?null:value);
			return null;
		} catch (IllegalArgumentException e) {
			return "Value "+value+" is illegal";
		}
	}
	
	protected void doSetOption(String[] tokens) {
		final String error = processOption(tokens);
		if (error!=null) {
			debug(error);
		}
	}
	
	protected void doIsReady(String[] tokens) {
		out("readyok");
	}

	protected void doNewGame(String[] tokens) {
		getEngine().newGame();
	}

	protected void doPosition(String[] tokens) {
		final String fen;
		if ("fen".equals(tokens[0])) {
			fen = getFEN(Arrays.copyOfRange(tokens, 1, tokens.length));
		} else if ("startpos".equals(tokens[0])) {
			fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		} else {
			debug("invalid position definition");
			return;
		}
		log("Setting board to FEN",fen);
		getEngine().setStartPosition(fen);
		Arrays.stream(tokens).dropWhile(t->!MOVES.equals(t)).skip(1).forEach(this::doMove);
	}
	
	private void doMove(String move) {
		log("Moving",move);
		getEngine().move(UCIMove.from(move));
	}
	
	private String getFEN(String[] tokens) {
		return Arrays.stream(tokens).takeWhile(t -> !MOVES.equals(t)).collect(Collectors.joining(" "));
	}
	
	protected void doBackground(Runnable task, Runnable stopper) {
		if (!backTasks.doBackground(task, stopper)) {
			debug("Engine is already working");
		}
	}

	protected void doGo(String[] tokens) {
		if (engine.getFEN()==null) {
			debug("No position defined");
		} else {
			final Optional<GoOptions> goOptions = getParams(Arrays.asList(tokens));
			if (goOptions.isPresent()) {
				final LongRunningTask<BestMoveReply> task = engine.go(goOptions.get());
				doBackground(() -> {
					final BestMoveReply reply = task.get();
					out("bestmove "+reply.getMove()+(reply.getPonderMove().isEmpty()?"":(" "+reply.getPonderMove().get())));
				}, task::stop);
			}
		}
	}
	private Optional<GoOptions> getParams(List<String> tokens) {
		try {
			final GoOptions result = new GoOptions(tokens);
			debug("The following go options were ignored "+result.getIgnoredOptions());
			return Optional.of(result);
		} catch (IllegalArgumentException e) {
			debug("There's illegal argument in the go options "+tokens);
			return Optional.empty();
		}
	}
	
	protected void doStop(String[] tokens) {
		if (!backTasks.stop()) {
			debug("Nothing to stop");
		}
	}
	
	protected void doDisplay(String[] tokens) {
		if (tokens.length==0) {
			out(getEngine().getBoardAsString());
		} else if (tokens.length==1 && "fen".equals(tokens[0])) {
			out(getEngine().getFEN());
		} else {
			debug("Unknown display options "+Arrays.asList(tokens));
		}
	}
	
	protected <M> void doPerft(String[] tokens) {
		if (engine.getFEN()==null) {
			debug("No position defined");
			return;
		}
		if (! (engine instanceof MoveGeneratorSupplier)) {
			debug("perft is not supported by this engine");
			return;
		}
		Optional<List<Integer>> params = new ParamsParser<>(this::debug, Integer::parseInt, (i,v) -> v>0).parse(tokens, Arrays.asList("search depth", "number of threads"), Arrays.asList(null, 1));
		if (params.isEmpty()) {
			return;
		}
		final int depth = params.get().get(0);
		final int parallelism = params.get().get(1);
		@SuppressWarnings("unchecked")
		final LongRunningTask<PerfTResult<M>> task = new PerftTask<>((MoveGeneratorSupplier<M>)engine, depth, parallelism);
		doBackground(() -> doPerft(task, parallelism), task::stop);
	}

	private <M> void doPerft(LongRunningTask<PerfTResult<M>> task, int parallelism) {
		final long start = System.currentTimeMillis(); 
		final PerfTResult<M> result = task.get();

		final long duration = System.currentTimeMillis() - start;
		if (result.isInterrupted()) {
			out("perft process has been interrupted");
		} else {
			result.getDivides().stream().forEach(d -> out (toString(d.getMove())+": "+d.getCount()));
			final long sum = result.getNbLeaves();
			out("perft "+f(sum)+" leaves in "+f(duration)+"ms ("+f(sum*1000/duration)+" leaves/s) (using "+parallelism+" thread(s))");
			out("perft "+f(result.getNbMovesFound())+" moves generated ("+f(result.getNbMovesFound()*1000/duration)+" mv/s). " + 
				f(result.getNbMovesMade())+" moves made ("+f(result.getNbMovesMade()*1000/duration)+" mv/s)");
		}
	}
	
	private <M> String toString(M move) {
		return (getEngine() instanceof MoveToUCIConverter) ? ((MoveToUCIConverter<M>)engine).toUCI(move) : move.toString();
	}
	
	protected void doPerfStat(String[] tokens) {
		if (! (getEngine() instanceof TestableMoveGeneratorSupplier)) {
			debug("test is not supported by this engine");
		}
		final Optional<List<Integer>> params = new ParamsParser<>(this::debug, Integer::parseInt, (i,v)->v>0).parse(tokens, Arrays.asList("search depth", "number of threads", "cut time"), Arrays.asList(null,1,Integer.MAX_VALUE));
		if (params.isEmpty()) {
			return;
		}
		final Collection<PerfTTestData> testData = readTestData();
		if (testData.isEmpty()) {
			out("No test data available");
			debug("You may override readTestData to read some data");
			return;
		}
		final int depth = params.get().get(0);
		final int parallelism = params.get().get(1);
		final int cutTime = params.get().get(2);
		doPerfStat(testData, (TestableMoveGeneratorSupplier<?>)getEngine(), depth, parallelism, cutTime);
	}

	private <M> void doPerfStat(Collection<PerfTTestData> testData, TestableMoveGeneratorSupplier<M> engine, int depth, final int parallelism, int cutTime) {
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
			timer.schedule(task, 1000L*cutTime);
			try {
				final long start = System.currentTimeMillis();
				long sum = test.run(depth, parallelism, engine);
				final long duration = System.currentTimeMillis() - start;
				out("perf: "+f(sum)+" moves in "+f(duration)+"ms ("+f(sum*1000/duration)+" mv/s) (using "+parallelism+" thread(s))");
			} finally {
				timer.cancel();
			}
			
		}, test::cancel);
	}
	
	protected Collection<PerfTTestData> readTestData() {
		return Collections.emptyList();
	}

	protected void doEngine(String[] tokens) {
		if (tokens.length==0) {
			out(ENGINE_CMD+" "+engine.getId());
			engines.keySet().stream().filter(engineId -> !engineId.equals(engine.getId())).forEach(engineId -> out(ENGINE_CMD+" "+engineId));
			return;
		}
		final String engineId = tokens[0];
		final Engine newEngine = engines.get(engineId);
		if (newEngine!=null) {
			if (newEngine.equals(this.engine)) {
			 return;	
			}
			final String pos = getEngine().getFEN();
			if (pos!=null) {
				newEngine.setStartPosition(pos);
			}
			this.engine = newEngine;
			buildOptionsTable(newEngine.getOptions());
			out(ENGINE_CMD+" "+engineId+" ok");
		} else {
			debug(ENGINE_CMD+" "+engineId+" is unknown");
		}
	}
	
	protected Engine getEngine() {
		return engine;
	}
	
	private void buildOptionsTable(Option<?>[] options) {
		this.options = new HashMap<>();
		Arrays.stream(options).forEach(o -> this.options.put(o.getName(), o));
	}

	private static String f(long num) {
		return NumberFormat.getInstance().format(num);
	}

	@Override
	public void run() {
		while (true) {
			log("Waiting for command...");
			final String command=getNextCommand();
	    	log(">",command);
			if ("quit".equals(command) || "q".equals(command)) {
				backTasks.close();
				break;
			}
			final String[] tokens = command.split(" ");
			if (!command.isEmpty() && tokens.length>0) {
				final Consumer<String[]> executor = executors.get(tokens[0]);
				if (executor==null) {
					debug("unknown command");
				} else {
					try {
						executor.accept(Arrays.copyOfRange(tokens, 1, tokens.length));
					} catch (RuntimeException e) {
						out(e,0);
					}
				}
			}
		}
	}

	protected void out(Throwable e, int level) {
		out((level>0 ? "caused by":"")+e.toString());
		Arrays.stream(e.getStackTrace()).forEach(f -> out(f.toString()));
		if (e.getCause()!=null) {
			out(e.getCause(),level+1);
		}
	}
	
	private void log(String... message) {
		log(true, message);
	}

	private synchronized void log(boolean append, String... messages) {
		if (!debug) {
			return;
		}
		try (BufferedWriter out=new BufferedWriter(new FileWriter("log.txt", append))) {
			out.write(LocalDateTime.now().format(DATE_FORMAT));
			out.write(" - ");
			for (String mess : messages) {
				out.write(mess);
				out.write(' ');
			}
			out.newLine();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Gets the next command from UCI client.
	 * <br>This method blocks until a command is available.
	 * <br>One can override this method in order to get commands from somewhere other than standard console input.
	 * @return The net command
	 */
	protected String getNextCommand() {
		String line;
	    if (System.console() != null) {
	        line = System.console().readLine();
	    } else {
		    try {
		    	line = IN.readLine();
		    } catch (IOException e) {
		    	throw new UncheckedIOException(e);
		    }
	    }
    	return line.trim();
	}
	
	/** Send a reply to UCI client.
	 * <br>One can override this method in order to send replies to somewhere other than standard console input.
	 * @param message The reply to send.
	 */
	@SuppressWarnings("java:S106")
	protected void out(CharSequence message) {
    	log(":",message.toString());
		System.out.println(message);
	}
	
	@SuppressWarnings("java:S106")
	protected void debug(CharSequence message) {
    	log(":","info","UCI debug is", Boolean.toString(debugUCI),message.toString());
		if (debugUCI) {
			System.out.print("info string ");
			System.out.println(message.toString());
		}
	}
}
