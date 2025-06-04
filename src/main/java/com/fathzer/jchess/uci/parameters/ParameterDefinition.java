package com.fathzer.jchess.uci.parameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The description of a parameter of a command.
 * <br>For instance the description of the <i>depth</i> parameter of the <i>go</i> command as <i>depth</i> as name and
 * a consumer/parser that converts the first element of the dequeue to an integer and sets the {@link GoParameters#getDepth()}
 * to this integer.
 * @param <T> the type of the command parameters.
 */
public class ParameterDefinition<T> {
	private final List<String> names;
	private final BiConsumer<T, Deque<String>> parser;
	
	/** Constructor.
	 * @param parser the action to use to parse the parameter value.
	 * @param name the name of the parameter.
	 * @param aliases the aliases of the parameter.
	 * @throws IllegalArgumentException if the parser is null or if non names are provided the names are null or blank.
	 */
	public ParameterDefinition(BiConsumer<T, Deque<String>> parser, String name, String ... aliases) {
		if (parser==null) {
			throw new IllegalArgumentException();
		}
		this.names = new LinkedList<>();
		this.names.add(name);
		this.names.addAll(Arrays.asList(aliases));
		if (names.stream().anyMatch(n -> n==null || n.isBlank())) {
			throw new IllegalArgumentException();
		}
		this.parser = parser;
	}

	/** Gets the names of the parameter and its aliases.
	 * @return a unmodifiable list of names.
	 */
	public List<String> getNames() {
		return Collections.unmodifiableList(names);
	}

	/** Gets the action to use to parse the parameter value.
	 * @return the parser to use to parse the parameter value.
	 */
	public BiConsumer<T, Deque<String>> getParser() {
		return parser;
	}
}
