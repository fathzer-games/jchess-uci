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
		private Function<UCIMove, List<UCIMove>> pvBuilder;
		private Function<UCIMove, Optional<Score>> scoreBuilder;
		private int hashFull;
		
		/** Constructor.
		 * @param depth The search depth.
		 */
		public Info(int depth) {
			this.depth = depth;
			this.extraMoves = Collections.emptyList();
			this.pvBuilder = Collections::singletonList;
			this.scoreBuilder = m -> Optional.empty();
			this.hashFull = -1;
		}
		/** Gets the search depth.
		 * @return The search depth.
		 */
		public int getDepth() {
			return depth;
		}

		/** Gets the transposition table occupancy in per mill.
		 * @return An integer. -1 if the occupancy is unknown.
		 */
		public int getHashFull() {
			return hashFull;
		}
		/** Sets the transposition table occupancy in per mill.
		 * @param hashFull An integer. -1 if the occupancy is unknown
		 */
		public void setHashFull(int hashFull) {
			this.hashFull = hashFull;
		}
		/** Gets the extra moves.
		 * @return A list of moves. An empty list if no extra moves have been set.
		 */
		public List<UCIMove> getExtraMoves() {
			return extraMoves;
		}

		/** Sets the extra moves.
		 * <br>This method allows to return additional 'best' moves when <i>MultiPV</i> is set and is not 1
		 * @param extraMoves A list of moves.
		 */
		public void setExtraMoves(List<UCIMove> extraMoves) {
			this.extraMoves = extraMoves;
		}

		/** Sets a function to build the principal variation of best and extra moves.
		 * @param pvBuilder A function that returns the principal variation (an empty list if variation is unavailable).
		 */
		public void setPvBuilder(Function<UCIMove, List<UCIMove>> pvBuilder) {
			this.pvBuilder = pvBuilder;
		}
		/** Sets a function to build the score of best and extra moves.
		 * @param scoreBuilder A function that returns the score or an empty optional if no score is available.
		 */
		public void setScoreBuilder(Function<UCIMove, Optional<Score>> scoreBuilder) {
			this.scoreBuilder = scoreBuilder;
		}
	}
	
	private final UCIMove bestMove;
	private final UCIMove ponderMove;
	private Info info;
	
	/** Constructor.
	 * @param move The best move.
	 */
	public GoReply(UCIMove move) {
		this(move, null);
	}
	
	/** Constructor.
	 * @param move The best move.
	 * @param ponderMove The ponder move (null if pondering is not activated).
	 */
	public GoReply(UCIMove move, UCIMove ponderMove) {
		this.bestMove = move;
		this.ponderMove = ponderMove;
	}
	
	/** Sets the information attached to the reply.
	 * @param info The information attached to the reply.
	 */
	public void setInfo(Info info) {
		this.info = info;
	}
	/** Gets the best move.
	 * @return An optional containing the best move or an empty optional if no move is available (typically if the engine is checked mate).
	 */
	public Optional<UCIMove> getMove() {
		return Optional.ofNullable(bestMove);
	}
	/** Gets the ponder move.
	 * @return An optional containing the ponder move or an empty optional if no ponder move is available.
	 */
	public Optional<UCIMove> getPonderMove() {
		return Optional.ofNullable(ponderMove);
	}
	
	/** Gets the information attached to the reply.
	 * @return An optional containing the information or an empty optional if no information is available.
	 */
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
		return bestMove==null || info==null ? Optional.empty() : getInfoString(0);
	}

	/** Gets a uci info line to return before sending the reply.
	 * @param index The move index (0 for the best move or the index or the extra moves passed to {@code Info#setExtraMoves(List)} +1
	 * @return The line or an empty optional if no information is available
	 * @throws IllegalArgumentException if the index is out of bounds
	 */
	public Optional<String> getInfoString(int index) {
		if (index<0 || info==null || index>info.extraMoves.size()) {
			throw new IllegalArgumentException();
		}
		final StringBuilder builder = new StringBuilder();
		if (info.depth>0) {
			builder.append("depth ").append(info.depth);
		}
		final UCIMove move = index==0 ? bestMove : info.extraMoves.get(index-1);
		final Optional<Score> score = info.scoreBuilder.apply(move);
		if (score.isPresent()) {
			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append("score ").append(score.get().toUCI());
		}
		if (info.hashFull>0) {
			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append("hashfull ").append(info.hashFull);
		}
		List<UCIMove> pv = info.pvBuilder.apply(move);
		if (pv.isEmpty()) {
			pv = Collections.singletonList(move);
		}
		if (!builder.isEmpty()) {
			builder.append(' ');
		}
		final String moves = String.join(" ", pv.stream().map(UCIMove::toString).toList());
		builder.append("multipv ").append(index+1).append(" pv ").append(moves);
		return builder.isEmpty() ? Optional.empty() : Optional.of("info "+builder);
	}
}
