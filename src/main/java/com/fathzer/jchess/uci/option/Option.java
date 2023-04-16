package com.fathzer.jchess.uci.option;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class Option<T> {
	static final String DEFAULT = " default ";
	
	enum Type {
		CHECK, SPIN, COMBO, BUTTON, STRING
	}
	
	private final String name;
	private T value;
	private final Consumer<T> trigger;  

	Option(String name, Consumer<T> trigger) {
		if (name==null || trigger==null) {
			throw new IllegalArgumentException();
		}
		this.name = name;
		this.value = null;
		this.trigger = trigger;
	}

	public String getName() {
		return name;
	}
	
	abstract Type getType();
	
	public T getValue() {
		return value;
	}

	public abstract void setValue(String value);
	
	void setCastedValue(T value) {
		final boolean equals = Objects.equals(this.value, value);
		this.value = value;
		if (!equals || Type.BUTTON.equals(getType())) {
			trigger.accept(value);
		}
	}
	
	public String toUCI() {
		return "option name " + getName() + " type "+getType().toString().toLowerCase();
	}
}
