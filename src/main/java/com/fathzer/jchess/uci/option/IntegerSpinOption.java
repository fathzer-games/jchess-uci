package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

/** An integer spin UCI option. */
public class IntegerSpinOption extends SpinOption<Integer> {
	/** Constructor.
	 * @param name The name of the option
	 * @param trigger The action to perform when the option is set
	 * @param defaultValue The default value of the option
	 * @param min The minimum value of the option
	 * @param max The maximum value of the option
	 * @throws IllegalArgumentException if the default value is not between min and max or if the trigger or name are null
	 */
	public IntegerSpinOption(String name, Consumer<Integer> trigger, int defaultValue, int min, int max) {
		super(name, trigger, defaultValue, min, max);
	}

	@Override
	protected Integer parse(String value) {
		return Integer.parseInt(value);
	}
}
