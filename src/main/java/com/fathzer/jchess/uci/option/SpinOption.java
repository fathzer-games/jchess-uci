package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

public class SpinOption extends Option<Integer> {
	private final int defaultValue;
	private final int min;
	private final int max;
	
	public SpinOption(String name, Consumer<Integer> trigger, int defaultValue, int min, int max) {
		super(name, trigger);
		if (defaultValue<min || defaultValue>max) {
			throw new IllegalArgumentException();
		}
		this.defaultValue = defaultValue;
		setCastedValue(defaultValue);
		this.min = min;
		this.max = max;
	}

	@Override
	Type getType() {
		return Type.SPIN;
	}
	
	@Override
	public void setValue(String value) {
		if (value==null) {
			throw new IllegalArgumentException();
		}
		final int val = Integer.parseInt(value);
		if (val>max || val<min) {
			throw new IllegalArgumentException();
		}
		setCastedValue(val);
	}

	@Override
	public String toUCI() {
		return super.toUCI()+DEFAULT+this.defaultValue+" min "+this.min+" max "+this.max;
	}
}
