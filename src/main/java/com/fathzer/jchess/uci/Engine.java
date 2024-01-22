package com.fathzer.jchess.uci;

import java.util.Collections;
import java.util.List;

import com.fathzer.jchess.uci.option.Option;
import com.fathzer.jchess.uci.parameters.GoParameters;

/** An engine able to respond to UCI protocol.
 */
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
		// Does nothing by default, assuming the engine doesn't cache anything.
	}
	/** Gets the default hash table size in MBytes.
	 * <br>If this method returns a positive number, the <i>Hash</i> standard option is automatically added to the options list.
	 * <br>In such a case, {@link #setHashTableSize(int)} may be called, so you should override it in order to not have the program hang at startup.
	 * @return a positive number (the default hash table size in MBytes) if this engine supports hash table. A negative number if it does
	 * not support hash table. 
	 * <br>The default implementation returns -1;
	 * @see Engine#setHashTableSize(int)
	 */
	default int getDefaultHashTableSize() {
		return -1;
	}
	/** Sets the hash table size.
	 * <br>The default implementation throws an UnsupportedOperationException.
	 * <br>You should override this method if {@link #getDefaultHashTableSize()}
	 * @param sizeInMB The size of the hash table in MBytes 
	 */
	default void setHashTableSize(int sizeInMB) {
		throw new UnsupportedOperationException();
	}

	/** Checks whether this engine supports <a href="https://en.wikipedia.org/wiki/Fischer_random_chess">Chess960</a>.
	 * <br>If this method returns true, the <i>UCI_Chess960</i> standard option is automatically added to the options list.
	 * @return true if chess960 is supported, false (the default) if not.
	 */
	default boolean isChess960Supported() {
		return false;
	}
	/** Switches the <a href="https://en.wikipedia.org/wiki/Fischer_random_chess">Chess960</a> mode.
	 * <br>The default implementation does nothing 
	 * @param chess960Mode true to start playing with chess 960 rules.
	 */
	default void setChess960(boolean chess960Mode) {
		// Does nothing by default
	}
	
	/** Checks whether this engine has its own opening book.
	 * <br>If this method returns true, the <i>OwnBook</i> standard option is automatically added to the options list.
	 * <br>In such a case, {@link #setOwnBook(boolean)} may be called, so you should override it in order to not have the program hang at startup.
	 * @return true if the engine hasItsOwn book, false (the default) if not.
	 * @see Engine#setOwnBook(boolean)
	 */
	default boolean hasOwnBook() {
		return false;
	}
	/** Ask the engine to use its own opening book or not. 
	 * @param activate true to activate the opening book. False to deactivate it.
	 * <br>The default implementation throws an UnsupportedOperationException.
	 */
	default void setOwnBook(boolean activate) {
		throw new UnsupportedOperationException();
	}
	
	/** Gets the options supported by the engine.
	 * <br>This method is called once during the engine instantiation.
	 * <br>The default implementation returns an empty array.
	 * @return An option list.
	 * <br>Please note that there's no need to send the UCI_Chess960 if chess960 is supported. This option will be automatically added if {@link #isChess960Supported()} returns true.
	 */
	default List<Option<?>> getOptions() {
		// By default engine has no option
		return Collections.emptyList();
	}

	/** Sets the start position.
	 * @param fen The start position in the fen format.
	 */
	void setStartPosition(String fen);
	/** Moves a piece on the chess board.
	 * @param move The move to apply.
	 */
	void move(UCIMove move);
	/** Start searching for the best move.
	 * <br>Please note that:<ul>
 	 * <li>The returned task is considered as a 'long running method' and its supplier will be called on a different thread than methods of this class.</li>
	 * <li>The supplier should be cooperative with the stopper; It should end as quickly as possible when stopper is invoked and <b>always</b> return a move.</li>
	 * </ul>
	 * @param params The go parameters.
	 * @return A long running task able to compute the engine's move.
	 */
	LongRunningTask<BestMoveReply> go(GoParameters params);
	
	/** Tests whether a position is set.
	 * @return true if a position is set
	 */
	boolean isPositionSet();
}
