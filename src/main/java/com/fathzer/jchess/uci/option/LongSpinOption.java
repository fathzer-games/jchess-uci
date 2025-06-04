package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

/** A long spin UCI option. */
public class LongSpinOption extends SpinOption<Long> {
	/** Constructor.
	 * @param name The name of the option
	 * @param trigger The action to perform when the option is set
	 * @param defaultValue The default value of the option
	 * @param min The minimum value of the option
	 * @param max The maximum value of the option
	 * @throws IllegalArgumentException if the default value is not between min and max or if the trigger or name are null
	 */
	public LongSpinOption(String name, Consumer<Long> trigger, long defaultValue, long min, long max) {
		super(name, trigger, defaultValue, min, max);
	}

	@Override
	protected Long parse(String value) {
		return Long.parseLong(value);
	}
}
