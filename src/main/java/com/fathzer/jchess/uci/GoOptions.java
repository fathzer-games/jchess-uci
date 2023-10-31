package com.fathzer.jchess.uci;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** The options of the go UCI command.
 */
public class GoOptions {
	enum Option {
		WHITE_TIME("wtime",Option::wtime), WHITE_TIME_INC("winc",Option::winc), BLACK_TIME("btime",Option::btime), BLACK_TIME_INC("binc",Option::binc), MOVES_TO_GO("movestogo",Option::movestogo), MOVE_TIME("movetime", Option::movetime), INFINITE("infinite", Option::infinite),
		DEPTH("depth", Option::depth), NODES("nodes", Option::nodes), MATE("mate", Option::mate), PONDER("ponder", Option::ponder),
		SEARCH_MOVES("searchmoves", Option::searchmoves);

		private String name;
		private BiConsumer<GoOptions, Deque<String>> parser;
		private Option(String name, BiConsumer<GoOptions, Deque<String>> parser) {
			this.name = name;
			this.parser = parser;
		}
		
		private static void wtime(GoOptions options, Deque<String> tokens) {
			options.time.whiteClock.remainingMs = positiveInt(tokens.pop());
		}

		private static void winc(GoOptions options, Deque<String> tokens) {
			options.time.whiteClock.incrementMs = positiveInt(tokens.pop());
		}

		private static void btime(GoOptions options, Deque<String> tokens) {
			options.time.blackClock.remainingMs = positiveInt(tokens.pop());
		}

		private static void binc(GoOptions options, Deque<String> tokens) {
			options.time.blackClock.incrementMs = positiveInt(tokens.pop());
		}

		private static void movestogo(GoOptions options, Deque<String> tokens) {
			options.time.movesToGo = positiveInt(tokens.pop());
		}

		private static void movetime(GoOptions options, Deque<String> tokens) {
			options.time.moveTimeMs = positiveInt(tokens.pop());
		}

		private static void infinite(GoOptions options, Deque<String> tokens) {
			options.time.infinite = true;
		}

		private static void depth(GoOptions options, Deque<String> tokens) {
			options.depth = positiveInt(tokens.pop());
		}

		private static void nodes(GoOptions options, Deque<String> tokens) {
			options.nodes = positiveInt(tokens.pop());
		}

		private static void mate(GoOptions options, Deque<String> tokens) {
			options.mate = positiveInt(tokens.pop());
		}

		private static void ponder(GoOptions options, Deque<String> tokens) {
			options.ponder = true;
		}
		
		private static int positiveInt(String value) {
			final int result = Integer.parseInt(value);
			if (result<0) {
				throw new IllegalArgumentException("Unexpected negative number "+value);
			}
			return result;
		}

		private static void searchmoves(GoOptions options, Deque<String> tokens) {
			while (!tokens.isEmpty()) {
				final String token = tokens.peek();
				if (PARSER_MAP.containsKey(token)) {
					break;
				} else {
					tokens.removeFirst();
					options.moveToSearch.add(UCIMove.from(token));
				}
			}
		}
	}

	private static final Map<String, BiConsumer<GoOptions, Deque<String>>> PARSER_MAP = new HashMap<>();
	static {
		for (Option option : Option.values()) {
			PARSER_MAP.put(option.name, option.parser);
		}
	}

	public static class PlayerClockData {
		private int remainingMs;
		private int incrementMs;

		public int getRemainingMs() {
			return remainingMs;
		}

		public int getIncrementMs() {
			return incrementMs;
		}
	}
	
	public static class TimeOptions {
		private int movesToGo;
		private int moveTimeMs;
		private PlayerClockData whiteClock = new PlayerClockData();
		private PlayerClockData blackClock = new PlayerClockData();
		private boolean infinite; 

		public int getMovesToGo() {
			return movesToGo;
		}

		public int getMoveTimeMs() {
			return moveTimeMs;
		}

		public PlayerClockData getWhiteClock() {
			return whiteClock;
		}

		public PlayerClockData getBlackClock() {
			return blackClock;
		}

		public boolean isInfinite() {
			return infinite;
		}
	}

	private List<String> ignoredOptions = new LinkedList<>();
	private TimeOptions time = new TimeOptions();
	private boolean ponder;
	private int depth = 0;
	private int nodes = 0;
	private int mate = 0;
	private List<UCIMove> moveToSearch = new LinkedList<>();

	/** Constructor.
	 * @param tokens the go command options as tokens (for example: wtime, 297999, btime, 300000, winc, 3000, binc, 3000)
	 * @throws IllegalArgumentException if a token is illegal (for instance, we expected a number, but we got a string).
	 * <br>Please note that unknown options are returned in {@link #getIgnoredOptions()}
	 */
	public GoOptions(List<String> tokenList) {
		Deque<String> tokens = new LinkedList<>(tokenList);
		while (!tokens.isEmpty()) {
			final String token = tokens.pop();
			final BiConsumer<GoOptions, Deque<String>> parser = PARSER_MAP.get(token);
			if (parser==null) {
				ignoredOptions.add(token);
			} else {
				parser.accept(this, tokens);
			}
		}
	}

	public List<String> getIgnoredOptions() {
		return ignoredOptions;
	}

	public TimeOptions getTimeOptions() {
		return time;
	}

	/** Gets the <i>depth</i> option.
	 * @return 0 if the option is not set
	 */
	public int getDepth() {
		return depth;
	}

	/** Gets the <i>nodes</i> option.
	 * @return 0 if the option is not set
	 */
	public int getNodes() {
		return nodes;
	}

	/** Gets the <i>mate</i> option.
	 * @return 0 if the option is not set
	 */
	public int getMate() {
		return mate;
	}

	public boolean isPonder() {
		return ponder;
	}

	public List<UCIMove> getMoveToSearch() {
		return moveToSearch;
	}
}
