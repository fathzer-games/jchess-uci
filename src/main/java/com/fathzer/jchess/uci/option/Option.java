package com.fathzer.jchess.uci.option;

import java.util.Objects;
import java.util.function.Consumer;

/** An abstract UCI option. */
public abstract class Option<T> {
	static final String DEFAULT = " default ";
	
	/** The type of the option. */
	enum Type {
		/** A check UCI option. */
		CHECK,
		/** A spin UCI option. */
		SPIN,
		/** A combo UCI option. */
		COMBO,
		/** A button UCI option. */
		BUTTON,
		/** A string UCI option. */
		STRING
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

	/** Gets the option's name
	 * @return a String
	 */
	public String getName() {
		return name;
	}
	
	abstract Type getType();
	
	/** Gets the current value of the option.
	 * 
	 * @return the current value of the option.
	 */
	public T getValue() {
		return value;
	}

	/** Sets the current value of the option.
	 * 
	 * @param value the new value of the option.
	 */
	public abstract void setValue(String value);
	
	void setCastedValue(T value) {
		final boolean equals = Objects.equals(this.value, value);
		this.value = value;
		if (!equals || Type.BUTTON.equals(getType())) {
			trigger.accept(value);
		}
	}
	
	/** Gets the UCI representation of the option (the one returned when the <i>uci</i> command is invoked).
	 * @return a String.
	 */
	public String toUCI() {
		return "option name " + getName() + " type "+getType().toString().toLowerCase();
	}
}
