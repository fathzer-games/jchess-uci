package com.fathzer.jchess.uci;

/** A class able to build instances of UCI specific move generator.
 * @param <M> The class of the moves returned by the move generator.
 */
public interface UCIMoveGeneratorProvider<M> {
	/** Gets a new move generator initialized on the current position.
	 */
	UCIMoveGenerator<M> getMoveGenerator();
}
