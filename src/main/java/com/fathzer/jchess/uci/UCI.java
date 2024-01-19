package com.fathzer.jchess.uci;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
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
import com.fathzer.jchess.uci.option.Option;
import com.fathzer.jchess.uci.parameters.GoParameters;
import com.fathzer.jchess.uci.parameters.Parser;

/** A class that implements a subset of the <a href="http://wbec-ridderkerk.nl/html/UCIProtocol.html">UCI protocol</a>.
 * <br>It does not support all UCI commands and contains some extensions. Please have a look at the project's <a href="https://github.com/fathzer-games/jchess-uci/">README</a> file.
 * @see Engine
 */
public class UCI implements Runnable, AutoCloseable {
	private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));
	private static final String MOVES = "moves";
	private static final String ENGINE_CMD = "engine";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnn");
	
	protected Engine engine;
	private final Map<String, Consumer<Deque<String>>> executors = new HashMap<>();
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
			buildOptionsTable(newEngine.getOptions());
			out(ENGINE_CMD+" "+engineId+" ok");
		} else {
			debug(ENGINE_CMD+" "+engineId+" is unknown");
		}
	}
	
	protected Engine getEngine() {
		return engine;
	}
	
	private void buildOptionsTable(List<Option<?>> options) {
		this.options = new HashMap<>();
		options.forEach(o -> this.options.put(o.getName(), o));
	}

	@Override
	public void run() {
		while (true) {
			log("Waiting for command...");
			final String command=getNextCommand();
	    	log(">",command);
			if ("quit".equals(command) || "q".equals(command)) {
				break;
			}
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
