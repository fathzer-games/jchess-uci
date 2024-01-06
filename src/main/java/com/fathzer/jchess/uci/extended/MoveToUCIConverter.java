package com.fathzer.jchess.uci.extended;

import com.fathzer.jchess.uci.UCIMove;

/** A class that can converts an internal move into a standard UCI move.
 * @param <M> The type of internal moves
 */
public interface MoveToUCIConverter<M> {
	/** Converts an internal move to its UCI representation.
	 * @param move The move to convert
	 * @return The move in its UCI format
	 */
	UCIMove toUCI(M move);
}
