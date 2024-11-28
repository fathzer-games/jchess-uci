package com.fathzer.jchess.uci.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.EvaluatedMove;
import com.fathzer.games.ai.evaluation.Evaluation;
import com.fathzer.games.movelibrary.MoveLibrary;

class DeferredReadMoveLibraryTest {
	
	@SuppressWarnings("unchecked")
	private final MoveGenerator<String> mv = mock(MoveGenerator.class);

	@Test
	void test() throws Exception {
		String httpsURL = "https://myApp.org/file.gz";
		assertEquals(URI.create(httpsURL).toURL(), DeferredReadMoveLibrary.toURL(httpsURL));
		
		assertThrows(IOException.class, () -> DeferredReadMoveLibrary.toURL("httpx://myApp.org/file.gz"));
		assertThrows(IOException.class, () -> DeferredReadMoveLibrary.toURL("src/test/resources/unknownFile.json"));
		
		final String path = "src/test/resources/FakeOpenings.txt";
		final DeferredReadMoveLibrary<String, MoveGenerator<String>> rb = new DeferredReadMoveLibrary<>(path, this::readOpenings);
		assertTrue(rb.isInitRequired());
		assertTrue(rb.apply(mv).isEmpty());
		rb.init();
		assertFalse(rb.isInitRequired());
		assertFalse(rb.apply(mv).isEmpty());
		// A second init call should no throw any exception
		rb.init();
	}
	
	private MoveLibrary<String, MoveGenerator<String>> readOpenings(URL url) {
		return new MoveLibrary<String, MoveGenerator<String>>() {
			@Override
			public Optional<EvaluatedMove<String>> apply(MoveGenerator<String> board) {
				return mv.equals(board) ? Optional.of(new EvaluatedMove<>("ok", Evaluation.score(100))) : Optional.empty(); 
			}
		};
	}
}
