package com.fathzer.jchess.uci.parameters;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class Parser<T> {
	private final Map<String, BiConsumer<T, Deque<String>>> parserMap;
	
	public Parser(Collection<ParamProperties<T>> paramProperties) {
		parserMap = new HashMap<>();
		for (ParamProperties<T> param : paramProperties) {
			add(param);
		}
	}
	
	public void add(ParamProperties<T> property) {
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
