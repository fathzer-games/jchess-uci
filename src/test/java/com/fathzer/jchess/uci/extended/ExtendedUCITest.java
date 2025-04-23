package com.fathzer.jchess.uci.extended;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.util.InstrumentedUCI;

class ExtendedUCITest {
	private interface MyEngine extends Engine, Displayable {
		
	}

	@Test
	void testDisplay() {
		final MyEngine engine = mock(MyEngine.class);
		when(engine.getFEN()).thenReturn("fen");
		when(engine.getBoardAsString()).thenReturn("board");
		
		try (InstrumentedUCI uci = InstrumentedUCI.start(engine)) {
			try {
				// Test no position set
				uci.post("d");
				uci.assertDebug();
				
				// Test with position
				uci.clear();
				uci.post("position startpos");
				uci.post("d");
				assertEquals(Collections.singletonList("board"), uci.out());
				uci.clear();
				
				// Test with board representation
				uci.post("d fen");
				assertEquals(Collections.singletonList("fen"), uci.out());
				uci.clear();
				
				// Test with illegalArgument
				uci.post("d foo");
				uci.assertDebug();
			} finally {
				uci.post("q");
			}
		}
	}

}
