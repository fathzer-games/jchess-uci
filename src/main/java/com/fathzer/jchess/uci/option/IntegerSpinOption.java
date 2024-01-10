package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

public class IntegerSpinOption extends SpinOption<Integer> {
	
	public IntegerSpinOption(String name, Consumer<Integer> trigger, int defaultValue, int min, int max) {
		super(name, trigger, defaultValue, min, max);
	}

	@Override
	protected Integer parse(String value) {
		return Integer.parseInt(value);
	}

	@Override
	protected int compare(Integer first, Integer other) {
		return first.compareTo(other);
	}

}
