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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fathzer.games.perft.Divide;
import com.fathzer.games.perft.PerfT;
import com.fathzer.games.perft.PerfTResult;
import com.fathzer.jchess.uci.option.Option;

/** A class that implements a subset of the <a href="http://wbec-ridderkerk.nl/html/UCIProtocol.html">UCI protocol</a>.
 * <br>It currently misses the following commands:<ul>
 * <li>Dont miss the ShredderChess Annual Barbeque:</li>
 * <li>register</li>
 * <li>go ignores the sub-commands (searchmoves, ponder, etc...). 
 * <li>ponderhit</li>
 * </ul>
 * <br>It also does not recognize commands starting with unknown token (to be honest, it's not very hard to implement but seemed a very bad, error prone, idea to me).
 * <br>It accepts the following extensions:<ul>
 * <li>It can accept different engines, that can be selected using the 'engine' command.<br>
 * You can view these engines as plugins.</li> 
 * <li>engine [engineId] //TODO</li>
 * <li>d [fen]: Displays a textual representation of the game. If the command is followed by 'fen', the representation is the <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a> representation.</li>
 * <li>perft depth [nbThreads]: Run <a href="https://www.chessprogramming.org/Perft">perft</a> test and displays the divide result. depth is mandatory and is the search depth of perft algorithm. It should be strictly positive. NbThreads allowed to process the queries. This number should be strictly positive.<br>Default is 1.</li>
 * <li>q is a shortcut for quit</li>
 * </ul>
 * @see Engine
 */
public class UCI implements Runnable {
	private static final BackgroundTaskManager BACK = new BackgroundTaskManager();
	private static final String MOVES = "moves";
	private static final String ENGINE_CMD = "engine";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnn");
	
	private final Map<String, Consumer<String[]>> executors = new HashMap<>();
	private final Map<String, Engine> engines = new HashMap<>();
	
	private Engine engine;
	private boolean debug = Boolean.getBoolean("debugUCI");
	private boolean debugUCI = false;
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
		for (Option<?> option : engine.getOptions()) {
			out(option.toUCI());
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
		getEngine().setFEN(fen);
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
		if (!BACK.doBackground(task, stopper)) {
			debug("Engine is already working");
		}
	}

	protected void doGo(String[] tokens) {
		if (engine.getFEN()==null) {
			debug("No position defined");
		} else {
			final LongRunningTask<UCIMove> task = engine.go();
			doBackground(() -> out("bestmove "+task.get()), task::stop);
		}
	}
	
	protected void doStop(String[] tokens) {
		if (!BACK.stop()) {
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
	
	protected void doPerft(String[] tokens) {
		if (engine.getFEN()==null) {
			debug("No position defined");
			return;
		}
		if (! (engine instanceof UCIMoveGeneratorProvider)) {
			debug("perft is not supported by this engine");
			return;
		}
		final Optional<Integer> depth = parseInt(tokens, 0, null);
		if (depth.isEmpty()) {
			return;
		}
		if (depth.get()<1) {
			debug("Search depth should be strictly positive");
		}
		final Optional<Integer> parallelism = parseInt(tokens, 1, 1);
		if (parallelism.isEmpty()) {
			return;
		}
		if (parallelism.get()<1) {
			debug("Number of threads should be strictly positive");
		}
		final LongRunningTask<PerfTResult<UCIMove>> task = getPerfTTask(depth.get(), parallelism.get());
		doBackground(() -> doPerft(task, parallelism.get()), task::stop);
	}
	
	protected <M> LongRunningTask<PerfTResult<UCIMove>> getPerfTTask(int depth, int parallelism) {
		final PerfT<M> perft = new PerfT<>(((UCIMoveGeneratorProvider<M>)getEngine())::getMoveGenerator);
		perft.setParallelism(parallelism);
		return new LongRunningTask<PerfTResult<UCIMove>>() {
			@Override
			public PerfTResult<UCIMove> get() {
				final PerfTResult<M> result = perft.divide(depth);
				final UCIMoveGenerator<M> uciMoveGenerator = (UCIMoveGenerator<M>) perft.buildMoveGenerator();
				final Function<Divide<M>, Divide<UCIMove>> mapper = d -> new Divide<>(uciMoveGenerator.toUCI(d.getMove()), d.getCount());
				return new PerfTResult<>(result.getNbMovesMade(), result.getNbMovesFound(), result.getDivides().stream().map(mapper).collect(Collectors.toList()), result.isInterrupted());
			}

			@Override
			public void stop() {
				super.stop();
				perft.interrupt();
			}
		};
	}

	private void doPerft(LongRunningTask<PerfTResult<UCIMove>> task, int parallelism) {
		final long start = System.currentTimeMillis(); 
		final PerfTResult<UCIMove> result = task.get();
		final long duration = System.currentTimeMillis() - start;
		if (result.isInterrupted()) {
			out("perft process has been interrupted");
		} else {
			result.getDivides().stream().forEach(d -> out (d.getMove().toString()+": "+d.getCount()));
			final long sum = result.getNbLeaves();
			out("perft "+f(sum)+" leaves in "+f(duration)+"ms ("+f(sum*1000/duration)+" leaves/s) (using "+parallelism+" thread(s))");
			out("perft "+f(result.getNbMovesFound())+" moves generated ("+f(result.getNbMovesFound()*1000/duration)+" mv/s). " + 
				f(result.getNbMovesMade())+" moves made ("+f(result.getNbMovesMade()*1000/duration)+" mv/s)");
		}
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
				newEngine.setFEN(pos);
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

	protected Optional<Integer> parseInt(String[] tokens, final int index, Integer defaultValue) {
		if (tokens.length<=index) {
			if (defaultValue==null) {
				debug("Missing argument");
			}
			return Optional.ofNullable(defaultValue);
		}
		try {
			return Optional.of(Integer.parseInt(tokens[index]));
		} catch (IllegalArgumentException e) {
			debug("invalid parameter "+tokens[index]);
			return Optional.empty();
		}
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
				BACK.close();
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

	private void out(Throwable e, int level) {
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

	private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));
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
	
	protected void out(CharSequence message) {
    	log(":",message.toString());
		System.out.println(message);
	}
	
	protected void debug(CharSequence message) {
    	log(":","info","UCI debug is", Boolean.toString(debugUCI),message.toString());
		if (debugUCI) {
			System.out.print("info string ");
			System.out.println(message.toString());
		}
	}
}
