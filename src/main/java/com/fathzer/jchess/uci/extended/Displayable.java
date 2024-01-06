package com.fathzer.jchess.uci.extended;

/** A chess board that can be displayed as FEN or in a custom format.
 */
public interface Displayable {
	
	/** Returns a string representation of the board.
	 * <br>The representation is totally free. The default implementation returns the fen representation.
	 * @return A string representing the chess board.
	 */
	default String getBoardAsString() {
		return getFEN();
	}
	
	/** Returns the <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">FEN</a> representation of the board.
	 * @return a string representing the chess board.
	 */
	String getFEN();
}
