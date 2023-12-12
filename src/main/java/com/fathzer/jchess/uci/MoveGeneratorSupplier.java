package com.fathzer.jchess.uci;

import java.util.function.Supplier;

import com.fathzer.games.MoveGenerator;

/** An interface of engines able to build instances of move generator.
 * <br>The instance supplied is initialized to the current engine position.
 * @param <M> The class of the moves returned by the move generator.
 */
public interface MoveGeneratorSupplier<M> extends Supplier<MoveGenerator<M>> {
}
