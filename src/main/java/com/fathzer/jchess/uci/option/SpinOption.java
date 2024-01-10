package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

abstract class SpinOption<N extends Number> extends Option<N> {
	private final N defaultValue;
	private final N min;
	private final N max;
	
	protected SpinOption(String name, Consumer<N> trigger, N defaultValue, N min, N max) {
		super(name, trigger);
		if (compare(defaultValue, min)<0 || compare(defaultValue, max)>0) {
			throw new IllegalArgumentException("default ("+defaultValue+") is not between min ("+min+") and max("+max+")");
		}
		this.defaultValue = defaultValue;
		setCastedValue(defaultValue);
		this.min = min;
		this.max = max;
	}
	
	protected abstract N parse(String value);
	protected abstract int compare(N first, N other);

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
		if (compare(val, max)>0 || compare(val, min)<0) {
			throw new IllegalArgumentException();
		}
		setCastedValue(val);
	}

	@Override
	public String toUCI() {
		return super.toUCI()+DEFAULT+this.defaultValue+" min "+this.min+" max "+this.max;
	}
}
