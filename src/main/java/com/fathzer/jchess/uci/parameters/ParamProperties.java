package com.fathzer.jchess.uci.parameters;

import java.util.Deque;
import java.util.function.BiConsumer;

public class ParamProperties<T> {
	private String[] names;
	private BiConsumer<T, Deque<String>> parser;
	
	public ParamProperties(BiConsumer<T, Deque<String>> parser, String ... names) {
		this.names = names;
		this.parser = parser;
	}

	public String[] getNames() {
		return names;
	}

	public BiConsumer<T, Deque<String>> getParser() {
		return parser;
	}
}
