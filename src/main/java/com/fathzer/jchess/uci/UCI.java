package com.fathzer.jchess.uci;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fathzer.jchess.uci.option.CheckOption;
import com.fathzer.jchess.uci.option.IntegerSpinOption;
import com.fathzer.jchess.uci.option.Option;
import com.fathzer.jchess.uci.parameters.GoParameters;
import com.fathzer.jchess.uci.parameters.Parser;

/** A class that implements a subset of the <a href="http://wbec-ridderkerk.nl/html/UCIProtocol.html">UCI protocol</a>.
 * <br>It does not support all UCI commands and contains some extensions. Please have a look at the project's <a href="https://github.com/fathzer-games/jchess-uci/">README</a> file.
 * @see Engine
 */
public class UCI implements Runnable, AutoCloseable {
	public static final String INIT_COMMANDS_PROPERTY_FILE = "uciInitCommands";
	
	private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));
	private static final String MOVES = "moves";
	private static final String ENGINE_CMD = "engine";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnn");
	
	private static final String CHESS960_OPTION = "UCI_Chess960";
	private static final String HASH_OPTION = "Hash";
	private static final String OWN_BOOK_OPTION = "OwnBook";
	
	protected Engine engine;
	private final Map<String, Consumer<Deque<String>>> executors = new HashMap<>();
	private final Map<String, Engine> engines = new HashMap<>();
	
	private final BackgroundTaskManager backTasks = new BackgroundTaskManager(e -> out(e, 0));
	private boolean debug = Boolean.getBoolean("logToFile");
	private boolean debugUCI = Boolean.getBoolean("debugUCI");
	private Map<String, Option<?>> options;
	
	public UCI(Engine defaultEngine) {
		engines.put(defaultEngine.getId(), defaultEngine);
		this.engine = defaultEngine;
		buildOptionsTable();
		addCommand(this::doUCI, "uci");
		addCommand(this::doDebug, "debug");
		addCommand(this::doSetOption, "setoption");
		addCommand(this::doIsReady, "isready");
		addCommand(this::doNewGame, "ucinewgame", "ng");
		addCommand(this::doPosition, "position");
		addCommand(this::doGo, "go");
		addCommand(this::doStop, "stop");
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

	protected void addCommand(Consumer<Deque<String>> method, String... commands) {
		Arrays.stream(commands).forEach(c -> executors.put(c, method));
	}
	
	protected void doDebug(Deque<String> tokens) {
		if (tokens.size()==1) {
			String arg = tokens.pop();
			if ("on".equals(arg)) {
				debugUCI = true;
			} else if ("off".equals(arg)) {
				debugUCI = false;
			} else {
				debug("Wrong argument "+arg);
			}
		} else {
			debug("Expected 1 argument to this command");
		}
	}

	protected void doUCI(Deque<String> tokens) {
		out("id name "+engine.getId());
		final String author = engine.getAuthor();
		if (author!=null) {
			out("id author "+author);
		}
		options.values().forEach( o -> out(o.toUCI()));
		out("uciok");
	}
	
	private String processOption(Deque<String> tokens) {
		if (tokens.size()<2) {
			return "Missing name prefix or option name";
		}
		if (!"name".equals(tokens.peek())) {
			return "setoption command should start with name";
		}
		// Be aware that option name can be contained by more than 1 token
		final String name = tokens.stream().skip(1).takeWhile(t->!"value".equals(t)).collect(Collectors.joining(" "));
		final String value = tokens.stream().dropWhile(t->!"value".equals(t)).skip(1).collect(Collectors.joining(" "));
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
	
	protected void doSetOption(Deque<String> tokens) {
		final String error = processOption(tokens);
		if (error!=null) {
			debug(error);
		}
	}
	
	protected void doIsReady(Deque<String> tokens) {
		out("readyok");
	}

	protected void doNewGame(Deque<String> tokens) {
		getEngine().newGame();
	}

	protected void doPosition(Deque<String> tokens) {
		final String first = tokens.pop();
		final String fen;
		if ("fen".equals(first)) {
			fen = getFEN(tokens);
		} else if ("startpos".equals(first)) {
			fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		} else {
			debug("invalid position definition");
			return;
		}
		log("Setting board to FEN",fen);
		getEngine().setStartPosition(fen);
		tokens.stream().dropWhile(t->!MOVES.equals(t)).skip(1).forEach(this::doMove);
	}
	
	private void doMove(String move) {
		log("Moving",move);
		getEngine().move(UCIMove.from(move));
	}
	
	private String getFEN(Collection<String> tokens) {
		return tokens.stream().takeWhile(t -> !MOVES.equals(t)).collect(Collectors.joining(" "));
	}
	
	/** Launches a task on the background thread.
	 * @param task The task to launch
	 * @param stopper A runnable that stops the task when invoked (it is user by the <i>stop</i> command in order to stop the task. 
	 * @return true if the task is launched, false if another task is already running.
	 */
	protected boolean doBackground(Runnable task, Runnable stopper) {
		return backTasks.doBackground(task, stopper);
	}

	protected void doGo(Deque<String> tokens) {
		if (!engine.isPositionSet()) {
			debug("No position defined");
		} else {
			final Optional<GoParameters> goOptions = parse(GoParameters::new, GoParameters.PARSER, tokens);
			if (goOptions.isPresent()) {
				final LongRunningTask<BestMoveReply> task = engine.go(goOptions.get());
				final boolean started = doBackground(() -> {
					final BestMoveReply reply = task.get();
					out("bestmove "+reply.getMove()+(reply.getPonderMove().isEmpty()?"":(" "+reply.getPonderMove().get())));
				}, task::stop);
				if (!started) {
					debug("Engine is already working");
				}
			}
		}
	}

	protected <T> Optional<T> parse(Supplier<T> builder, Parser<T> parser, Deque<String> tokens) {
		try {
			final T result = builder.get();
			final List<String> ignored = parser.parse(result, tokens);
			if (!ignored.isEmpty()) {
				debug("The following parameters were ignored "+ignored);
			}
			return Optional.of(result);
		} catch (IllegalArgumentException e) {
			debug("There's an illegal argument in "+tokens);
			return Optional.empty();
		}
	}

	protected void doStop(Deque<String> tokens) {
		if (!backTasks.stop()) {
			debug("Nothing to stop");
		}
	}
	
	protected void doEngine(Deque<String> tokens) {
		if (tokens.isEmpty()) {
			out(ENGINE_CMD+" "+engine.getId());
			engines.keySet().stream().filter(engineId -> !engineId.equals(engine.getId())).forEach(engineId -> out(ENGINE_CMD+" "+engineId));
			return;
		}
		final String engineId = tokens.peek();
		final Engine newEngine = engines.get(engineId);
		if (newEngine!=null) {
			if (newEngine.equals(this.engine)) {
			 return;	
			}
			if (engine.isPositionSet()) {
				debug("position is cleared by engine change");
			}
			this.engine = newEngine;
			buildOptionsTable();
			out(ENGINE_CMD+" "+engineId+" ok");
		} else {
			debug(ENGINE_CMD+" "+engineId+" is unknown");
		}
	}
	
	protected Engine getEngine() {
		return engine;
	}
	
	private void buildOptionsTable() {
		final List<Option<?>> engineOptions = engine.getOptions();
		this.options = new HashMap<>();
		engineOptions.forEach(o -> this.options.put(o.getName(), o));
		if (engine.isChess960Supported()) {
			options.computeIfAbsent(CHESS960_OPTION, k -> new CheckOption(k, engine::setChess960, false));
		}
		if (engine.hasOwnBook()) {
			options.computeIfAbsent(OWN_BOOK_OPTION, k -> new CheckOption(k, engine::setOwnBook, true));
		}
		if (engine.getDefaultHashTableSize()>=0) {
			options.computeIfAbsent(HASH_OPTION, k -> new IntegerSpinOption(k, engine::setHashTableSize, engine.getDefaultHashTableSize(), 1, 64*1024));
		}
	}

	@Override
	public void run() {
		init();
		while (true) {
			log("Waiting for command...");
			final String command=getNextCommand();
			if ("quit".equals(command) || "q".equals(command)) {
		    	log(">",command);
				break;
			}
			doCommand(command);
		}
	}
	
	private void init() {
		final String initFile = System.getProperty(INIT_COMMANDS_PROPERTY_FILE);
		if (initFile!=null) {
			try {
				Files.readAllLines(Paths.get(initFile)).stream().map(String::trim).filter(s -> !s.isEmpty()).forEach(this::doCommand);
			} catch (IOException e) {
				out(e,0);
			}
		}

	}

	/** Executes a command.
	 * @param command The command to execute
	 */
	protected void doCommand(final String command) {
    	log(">",command);
		final Deque<String> tokens = new LinkedList<>(Arrays.asList(command.split(" ")));
		if (!command.isEmpty() && !tokens.isEmpty()) {
			final Consumer<Deque<String>> executor = executors.get(tokens.pop());
			if (executor==null) {
				debug("unknown command");
			} else {
				try {
					executor.accept(tokens);
				} catch (RuntimeException e) {
					out(e,0);
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
	    try {
	        line = System.console() == null ? IN.readLine() : System.console().readLine();
	        if (line==null) {
	        	throw new EOFException("End of system input has been reached");
	        }
	    } catch (IOException e) {
	    	throw new UncheckedIOException(e);
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

	@Override
	public void close() {
		backTasks.close();
	}
}
