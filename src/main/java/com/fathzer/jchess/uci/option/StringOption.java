package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

/** A string UCI option. */
public class StringOption extends Option<String> {
	private final String defaultValue;
	
	/** Constructor.
	 * @param name The name of the option
	 * @param trigger The action to perform when the option is set
	 * @param defaultValue The default value of the option
	 * @throws IllegalArgumentException if the trigger, name or default value are null
	 */
	public StringOption(String name, Consumer<String> trigger, String defaultValue) {
		super(name, trigger);
		if (defaultValue==null) {
			throw new IllegalArgumentException();
		}
		this.defaultValue = defaultValue;
		this.setValue(defaultValue);
	}

	@Override
	Type getType() {
		return Type.STRING;
	}
	
	@Override
	public void setValue(String value) {
		if (value==null) {
			throw new IllegalArgumentException();
		}
		setCastedValue(value);
	}

	@Override
	public String toUCI() {
		return super.toUCI()+DEFAULT+this.defaultValue;
	}
}
