package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

public class CheckOption extends Option<Boolean> {
	private final boolean defaultValue;
	
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
