package com.fathzer.jchess.uci.extended;

import com.fathzer.games.MoveGenerator;

/** An interface able to return instances of move generator.
 * <br>The instance supplied is initialized to the current engine position.
 * @param <M> The class of the moves returned by the move generator.
 */
@FunctionalInterface
public interface MoveGeneratorSupplier<M> {
	/** Gets the move generator. */
	MoveGenerator<M> getMoveGenerator();
}
