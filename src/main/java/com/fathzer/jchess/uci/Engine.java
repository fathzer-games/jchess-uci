package com.fathzer.jchess.uci;

import static com.fathzer.jchess.uci.UsualOptions.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.Map;

import com.fathzer.jchess.uci.option.Option;
import com.fathzer.jchess.uci.parameters.GoParameters;

/** An engine able to respond to UCI protocol.
 */
public interface Engine {
	/** Annotation to mark an engine as supporting <a href="https://en.wikipedia.org/wiki/Fischer_random_chess">Chess960</a>. 
	 * @see #isChess960Supported() 
	 */
	@Target( {TYPE, TYPE_USE} )
	@Retention(RUNTIME)
	@Inherited
	public @interface Chess960Supported {}

	/** Annotation to declare the engine's id.
	 * @see #getId()
	 */
	@Target( {TYPE, TYPE_USE} )
	@Retention(RUNTIME)
	@Inherited
	public @interface Id {
		/** Gets the engine's id.
		 * @return a String
		 */
		String value();
	}
	/** Annotation to declare the engine's author.
	 * @see #getAuthor()
	 */
	@Target( {TYPE, TYPE_USE} )
	@Retention(RUNTIME)
	@Inherited
	public @interface Author {
		/** Gets the engine's author.
		 * @return a String
		 */
		String value();
	}

	/** Gets the engine's id, the one returned when a uci command is received.
	 * <br>The default implementation returns the value of the {@link Id} annotation and throws an exception if the annotation is missing.
	 * @return a non null String
	 * @throws IllegalStateException if the {@link Id} annotation is missing on the class.
	 */
	default String getId() {
		final Id annotation = getAnnotation(getClass(), Id.class);
		if (annotation==null) {
			throw new IllegalStateException(Id.class+ "annotation is missing on class "+getClass());
		}
		return annotation.value();
	}
	
	private static <T extends Annotation> T getAnnotation(Class<?> target, Class<T> annotationClass) {
		final AnnotatedType type = target.getAnnotatedSuperclass();
		final T annotation = type.getAnnotation(annotationClass);
		return annotation!=null ? annotation : target.getAnnotation(annotationClass);
	}

	/** Gets the engine's author, the one returned when a uci command is received.
	 * <br>The default implementation returns the value of the {@link Author} annotation.
	 * @return a String. Null if author is unknown (this is the default implementation).
	 */
	default String getAuthor() {
		final Author annotation = getAnnotation(getClass(), Author.class);
		return annotation==null ? null : annotation.value();
	}

	/** Clears all data from previous game.
	 * <br>The default implementation does nothing
	 */
	default void newGame() {
		// Does nothing by default, assuming the engine doesn't cache anything.
	}
	
	/** Gets the default hash table size in MBytes.
	 * <br>If this method returns a positive number, the <i>Hash</i> standard option is automatically added to the options list.
	 * <br>In such a case, {@link #setHashTableSize(int)} and {@link #clearHashTable()} may be called, so you should override them in order to not have the program hang.
	 * @return a positive number (the default hash table size in MBytes) if this engine supports hash table. A negative number if it does
	 * not support hash table. 
	 * <br>The default implementation returns -1;
	 * @see Engine#setHashTableSize(int)
	 */
	default int getDefaultHashTableSize() {
		return -1;
	}
	/** Sets the hash table size.
	 * <br>The default implementation throws an exception.
	 * <br>You should override this method if {@link #getDefaultHashTableSize()} is.
	 * @param sizeInMB The size of the hash table in MBytes 
	 * @Throws UnsupportedOperationException if {@link #getDefaultHashTableSize()} returns a negative number.
	 * @Throws IllegalStateException if {@link #getDefaultHashTableSize()} returns a number &gt;=0.
	 */
	default void setHashTableSize(int sizeInMB) {
		throw getDefaultHashTableSize()<0 ? new UnsupportedOperationException() : new IllegalStateException();
	}
	
	/** Clears the hash table.
	 * <br>The default implementation throws an exception.
	 * <br>You should override this method if you override {@link #getDefaultHashTableSize()} is.
	 * @Throws UnsupportedOperationException if {@link #getDefaultHashTableSize()} returns a negative number.
	 * @Throws IllegalStateException if {@link #getDefaultHashTableSize()} returns a number &gt;=0.
	 */
	default void clearHashTable() {
		throw getDefaultHashTableSize()<0 ? new UnsupportedOperationException() : new IllegalStateException();
	}

	/** Checks whether this engine supports <a href="https://en.wikipedia.org/wiki/Fischer_random_chess">Chess960</a>.
	 * <br>If this method returns true, the <i>UCI_Chess960</i> standard option is automatically added to the options list.
	 * @return true if chess960 is supported. The default implementation returns true if the {@link Chess960Supported} annotation is present.
	 * @see #setChess960(boolean)
	 */
	default boolean isChess960Supported() {
		return getAnnotation(getClass(), Chess960Supported.class) != null;
	}
	/** Switches the <a href="https://en.wikipedia.org/wiki/Fischer_random_chess">Chess960</a> mode.
	 * <br>The default implementation throws an exception 
	 * @param chess960Mode true to start playing with chess 960 rules.
	 * @Throws UnsupportedOperationException if chess 960 is not supported.
	 * @Throws IllegalStateException if chess 960 is supported.
	 */
	default void setChess960(boolean chess960Mode) {
		throw isChess960Supported() ? new IllegalStateException() : new UnsupportedOperationException();
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
	 * <br>The default implementation throws an exception 
	 * @param activate true to activate the opening book. False to deactivate it.
	 * @Throws UnsupportedOperationException if engine has not its own book.
	 * @Throws IllegalStateException if engine has its own book.
	 */
	default void setOwnBook(boolean activate) {
		throw hasOwnBook() ? new IllegalStateException() : new UnsupportedOperationException();
	}
	
	/** Gets the options supported by the engine.
	 * <br>This method is called once during the engine instantiation.
	 * <br>The default implementation returns the standard options according to {@link #isChess960Supported()}, {@link #getDefaultHashTableSize()} and {@link #hasOwnBook()}
	 * @return An option name to option map.
	 */
	default Map<String, Option<?>> getOptions() {
		final Map<String, Option<?>> options = new HashMap<>();
		if (isChess960Supported()) {
			options.put(CHESS960_NAME, chess960(this::setChess960));
		}
		if (hasOwnBook()) {
			options.put(OWN_BOOK_NAME, ownBook(this::setOwnBook, true));
		}
		if (getDefaultHashTableSize()>=0) {
			options.put(HASH_NAME, hash(this::setHashTableSize, this.getDefaultHashTableSize(), Math.min(getDefaultHashTableSize(), 1), 4096*1024));
			options.put(CLEAR_HASH_NAME, clearHash(this::clearHashTable));
		}
		return options;
	}

	/** Sets the start position.
	 * @param fen The start position in the fen format.
	 * @throws IllegalArgumentException if fen is illegal.
	 */
	void setStartPosition(String fen);
	/** Moves a piece on the chess board.
	 * @param move The move to apply.
	 * @throws IllegalArgumentException if move is illegal.
	 */
	void move(UCIMove move);
	
	/** Start searching for the best move.
	 * <br>Please note that:<ul>
 	 * <li>The returned task is considered as a 'long running method' and its supplier will be called on a different thread than methods of this class.</li>
	 * <li>The supplier should be cooperative with the stopper; It should end as quickly as possible when stopper is invoked and <b>always</b> return a move.</li>
	 * </ul>
	 * @param params The go parameters.
	 * @return A task able to compute the engine's move.
	 */
	StoppableTask<GoReply> go(GoParameters params);
}
