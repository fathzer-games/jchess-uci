package com.fathzer.jchess.uci;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import static com.fathzer.jchess.uci.Engine.*;

import com.fathzer.jchess.uci.parameters.GoParameters;
import com.fathzer.jchess.uci.util.InstrumentedEngine;

class EngineTest {
	@Id("Tagged")
	@Author("Tagger")
	@Chess960Supported
	private static class TaggedSubclass extends InstrumentedEngine {
	}
	
	private static class ADirectSubclass extends TaggedSubclass {
	}
	
	private static class FakeEngine implements Engine {
		@Override
		public void setStartPosition(String fen) {
			// Fake
		}

		@Override
		public void move(UCIMove move) {
			// Fake
		}

		@Override
		public StoppableTask<GoReply> go(GoParameters params) {
			return null;
		}
	}

	@Test
	void testAnnotations() {
		final Engine instrumentedEngine = new InstrumentedEngine();
		assertEquals("InstrumentedEngine", instrumentedEngine.getId());
		assertFalse(instrumentedEngine.isChess960Supported());
		assertNull(instrumentedEngine.getAuthor());
		assertEquals(-1, instrumentedEngine.getDefaultHashTableSize());
		
		final Engine tagged = new TaggedSubclass();
		assertEquals("Tagged", tagged.getId());
		assertEquals("Tagger", tagged.getAuthor());
		assertTrue(tagged.isChess960Supported());
		
		final Engine aDirectSubclass = new ADirectSubclass();
		assertEquals("Tagged", aDirectSubclass.getId());
		assertEquals("Tagger", aDirectSubclass.getAuthor());
		assertTrue(aDirectSubclass.isChess960Supported());
		
		final Engine innerClass = new @Engine.Id("toto") @Author("me") @Chess960Supported InstrumentedEngine(){};
		assertEquals("toto", innerClass.getId());
		assertTrue(innerClass.isChess960Supported());
		assertEquals("me", innerClass.getAuthor());
	}
	
	@Test
	void testDefault() {
		final Engine engine = new FakeEngine();
		assertThrows(IllegalStateException.class, engine::getId);
		assertNull(engine.getAuthor());
		assertFalse(engine.isChess960Supported());

		assertThrows(UnsupportedOperationException.class, () -> engine.setHashTableSize(10));
		assertThrows(UnsupportedOperationException.class, engine::clearHashTable);
		assertThrows(UnsupportedOperationException.class, () -> engine.setChess960(true));
		assertThrows(UnsupportedOperationException.class, () -> engine.setChess960(false));
		
		assertThrows(UnsupportedOperationException.class, () -> engine.setOwnBook(true));
		assertThrows(UnsupportedOperationException.class, () -> engine.setOwnBook(false));
		
		assertTrue(engine.getOptions().isEmpty());
		
		final Engine fullEngine = new @Chess960Supported FakeEngine() {
			@Override
			public int getDefaultHashTableSize() {
				return 16;
			}

			@Override
			public boolean hasOwnBook() {
				return true;
			}
		};
		assertThrows(IllegalStateException.class, () -> fullEngine.setChess960(false));
		assertThrows(IllegalStateException.class, () -> fullEngine.setChess960(true));
		assertThrows(IllegalStateException.class, () -> fullEngine.setOwnBook(false));
		assertThrows(IllegalStateException.class, () -> fullEngine.setOwnBook(true));
		assertThrows(IllegalStateException.class, () -> fullEngine.setHashTableSize(64));
		assertThrows(IllegalStateException.class, fullEngine::clearHashTable);
		
		//TODO Check getOptions
	}
}
