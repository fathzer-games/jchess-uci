package com.fathzer.jchess.uci;

/** A class able to build instances of UCI specific move generator.
 * @param <M> The class of the moves returned by the move generator.
 */
public interface UCIMoveGeneratorProvider<M> {
	/** Sets the start position.
	 * @param fen The start position in the fen format.
	 */
	void setFEN(String fen);

	/** Gets a new move generator initialized on the current position.
	 */
	UCIMoveGenerator<M> getMoveGenerator();
}
