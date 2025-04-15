package com.fathzer.jchess.uci.parameters;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Parses the parameters of a command.
 * <br>The parameters have a name optionally followed by one or more values.
 * <br>The values of a parameter extends until no more tokens are available or a new parameter name is encountered.
 * <br>For example: <code>go wtime 297999 btime 300000 winc 3000 binc 3000</code>
 * @param <T> the type of the object that represents the command parameters.
 */
public class Parser<T> {
	private final Map<String, BiConsumer<T, Deque<String>>> parserMap;
	
	/**
	 * Constructor.
	 * @param paramProperties the properties of the command parameters.
	 */
	public Parser(Collection<ParameterDefinition<T>> paramProperties) {
		parserMap = new HashMap<>();
		for (ParameterDefinition<T> param : paramProperties) {
			add(param);
		}
	}

	/** Adds a new parameter.
	 * @param property the property to add.
	 * @throws IllegalArgumentException if the name is already registered.
	 */
	public void add(ParameterDefinition<T> property) {
		for (String name : property.getNames()) {
			if (parserMap.putIfAbsent(name, property.getParser())!=null) {
				throw new IllegalArgumentException(name+" is already registered");
			}
		}
	}

	/** Parses the parameters of a command.
	 * @param target The object that represents the command parameters. It should be initialized to its default value.
	 * @param tokens the command parameters as tokens (for example: wtime, 297999, btime, 300000, winc, 3000, binc, 3000 for a <i>go</i> command)
	 * @return The list of ignored tokens. <i>target</i> is updated by this method.
	 * @throws IllegalArgumentException if a token is illegal (for instance, we expected a number, but we got a string).
	 */
	public List<String> parse(T target, Deque<String> tokens) {
		final List<String> ignoredOptions = new LinkedList<>();
		while (!tokens.isEmpty()) {
			final String token = tokens.pop();
			final BiConsumer<T, Deque<String>> parser = parserMap.get(token);
			if (parser==null) {
				ignoredOptions.add(token);
			} else {
				// Compute the arguments (all tokens until next command)
				final Deque<String> arguments = new LinkedList<>();
				while (!tokens.isEmpty()) {
					final String arg = tokens.peek();
					if (!parserMap.containsKey(arg)) {
						tokens.pop();
						arguments.add(arg);
					} else {
						break;
					}
				}
				parser.accept(target, arguments);
				if (!arguments.isEmpty()) {
					// If some arguments were not consumed, add them to ignored arguments
					ignoredOptions.addAll(arguments);
				}
			}
		}
		return ignoredOptions;
	}
	
	/**
	 * Parses a positive integer from a token list.
	 * @param arguments the token list.
	 * @return the parsed value. The first token of the list is removed.
	 * @throws IllegalArgumentException if the token list is empty or the first token is not a positive integer.
	 */
	public static int positiveInt(Deque<String> arguments) {
		if (arguments.isEmpty()) {
			throw new IllegalArgumentException("Expected a value, but none is provided");
		}
		final String value = arguments.pop();
		final int result = Integer.parseInt(value);
		if (result<0) {
			throw new IllegalArgumentException("Unexpected negative number "+value);
		}
		return result;
	}
}
