package com.fathzer.jchess.uci;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

class ParamsParser<V> {
	private final Consumer<String> errConsumer;
	private final BiPredicate<Integer, V> valueValidator;
	private final Function<String,V> parser;
	
	ParamsParser(Consumer<String> errConsumer, Function<String,V> parser, BiPredicate<Integer, V> valueValidator) {
		this.errConsumer = errConsumer;
		this.valueValidator = valueValidator;
		this.parser = parser;
	}
	
	Optional<List<V>> parse(String[] tokens, List<String> wordings, List<V> defaultValues) {
		if (wordings.size()!=defaultValues.size()) {
			throw new IllegalArgumentException();
		}
		final int nb = wordings.size();
		final List<V> result = new ArrayList<>(nb);
		for (int i = 0; i < nb; i++) {
			final V value = parseToken(tokens, i, defaultValues.get(i), wordings.get(i));
			if (value==null) {
				return Optional.empty();
			} else {
				result.add(value);
			}
		}
		// Ignore extra tokens
		if (nb < tokens.length) {
			errConsumer.accept("Extra tokens are ignored");
		}
		return Optional.of(result);
	}
	
	private V parseToken(String[] tokens, final int index, V defaultValue, String wording) {
		if (tokens.length<=index) {
			if (defaultValue==null) {
				errConsumer.accept("Missing argument");
			}
			return defaultValue;
		}
		try {
			final V value = parser.apply(tokens[index]);
			return valueValidator.test(index, value) ? value : null;
		} catch (IllegalArgumentException e) {
			errConsumer.accept("invalid "+wording+" parameter: "+tokens[index]);
			return null;
		}
	}

}
