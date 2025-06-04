package com.fathzer.jchess.uci;

import java.util.function.Consumer;

import com.fathzer.jchess.uci.option.ButtonOption;
import com.fathzer.jchess.uci.option.CheckOption;
import com.fathzer.jchess.uci.option.IntegerSpinOption;
import com.fathzer.jchess.uci.option.SpinOption;

/** Provides some usual options found in uci protocol definition or in Stockfish.
 */
public final class UsualOptions {
	/** The name of the standard option to clear the transposition table. */
	public static final String CLEAR_HASH_NAME = "Clear Hash";
	/** The name of the standard option to set the size of the transposition table. */
	public static final String HASH_NAME = "Hash";
	/** The name of the standard option to set the number of best moves to search. */
	public static final String MULTI_PV_NAME = "MultiPV";
	/** The name of the standard option to ask the engine to use its own opening book. */
	public static final String OWN_BOOK_NAME = "OwnBook";
	/** The name of the standard option to ask the engine to ponder. */
	public static final String PONDER_NAME = "Ponder";
	/** The name of the standard option to accept Chess 960 games. */
	public static final String CHESS960_NAME = "UCI_Chess960";
	/** The name of the standard option to set the engine Elo. */
	public static final String ELO_NAME = "UCI_Elo";
	/** The name of the standard option to limit the engine strength. */
	public static final String LIMIT_STRENGTH_NAME = "UCI_LimitStrength";

	/** The name of the Stockfish's option to set the engine skill level. */
	public static final String LEVEL_NAME = "Skill Level";
	/** The name of the Stockfish's option to set the number of CPU threads used for searching a position. */
	public static final String THREADS_NAME = "Threads";
	
	private UsualOptions() {
		super();
	}
	
	/** Gets the standard option to accept Chess 960 games. 
	 * @param trigger The consumer to call when value is changed.
	 * @return The standard {@value UsualOptions#CHESS960_NAME} option.
	 */
	public static CheckOption chess960(Consumer<Boolean> trigger) {
		return new CheckOption(CHESS960_NAME, trigger, false);
	}
	
	/** Gets the standard option to set the size of the transposition table.
	 * @param trigger The consumer to call when value is changed.
	 * @param defaultSize The default size of the transposition table.
	 * @param minSize The minimum size of the transposition table.
	 * @param maxSize The maximum size of the transposition table.
	 * @return The standard {@value UsualOptions#HASH_NAME} option.
	 */
	public static SpinOption<Integer> hash(Consumer<Integer> trigger, int defaultSize, int minSize, int maxSize) {
		return new IntegerSpinOption(HASH_NAME, trigger, defaultSize, minSize, maxSize);
	}
	
	/** Gets the standard option to clear the transposition table.
	 * @param trigger The consumer to call when value is changed.
	 * @return The standard {@value UsualOptions#CLEAR_HASH_NAME} option.
	*/
	public static ButtonOption clearHash(Runnable trigger) {
		return new ButtonOption(CLEAR_HASH_NAME, trigger);
	}
	
	/** Gets the standard option to set the engine Elo.
	 * @param trigger The consumer to call when value is changed.
	 * @param defaultElo The default value of the option.
	 * @param minElo The minimum value of the option.
	 * @param maxElo The maximum value of the option.
	 * @return The standard {@value UsualOptions#ELO_NAME} option.
	*/
	public static SpinOption<Integer> elo(Consumer<Integer> trigger, int defaultElo, int minElo, int maxElo) {
		return new IntegerSpinOption(ELO_NAME, trigger, defaultElo, minElo, maxElo);
	}
	/** Gets the standard option to limit the engine strength.
	 * @param trigger The consumer to call when value is changed.
	 * @return The standard {@value UsualOptions#LIMIT_STRENGTH_NAME} option.
	 */
	public static CheckOption limitStrength(Consumer<Boolean> trigger) {
		return new CheckOption(LIMIT_STRENGTH_NAME, trigger, false);
	}
	
	/** Gets the standard option to set the number of best moves to search.
	 * @param trigger The consumer to call when value is changed.
	 * @return The standard {@value UsualOptions#MULTI_PV_NAME} option. Default and min values are 1, max value is 256.
	 */
	public static SpinOption<Integer> multiPV(Consumer<Integer> trigger) {
		return new IntegerSpinOption(MULTI_PV_NAME, trigger, 1, 1, 256);
	}
	
	/** Gets the standard option to ask the engine to use its own opening book.
	 * @param trigger The consumer to call when value is changed.
	 * @param defaultValue The default value of the option.
	 * @return The standard {@value UsualOptions#OWN_BOOK_NAME} option.
	 */
	public static CheckOption ownBook(Consumer<Boolean> trigger, boolean defaultValue) {
		return new CheckOption(OWN_BOOK_NAME, trigger, defaultValue);
	}

	/** Gets the standard option to ask the engine to ponder.
	 * @param trigger The consumer to call when value is changed.
	 * @param defaultValue The default value of the option.
	 * @return The standard {@value UsualOptions#PONDER_NAME} option.
	 */
	public static CheckOption ponder(Consumer<Boolean> trigger, boolean defaultValue) {
		return new CheckOption(PONDER_NAME, trigger, defaultValue);
	}
	
	/** Gets an spin option to set the engine strength.
	 * @param trigger The consumer to call when value is changed.
	 * @param maxValue The maximum value of the option.
	 * @return an option whose name is {@value UsualOptions#LEVEL_NAME}, the default value is {@code maxValue} and minimal value is 0.
	 */
	public static SpinOption<Integer> level(Consumer<Integer> trigger, int maxValue) {
		return new IntegerSpinOption(LEVEL_NAME, trigger, maxValue, 0, maxValue);
	}
	
	/** Gets an spin option to set the number of CPU threads used for searching a position.
	 * @param trigger The consumer to call when value is changed.
	 * @param defaultValue The default value of the option.
	 * @return an option whose name is {@value UsualOptions#THREADS_NAME}, the minimal value is 1 and and maximal value is the number of available processors reported by {@code Runtime.getRuntime().availableProcessors()}
	 */
	public static SpinOption<Integer> threads(Consumer<Integer> trigger, int defaultValue) {
		return new IntegerSpinOption(THREADS_NAME, trigger, defaultValue, 1, Runtime.getRuntime().availableProcessors());
	}
}
