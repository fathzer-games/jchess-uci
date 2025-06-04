package com.fathzer.jchess.uci;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.fathzer.jchess.uci.Engine.*;

import com.fathzer.jchess.uci.option.Option;
import com.fathzer.jchess.uci.parameters.GoParameters;

class EngineTest {
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
	
	@Id("minimal")
	private static class MinimalEngine extends FakeEngine {}

	@Id("Tagged")
	@Author("Tagger")
	@Chess960Supported
	private static class TaggedSubclass extends FakeEngine {
	}
	
	private static class ADirectSubclass extends TaggedSubclass {
	}
	

	@Test
	void testAnnotations() {
		final Engine minimal = new MinimalEngine();
		assertEquals("minimal", minimal.getId());
		assertFalse(minimal.isChess960Supported());
		assertNull(minimal.getAuthor());
		assertEquals(-1, minimal.getDefaultHashTableSize());
		
		final Engine tagged = new TaggedSubclass();
		assertEquals("Tagged", tagged.getId());
		assertEquals("Tagger", tagged.getAuthor());
		assertTrue(tagged.isChess960Supported());
		
		final Engine aDirectSubclass = new ADirectSubclass();
		assertEquals("Tagged", aDirectSubclass.getId());
		assertEquals("Tagger", aDirectSubclass.getAuthor());
		assertTrue(aDirectSubclass.isChess960Supported());
		
		final Engine innerClass = new @Engine.Id("toto") @Author("me") @Chess960Supported FakeEngine(){};
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
		
		final Engine incompleteEngine = new @Chess960Supported FakeEngine() {
			@Override
			public int getDefaultHashTableSize() {
				return 16;
			}

			@Override
			public boolean hasOwnBook() {
				return true;
			}
		};
		
		assertThrows(IllegalStateException.class, () -> incompleteEngine.setChess960(false));
		assertThrows(IllegalStateException.class, () -> incompleteEngine.setChess960(true));
		assertThrows(IllegalStateException.class, () -> incompleteEngine.setOwnBook(false));
		assertThrows(IllegalStateException.class, () -> incompleteEngine.setOwnBook(true));
		assertThrows(IllegalStateException.class, () -> incompleteEngine.setHashTableSize(64));
		assertThrows(IllegalStateException.class, incompleteEngine::clearHashTable);
		
		final Engine fullEngine = new @Chess960Supported FakeEngine() {
			@Override
			public int getDefaultHashTableSize() {
				return 16;
			}

			@Override
			public boolean hasOwnBook() {
				return true;
			}

			@Override
			public void setHashTableSize(int sizeInMB) {
				// Does nothing
			}

			@Override
			public void clearHashTable() {
				// Does nothing
			}

			@Override
			public void setChess960(boolean chess960Mode) {
				// Does nothing
			}

			@Override
			public void setOwnBook(boolean activate) {
				// Does nothing
			}
		};
		Map<String, Option<?>> options = fullEngine.getOptions();
		assertNotNull(options);
		assertEquals(4, options.size());
		assertNotNull(options.get(UsualOptions.CHESS960_NAME));
		final Option<?> hash = options.get(UsualOptions.HASH_NAME);
		assertNotNull(hash);
		assertEquals(fullEngine.getDefaultHashTableSize(), hash.getValue());
		assertNotNull(options.get(UsualOptions.CLEAR_HASH_NAME));
		assertNotNull(options.get(UsualOptions.OWN_BOOK_NAME));
	}
}
