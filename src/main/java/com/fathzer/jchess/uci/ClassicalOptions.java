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
	
	public static CheckOption chess960(Consumer<Boolean> trigger) {
		return new CheckOption(CHESS960_NAME, trigger, false);
	}
	
	public static ButtonOption clearHash(Consumer<Void> trigger) {
		return new ButtonOption(HASH_NAME, trigger);
	}
	
	public static CheckOption limitStrength(Consumer<Boolean> trigger) {
		return new CheckOption(LIMIT_STRENGTH_NAME, trigger, false);
	}
	
	public static SpinOption<Integer> multiPV(Consumer<Integer> trigger, int maxValue) {
		return new IntegerSpinOption(MULTI_PV_NAME, trigger, 1, 1, maxValue);
	}
	
	public static CheckOption ownBook(Consumer<Boolean> trigger, boolean defaultValue) {
		return new CheckOption(OWN_BOOK_NAME, trigger, defaultValue);
	}
	
	public static CheckOption ponder(Consumer<Boolean> trigger, boolean defaultValue) {
		return new CheckOption(PONDER_NAME, trigger, defaultValue);
	}
	
	public static SpinOption<Integer> level(Consumer<Integer> trigger, int maxValue) {
		return new IntegerSpinOption(LEVEL_NAME, trigger, maxValue, 0, maxValue);
	}
	
	public static SpinOption<Integer> threads(Consumer<Integer> trigger, int defaultValue) {
		return new IntegerSpinOption(THREADS_NAME, trigger, defaultValue, 1, Runtime.getRuntime().availableProcessors());
	}
}
