package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

abstract class SpinOption<N extends Number & Comparable<N>> extends Option<N> {
	private final N defaultValue;
	private final N min;
	private final N max;
	
	protected SpinOption(String name, Consumer<N> trigger, N defaultValue, N min, N max) {
		super(name, trigger);
		if (defaultValue.compareTo(min)<0 || defaultValue.compareTo(max)>0) {
			throw new IllegalArgumentException("default ("+defaultValue+") is not between min ("+min+") and max("+max+")");
		}
		this.defaultValue = defaultValue;
		setCastedValue(defaultValue);
		this.min = min;
		this.max = max;
	}
	
	protected abstract N parse(String value);

	@Override
	Type getType() {
		return Type.SPIN;
	}
	
	@Override
	public void setValue(String value) {
		if (value==null) {
			throw new IllegalArgumentException();
		}
		final N val = parse(value);
		if (val.compareTo(max)>0 || val.compareTo(min)<0) {
			throw new IllegalArgumentException();
		}
		setCastedValue(val);
	}

	@Override
	public String toUCI() {
		return super.toUCI()+DEFAULT+this.defaultValue+" min "+this.min+" max "+this.max;
	}
}
