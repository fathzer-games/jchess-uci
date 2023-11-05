package com.fathzer.jchess.uci.option;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ComboOption extends Option<String> {
	private final String defaultValue;
	private final Set<String> values;
	
	public ComboOption(String name, Consumer<String> trigger, String defaultValue, Set<String> values) {
		super(name, trigger);
		if (!values.contains(defaultValue) || values.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.defaultValue = defaultValue;
		this.values = values;
		this.setValue(defaultValue);
	}

	@Override
	Type getType() {
		return Type.COMBO;
	}
	
	@Override
	public void setValue(String value) {
		if (value==null || !values.contains(value)) {
			throw new IllegalArgumentException();
		}
		setCastedValue(value);
	}

	@Override
	public String toUCI() {
		return super.toUCI()+DEFAULT+this.defaultValue+values.stream().map(v->" var "+v).collect(Collectors.joining());
	}
}
