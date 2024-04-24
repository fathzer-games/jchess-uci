package com.fathzer.jchess.uci;

import java.util.function.Consumer;

import com.fathzer.jchess.uci.option.ButtonOption;
import com.fathzer.jchess.uci.option.CheckOption;
import com.fathzer.jchess.uci.option.IntegerSpinOption;
import com.fathzer.jchess.uci.option.SpinOption;

/** Provides some classical options found in uci protocol definition or in Stockfish.
 */
public final class ClassicalOptions {
	public static final String CLEAR_HASH_NAME = "Clear Hash"; 
	public static final String HASH_NAME = "Hash"; 
	public static final String MULTI_PV_NAME = "MultiPV";
	public static final String OWN_BOOK_NAME = "OwnBook";
	public static final String PONDER_NAME = "Ponder";
	public static final String CHESS960_NAME = "UCI_Chess960";
	public static final String ELO_NAME = "UCI_Elo";
	public static final String LIMIT_STRENGTH_NAME = "UCI_LimitStrength";

	public static final String LEVEL_NAME = "Skill Level";
	public static final String THREADS_NAME = "Threads";
	
	private ClassicalOptions() {
		super();
	}
	
	/** Gets the standard option to accept Chess 960 games. 
	 * @param trigger The consumer to call when value is changed.
	 * @return The standard {@value ClassicalOptions#CHESS960_NAME} option.
	 */
	public static CheckOption chess960(Consumer<Boolean> trigger) {
		return new CheckOption(CHESS960_NAME, trigger, false);
	}
	
	public static ButtonOption clearHash(Consumer<Void> trigger) {
		return new ButtonOption(HASH_NAME, trigger);
	}
	
	public static CheckOption limitStrength(Consumer<Boolean> trigger) {
		return new CheckOption(LIMIT_STRENGTH_NAME, trigger, false);
	}
	
	public static SpinOption<Integer> multiPV(Consumer<Integer> trigger) {
		return new IntegerSpinOption(MULTI_PV_NAME, trigger, 1, 1, 256);
	}
	
	public static CheckOption ownBook(Consumer<Boolean> trigger, boolean defaultValue) {
		return new CheckOption(OWN_BOOK_NAME, trigger, defaultValue);
	}
	
	public static CheckOption ponder(Consumer<Boolean> trigger, boolean defaultValue) {
		return new CheckOption(PONDER_NAME, trigger, defaultValue);
	}
	
	/** Gets an spin option to set the engine strength.
	 * @param trigger The consumer to call when value is changed.
	 * @param maxValue The maximum value of the option.
	 * @return an option whose name is {@value ClassicalOptions#LEVEL_NAME}, the default value is {@code maxValue} and minimal value is 0.
	 */
	public static SpinOption<Integer> level(Consumer<Integer> trigger, int maxValue) {
		return new IntegerSpinOption(LEVEL_NAME, trigger, maxValue, 0, maxValue);
	}
	
	/** Gets an spin option to set the number of CPU threads used for searching a position.
	 * @param trigger The consumer to call when value is changed.
	 * @param defaultValue The default value of the option.
	 * @return an option whose name is {@value ClassicalOptions#THREADS_NAME}, the minimal value is 1 and and maximal value is the number of available processors reported by {@code Runtime.getRuntime().availableProcessors()}
	 */
	public static SpinOption<Integer> threads(Consumer<Integer> trigger, int defaultValue) {
		return new IntegerSpinOption(THREADS_NAME, trigger, defaultValue, 1, Runtime.getRuntime().availableProcessors());
	}
}
