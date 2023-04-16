package com.fathzer.jchess.uci;

import com.fathzer.jchess.uci.option.Option;

public interface Engine {
	/** Gets the engine's id, the one returned when a uci command is received.
	 * @return a non null String
	 */
	String getId();
	/** Gets the engine's author, the one returned when a uci command is received.
	 * @return a String. Null if author is unknown (this is the default implementation).
	 */
	default String getAuthor() {
		return null;
	}
	/** Clears all data from previous game.
	 * <br>The default implementation does nothing
	 */
	default void newGame() {
		// Does nothing by default, assuming the engine do not have cached things
	}
	/** Gets the options supported by the engine.
	 * <br>The default implementation returns an empty array.
	 * @return An option array.
	 */
	default Option<?>[] getOptions() {
		// By default engine has no option
		return new Option[0];
	}
	/** Sets the start position.
	 * @param fen The start position in the fen format.
	 */
	void setFEN(String fen);
	/** Moves a piece on the chess board.
	 * @param move The move to apply.
	 */
	void move(UCIMove move);
	/** Start searching for the best move.
	 * <br>Please note that:<ul>
 	 * <li>The returned task is considered as a 'long running method' and its supplier will be called on a different thread than methods of this class.</li>
	 * <li>The supplier should be cooperative with the stopper; It should end as quickly as possible when stopper is invoked and <b>always</b> return a move.</li>
	 * @return A long running task able to compute the engine's move.
	 */
	LongRunningTask<UCIMove> go();
	
	/** Returns a string representation of the board.
	 * <br>The representation is totally free. The default implementation returns the fen representation.
	 * @return A string representing the chess board.
	 */
	default String getBoardAsString() {
		final String fen = getFEN();
		return fen == null ? "no position defined" : fen;
	}
	/** Returns the <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">FEN</a> representation of the board.
	 * @return a string representing the chess board. Null if no position were defined
	 */
	String getFEN();
}
