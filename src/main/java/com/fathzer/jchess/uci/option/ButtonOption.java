package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

public class ButtonOption extends Option<Void> {
	
	public ButtonOption(String name, Consumer<Void> trigger) {
		super(name, trigger);
	}
	
	@Override
	Type getType() {
		return Type.BUTTON;
	}

	@Override
	public void setValue(String value) {
		if (value!=null) {
			throw new IllegalArgumentException("Button does not accept any value");
		}
		setCastedValue(null);
	}
}
