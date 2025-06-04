package com.fathzer.jchess.uci.option;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** A combo UCI option. */
public class ComboOption extends Option<String> {
	private final String defaultValue;
	private final Set<String> values;
	
	/** Constructor.
	 * @param name The name of the option
	 * @param trigger The action to perform when the option is set
	 * @param defaultValue The default value of the option
	 * @param values The values the option can take
	 * @throws IllegalArgumentException if the default value is not in the set of values or if the trigger or name are null
	 */
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
