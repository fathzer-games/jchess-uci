package com.fathzer.jchess.uci;

import com.fathzer.games.MoveGenerator;

/** A move generator that can translate its inner generated moves to UCI format.
 * @param <M> The inner move representation
 */
public interface UCIMoveGenerator<M> extends MoveGenerator<M> {
	/** Converts a move to UCI format.
	 * @param move The move
	 * @return The uci representation of the move 
	 */
	UCIMove toUCI(M move);
}
