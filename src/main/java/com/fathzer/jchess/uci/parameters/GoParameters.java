package com.fathzer.jchess.uci.parameters;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fathzer.jchess.uci.UCIMove;

/** The arguments of the <i>go</i> UCI command.
 */
public class GoParameters {
	private static final ParamProperties<GoParameters> WTIME_PARAM = new ParamProperties<>((p,tok) -> p.time.whiteClock.remainingMs=Parser.positiveInt(tok), "wtime");
	private static final ParamProperties<GoParameters> WHITE_TIME_INC_PARAM = new ParamProperties<>((p,tok) -> p.time.whiteClock.incrementMs=Parser.positiveInt(tok), "winc");
	private static final ParamProperties<GoParameters> BTIME_PARAM = new ParamProperties<>((p,tok) -> p.time.blackClock.remainingMs=Parser.positiveInt(tok), "btime");
	private static final ParamProperties<GoParameters> BLACK_TIME_INC_PARAM = new ParamProperties<>((p,tok) -> p.time.blackClock.incrementMs=Parser.positiveInt(tok), "binc");
	private static final ParamProperties<GoParameters> MOVES_TO_GO_PARAM = new ParamProperties<>((p,tok) -> p.time.movesToGo=Parser.positiveInt(tok), "movestogo");
	private static final ParamProperties<GoParameters> MOVE_TIME_PARAM = new ParamProperties<>((p,tok) -> p.time.moveTimeMs=Parser.positiveInt(tok), "movetime");
	private static final ParamProperties<GoParameters> INFINITE_PARAM = new ParamProperties<>((p,tok) -> p.time.infinite=true, "infinite");

	private static final ParamProperties<GoParameters> DEPTH_PARAM = new ParamProperties<>((p,tok) -> p.depth=Parser.positiveInt(tok), "depth");
	private static final ParamProperties<GoParameters> NODES_PARAM = new ParamProperties<>((p,tok) -> p.nodes=Parser.positiveInt(tok), "nodes");
	private static final ParamProperties<GoParameters> MATE_PARAM = new ParamProperties<>((p,tok) -> p.mate=Parser.positiveInt(tok), "mate");
	private static final ParamProperties<GoParameters> PONDER_PARAM = new ParamProperties<>((p,tok) -> p.ponder=true, "ponder");
	private static final ParamProperties<GoParameters> SEARCH_MOVES_PARAM = new ParamProperties<>((p,tok) -> {
		while (!tok.isEmpty()) {
			p.moveToSearch.add(UCIMove.from(tok.pop()));
		}
	}, "searchmoves");

	public static final Parser<GoParameters> PARSER = new Parser<>(Arrays.asList(WTIME_PARAM, WHITE_TIME_INC_PARAM, BTIME_PARAM, BLACK_TIME_INC_PARAM,
			MOVES_TO_GO_PARAM, MOVE_TIME_PARAM, INFINITE_PARAM, DEPTH_PARAM, NODES_PARAM, MATE_PARAM, PONDER_PARAM, SEARCH_MOVES_PARAM));

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

	private TimeOptions time = new TimeOptions();
	private boolean ponder;
	private int depth = 0;
	private int nodes = 0;
	private int mate = 0;
	private List<UCIMove> moveToSearch = new LinkedList<>();

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
