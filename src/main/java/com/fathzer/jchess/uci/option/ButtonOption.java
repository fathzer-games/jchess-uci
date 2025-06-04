package com.fathzer.jchess.uci.option;

import java.util.function.Consumer;

/** A button UCI option. */
public class ButtonOption extends Option<Void> {
	
	/** Constructor.
 * @param name The name of the option
 * @param trigger The action to perform when the option is set
 * @throws IllegalArgumentException if the trigger or name are null
 */
	public ButtonOption(String name, Runnable trigger) {
		super(name, getConsumer(trigger));
	}
	
	private static Consumer<Void> getConsumer(Runnable trigger) {
		if (trigger==null) {
			throw new IllegalArgumentException();
		}
		return x -> trigger.run();
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
