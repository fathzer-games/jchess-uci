package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

/** An abstract spin UCI option. */
public abstract class SpinOption<N extends Number & Comparable<N>> extends Option<N> {
	private final N defaultValue;
	private final N min;
	private final N max;
	
	/** Constructor.
	 * @param name The name of the option
	 * @param trigger The action to perform when the option is set
	 * @param defaultValue The default value of the option
	 * @param min The minimum value of the option
	 * @param max The maximum value of the option
	 * @throws IllegalArgumentException if the default value is not between min and max
	 */
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
	
	/** Parses the value.
	 * @param value The value to parse
	 * @return The parsed value
	 */
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
