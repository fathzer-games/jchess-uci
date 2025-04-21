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

import com.fathzer.jchess.uci.BackgroundTaskManager.Task;
import com.fathzer.jchess.uci.GoReply.Info;
import com.fathzer.jchess.uci.option.Option;
import com.fathzer.jchess.uci.parameters.GoParameters;
import com.fathzer.jchess.uci.parameters.Parser;

/** A class that implements a subset of the <a href="http://wbec-ridderkerk.nl/html/UCIProtocol.html">UCI protocol</a>.
 * <br>It does not support all UCI commands and contains some extensions. Please have a look at the project's <a href="https://github.com/fathzer-games/jchess-uci/">README</a> file.
 * @see Engine
 */
public class UCI implements Runnable, AutoCloseable {
	/** If the file whose path is in this system property exists, the commands it contains will be executed when the engine is started. */
	public static final String INIT_COMMANDS_PROPERTY_FILE = "uciInitCommands";

	private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));
	private static final String MOVES = "moves";
	private static final String ENGINE_CMD = "engine";
	private static final String GO_CMD = "go";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnn");
	
	/** The current engine. */
	protected Engine engine;

	private final Map<String, Consumer<Deque<String>>> executors = new HashMap<>();
	private final Map<String, Engine> engines = new HashMap<>();

	private final BackgroundTaskManager backTasks = new BackgroundTaskManager();
	private boolean debug = Boolean.getBoolean("logToFile");
	private boolean debugUCI = Boolean.getBoolean("debugUCI");
	private Map<String, Option<?>> options;
	
	private boolean isPositionSet;
	
	/** Creates a new instance.
	 * @param defaultEngine The default engine to use.
	 */
	public UCI(Engine defaultEngine) {
		engines.put(defaultEngine.getId(), defaultEngine);
		this.engine = defaultEngine;
		addCommand(this::doUCI, "uci");
		addCommand(this::doDebug, "debug");
		addCommand(this::doSetOption, "setoption");
		addCommand(this::doIsReady, "isready");
		addCommand(this::doNewGame, "ucinewgame", "ng");
		addCommand(this::doPosition, "position");
		addCommand(this::doGo, GO_CMD);
		addCommand(this::doStop, "stop");
		addCommand(this::doEngine,ENGINE_CMD);
		if (System.console()!=null) {
			log(false, "Input from System.console()");
		} else {
			log(false, "Input from System.in");
		}
	}
	
	/** Adds a new engine.
	 * @param engine The engine to add.
	 * @throws IllegalArgumentException If there's already an engine with the same id.
	 * @see #doEngine(Deque)
	 */
	public void add(Engine engine) {
		final String id = engine.getId();
		if (id==null || id.isBlank()) {
			throw new IllegalArgumentException("Engine can't have a null or blank id");
		}
		if (engines.containsKey(id)) {
			throw new IllegalArgumentException("There's already an engine with id "+id);
		}
		engines.put(id, engine);
	}
	
	/**
	 * Removes an engine.
	 * @param id The id of the engine to remove.
	 * @return The removed engine, or null if no engine with the given id was found.
	 * @throws IllegalStateException If id is the current engine's id.
	 */
	public Engine removeEngine(String id) {
		if (id.equals(engine.getId())) {
			throw new IllegalStateException("Can't remove current engine");
		} 
		return engines.remove(id);
	}
	
	/** Adds a new command.
	 * <br>If command or an alias is already registered, it will be replaced by the provided one.
	 * @param method The method to invoke when the command is received.
	 * @param command The command that will invoke the method.
	 * @param aliases The aliases of the command.
	 * @throws IllegalArgumentException If the method is null or if command or any alias is null or blank.
	 */
	protected void addCommand(Consumer<Deque<String>> method, String command, String... aliases) {
		if (command==null || command.isBlank() || method==null) {
			throw new IllegalArgumentException();
		}
		if (Arrays.stream(aliases).anyMatch(c -> c==null || c.isBlank())) {
			throw new IllegalArgumentException();
		}
		executors.put(command, method);
		Arrays.stream(aliases).forEach(c -> executors.put(c, method));
	}
	
	/** Executes the debug command.
	 * @param tokens The tokens of the command.
	 */
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

	/** Executes the uci command.
	 * @param tokens The tokens of the command.
	 */
	protected void doUCI(Deque<String> tokens) {
		out("id name "+engine.getId());
		final String author = engine.getAuthor();
		if (author!=null) {
			out("id author "+author);
		}
		getOptions().values().forEach( o -> out(o.toUCI()));
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
		final Option<?> option = getOptions().get(name);
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

	/** Executes the setoption command.
	 * @param tokens The tokens of the command.
	 */
	protected void doSetOption(Deque<String> tokens) {
		final String error = processOption(tokens);
		if (error!=null) {
			debug(error);
		}
	}
	
	/** Executes the isready command.
	 * @param tokens The tokens of the command.
	 */
	protected void doIsReady(Deque<String> tokens) {
		out("readyok");
	}

	/** Executes the newgame command.
	 * @param tokens The tokens of the command.
	 */
	protected void doNewGame(Deque<String> tokens) {
		engine.newGame();
		isPositionSet = false;
	}

	/** Executes the position command.
 * @param tokens The tokens of the command.
	 */
	protected void doPosition(Deque<String> tokens) {
		if (tokens.isEmpty()) {
			debug("missing position definition");
			return;
		}
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
		try {
			engine.setStartPosition(fen);
			tokens.stream().dropWhile(t->!MOVES.equals(t)).skip(1).forEach(this::doMove);
			isPositionSet = true;
		} catch (IllegalArgumentException e) {
			debug("invalid position definition");
		}
	}
	
	private void doMove(String move) {
		log("Moving",move);
		try {
			engine.move(UCIMove.from(move));
		} catch (IllegalArgumentException e) {
			debug("invalid move "+move);
		}
	}
	
	private String getFEN(Collection<String> tokens) {
		return tokens.stream().takeWhile(t -> !MOVES.equals(t)).collect(Collectors.joining(" "));
	}
	
	/** Launches a task on the background thread.
	 * @param task The task to launch
	 * @param stopper A runnable that stops the task when invoked (it is user by the <i>stop</i> command in order to stop the task.
	 * @param logger Where to send the exceptions 
	 * @return true if the task is launched, false if another task is already running.
	 */
	protected boolean doBackground(ThrowingRunnable task, Runnable stopper, Consumer<Exception> logger) {
		return backTasks.doBackground(new Task(task, stopper, logger));
	}

	/** Executes the go command.
	 * @param tokens The tokens of the command.
	 */
	protected void doGo(Deque<String> tokens) {
		if (!isPositionSet()) {
			debug("No position defined");
		} else {
			final Optional<GoParameters> goOptions = parse(GoParameters::new, GoParameters.PARSER, tokens);
			if (goOptions.isPresent()) {
				final StoppableTask<GoReply> task = engine.go(goOptions.get());
				final boolean started = doBackground(() -> processGo(task), task::stop, e -> err(GO_CMD, e));
				if (!started) {
					debug("Engine is already working");
				}
			}
		}
	}

	private void processGo(final StoppableTask<GoReply> task) throws Exception {
		final GoReply goReply = task.call();
		final Optional<String> mainInfo = goReply.getMainInfoString();
		if (mainInfo.isPresent()) {
			this.out(mainInfo.get());
			final Optional<Info> info = goReply.getInfo();
			final int nb = info.isPresent() ? info.get().getExtraMoves().size() : 0;
			for (int i = 1; i <= nb; i++) {
				goReply.getInfoString(i).ifPresent(this::out);
			}
		}
		out(goReply.toString());
	}

	/** Parses the parameter tokens of a command.
	 * @param <T> The type of the object that represents the command parameters.
	 * @param builder A supplier that creates a new instance of the object that represents the command parameters.
	 * @param parser The parser that will parse the tokens.
	 * @param tokens The tokens to parse (excluding the command name itself).
	 * @return An optional containing the parsed object or empty if the parsing failed.
	 */
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

	/** Executes the stop command.
	 * @param tokens The tokens of the command.
	 */
	protected void doStop(Deque<String> tokens) {
		if (!backTasks.stop()) {
			debug("Nothing to stop");
		}
	}
	
	/** Executes the engine command.
	 * <br>This command allow to list the available engines (if tokens is empty) or to change the current engine.
	 * @param tokens The tokens of the command.
	 */
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
			if (isPositionSet()) {
				isPositionSet = false;
				debug("position is cleared by engine change");
			}
			this.engine = newEngine;
			this.options =  null;
			out(ENGINE_CMD+" "+engineId+" ok");
		} else {
			debug(ENGINE_CMD+" "+engineId+" is unknown");
		}
	}

	private Map<String, Option<?>> getOptions() {
		if (options==null) {
			this.options = engine.getOptions();
		}
		return options;
	}

	@Override
	public void run() {
		init();
		while (true) {
			log("Waiting for command...");
			final String command=getNextCommand().trim();
			if ("quit".equals(command) || "q".equals(command)) {
		    	log(">",command);
				break;
			}
			if (!command.isEmpty()) {
				doCommand(command);
			}
		}
	}
	
	private void init() {
		final String initFile = System.getProperty(INIT_COMMANDS_PROPERTY_FILE);
		if (initFile!=null) {
			try {
				Files.readAllLines(Paths.get(initFile)).stream().map(String::trim).filter(s -> !s.isEmpty()).forEach(this::doCommand);
			} catch (IOException e) {
				err("init engine", e);
			}
		}

	}

	/** Executes a command.
	 * @param command The command to execute
	 * @return true if the command was found
	 */
	protected boolean doCommand(final String command) {
    	log(">",command);
		final Deque<String> tokens = new LinkedList<>(Arrays.asList(command.split(" ")));
		final Consumer<Deque<String>> executor = executors.get(tokens.pop());
		if (executor==null) {
			debug("unknown command");
			return false;
		} else {
			try {
				executor.accept(tokens);
			} catch (RuntimeException e) {
				err(command, e);
			}
			return true;
		}
	}

	/** Sends an error message on an exception.
	 * <br>The default implementation uses the {@link #err(CharSequence)} method to write the message and the exception stack trace.
	 * <br>One can override this method in order to change this behavior.
	 * @param tag The tag of the command that failed
	 * @param e The exception that occurred
	 */
	protected void err(String tag, Throwable e) {
		err("Error with "+tag+" tag");
		err(e,0);
	}
	
	private void err(Throwable e, int level) {
		err((level>0 ? "caused by":"")+e.toString());
		Arrays.stream(e.getStackTrace()).forEach(f -> err(f.toString()));
		if (e.getCause()!=null) {
			err(e.getCause(),level+1);
		}
	}
	
	/** Sends an error message.
	 * <br>The default implementation write the message the <code>System.err</code>.
	 * <br>One can override this method in order to send error messages to somewhere else.
	 * @param message The message to send.
	 */
	protected void err(CharSequence message) {
		System.err.println(message);
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
	 * @return The next command
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
	 * <br>One can override this method in order to send replies to somewhere other than standard console output.
	 * @param message The reply to send.
	 */
	@SuppressWarnings("java:S106")
	protected void out(CharSequence message) {
    	log(":",message.toString());
		System.out.println(message);
	}
	
	/** Tests whether the debug mode is on.
	 * @return true if debug mode is on.
	 * @see #doDebug(Deque)
	 */
	protected boolean isDebugMode() {
		return debugUCI;
	}
	
	/** Sends a debug message.
	 * <br>The default implementation calls {@link #log(String...)} then, if debug is on, outputs an <i>info string</i> message.
	 * <br>One can override this method in order to send debug messages to somewhere else than standard uci output.
	 * @param message The message to send.
	 * @see #isDebugMode()
	 * @see #out(CharSequence)
	 */
	@SuppressWarnings("java:S106")
	protected void debug(CharSequence message) {
    	log(":","info","UCI debug is", Boolean.toString(debugUCI),message.toString());
		if (debugUCI) {
			out("info string "+message);
		}
	}
	
	/** Tests whether a position is set.
	 * @return true if a position is set
	 */
	protected boolean isPositionSet() {
		return isPositionSet;
	}

	@Override
	public void close() {
		backTasks.close();
	}
}
