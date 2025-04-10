package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

/** A check UCI option. */
public class CheckOption extends Option<Boolean> {
	private final boolean defaultValue;
	
	/** Constructor.
	 * @param name The name of the option
	 * @param trigger The action to perform when the option is set
	 * @param defaultValue The default value of the option
	 * @throws IllegalArgumentException if the trigger or name are null
	 */
	public CheckOption(String name, Consumer<Boolean> trigger, boolean defaultValue) {
		super(name, trigger);
		this.defaultValue = defaultValue;
		setCastedValue(defaultValue);
	}

	@Override
	Type getType() {
		return Type.CHECK;
	}
	
	@Override
	public void setValue(String value) {
		if ("true".equals(value)) {
			setCastedValue(true);
		} else if ("false".equals(value)) {
			setCastedValue(false);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	public String toUCI() {
		return super.toUCI()+DEFAULT+defaultValue;
	}
}
