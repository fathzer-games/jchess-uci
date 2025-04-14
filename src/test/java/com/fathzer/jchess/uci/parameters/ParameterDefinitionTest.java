package com.fathzer.jchess.uci.parameters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ParameterDefinitionTest {

	@Test
	void test() {
		assertThrows(IllegalArgumentException.class, () -> new ParameterDefinition<>(null, "toto"));
		assertThrows(IllegalArgumentException.class, () -> new ParameterDefinition<>((x,y) -> {}, null));
		assertThrows(IllegalArgumentException.class, () -> new ParameterDefinition<>((x,y) -> {}, ""));
		assertThrows(IllegalArgumentException.class, () -> new ParameterDefinition<>((x,y) -> {}, " "));
		assertThrows(IllegalArgumentException.class, () -> new ParameterDefinition<>((x,y) -> {}, "toto", ""));
		assertThrows(IllegalArgumentException.class, () -> new ParameterDefinition<>((x,y) -> {}, "toto", "t", null));
		assertThrows(IllegalArgumentException.class, () -> new ParameterDefinition<>((x,y) -> {}, "toto", "t", "  "));
	}

}
