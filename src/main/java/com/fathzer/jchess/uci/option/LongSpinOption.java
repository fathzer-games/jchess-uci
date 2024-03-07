package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

public class LongSpinOption extends SpinOption<Long> {
	
	public LongSpinOption(String name, Consumer<Long> trigger, long defaultValue, long min, long max) {
		super(name, trigger, defaultValue, min, max);
	}

	@Override
	protected Long parse(String value) {
		return Long.parseLong(value);
	}
}
