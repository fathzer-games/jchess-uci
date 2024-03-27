package com.fathzer.jchess.uci;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** The reply to a go request.
 */
public class GoReply {
	/** A score.*/
	public sealed interface Score {
		/** Gets the UCI representation of a score.
		 * @return a String
		 */
		String toUCI();
	}
	/** An exact score expressed in centipawns.
	 * @param cp The number of centipawns
	 */
	public final record CpScore (int cp) implements Score {
		@Override
		public String toUCI() {
			return "cp "+cp;
		}
	}
	/** A lower bound score expressed in centipawns.
	 * @param cp The number of centipawns
	 */
	public final record LowerScore (int cp) implements Score {
		@Override
		public String toUCI() {
			return "lowerbound "+cp;
		}
	}
	/** An upper bound score expressed in centipawns.
	 * @param cp The number of centipawns
	 */
	public final record UpperScore (int cp) implements Score {
		@Override
		public String toUCI() {
			return "upperbound "+cp;
		}
	}
	/** A mate score.
	 * @param moveNumber The number of moves (not plies) before mate. A negative number if engine is mated.
	 */
	public final record MateScore (int moveNumber) implements Score {
		@Override
		public String toUCI() {
			return "mate "+moveNumber;
		}
	}

	/** The information attached to the reply (the information returned in info lines).
	 */
	public static class Info {
		private final int depth;
		private List<UCIMove> extraMoves;
		private Function<UCIMove, Optional<List<UCIMove>>> pvBuilder;
		private Function<UCIMove, Optional<Score>> scoreBuilder;
		
		/** Constructor.
		 * @param depth The search depth.
		 */
		public Info(int depth) {
			this.depth = depth;
			this.extraMoves = Collections.emptyList();
			this.pvBuilder = m -> Optional.empty();
			this.scoreBuilder = m -> Optional.empty();
		}

		public int getDepth() {
			return depth;
		}

		public List<UCIMove> getExtraMoves() {
			return extraMoves;
		}

		public void setExtraMoves(List<UCIMove> extraMoves) {
			this.extraMoves = extraMoves;
		}

		public Function<UCIMove, Optional<List<UCIMove>>> getPvBuilder() {
			return pvBuilder;
		}

		public void setPvBuilder(Function<UCIMove, Optional<List<UCIMove>>> pvBuilder) {
			this.pvBuilder = pvBuilder;
		}

		public Function<UCIMove, Optional<Score>> getScoreBuilder() {
			return scoreBuilder;
		}

		public void setScoreBuilder(Function<UCIMove, Optional<Score>> scoreBuilder) {
			this.scoreBuilder = scoreBuilder;
		}
	}
	
	private final UCIMove bestMove;
	private final UCIMove ponderMove;
	private Info info;
	
	public GoReply(UCIMove move) {
		this(move, null);
	}
	
	public GoReply(UCIMove move, UCIMove ponderMove) {
		this.bestMove = move;
		this.ponderMove = ponderMove;
	}
	
	public Optional<UCIMove> getMove() {
		return Optional.ofNullable(bestMove);
	}
	
	public Optional<UCIMove> getPonderMove() {
		return Optional.ofNullable(ponderMove);
	}
	
	public Optional<Info> getInfo() {
		return Optional.ofNullable(info);
	}
	
	@Override
	/** Gets the uci representation of the reply.
	 * @return a String
	 * @see #getMainInfoString()
	 */
	public String toString() {
		return "bestmove "+(bestMove==null?"(none)":bestMove)+(ponderMove==null?"":(" "+ponderMove));
	}
	
	/** Gets the uci info line to return just before sending the reply.
	 * @return The line or an empty optional if no information is available
	 */
	public Optional<String> getMainInfoString() {
		if (info==null) {
			return Optional.empty();
		}
		//TODO
		return Optional.of("depth "+info.depth);
	}
}
