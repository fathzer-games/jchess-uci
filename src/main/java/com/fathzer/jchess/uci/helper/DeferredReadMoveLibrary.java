package com.fathzer.jchess.uci.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.EvaluatedMove;
import com.fathzer.games.movelibrary.MoveLibrary;

/** A move library that encapsulates another move library and read its content outside of its constructor.
 * <br>This typically allows an UCI engine to instantiate a move library during the launch process, and
 * deferred the effective read of the moves during the <i>isReady</i> command execution as specified in
 * UCI protocol. 
 * @param <M> The type of a move
 * @param <B> The type of the move generator used by the UCI engine
 */
public class DeferredReadMoveLibrary<M, B extends MoveGenerator<M>> implements MoveLibrary<M, B> {
	/** A interface that can read an object from an URL and sends an IOException if sommething goes wrong. 
	 * @param <T> The type of the read object
	 */
	@FunctionalInterface
	public static interface IOReader<T> {
		/** Reads an object.
		 * @param url The url where to read the object
		 * @return The object
		 * @throws IOException If something went wrong
		 */
		T read(URL url) throws IOException;
	}
	
	private final String url;
	private final IOReader<MoveLibrary<M, B>> reader;
	private MoveLibrary<M, B> internal;

	/** Constructor.
	 * @param url The url where to read the MoveLibrary
	 * @param reader The reader to read the library.
	 */
	public DeferredReadMoveLibrary(String url, IOReader<MoveLibrary<M,B>> reader) {
		if (url==null || reader==null) {
			throw new IllegalArgumentException();
		}
		this.url = url;
		this.reader = reader;
	}
	
	

	@Override
	public List<EvaluatedMove<M>> getMoves(B board) {
		if (internal==null) {
			return Collections.emptyList();
		}
		return internal.getMoves(board);
	}

	/** {@inheritDoc}
	 * <br>If the move library is not initialized, an empty optional is returned.
	 */
	@Override
	public Optional<EvaluatedMove<M>> apply(B board) {
		if (internal==null) {
			return Optional.empty();
		}
		return internal.apply(board);
	}
	
	/** Tests if this move library should be initialized.
	 * @return false if initialization has been made and succeeded.
	 * @see #init()
	 */
	public boolean isInitRequired() {
		return internal==null;
	}
	
	/** Initializes this move library.
	 * @throws IOException If something went wrong
	 */
	public void init() throws IOException {
		if (isInitRequired()) {
			internal = reader.read(toURL(this.url));
		}
	}
	
	static URL toURL(String path) throws IOException {
		URL url;
		try {
			url = new URL(path);
		} catch (MalformedURLException e) {
			File file = new File(path);
			if (!file.exists()) {
				throw new FileNotFoundException();
			}
			url = file.toURI().toURL();
		}
		return url;
	}

	/** Gets the URL where the data should be/has been read.
	 * @return A String
	 */
	public String getUrl() {
		return url;
	}
}
