package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

public class StringOption extends Option<String> {
	private final String defaultValue;
	
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
