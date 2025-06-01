package com.fathzer.jchess.uci.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.EOFException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.games.perft.PerfTTestData;
import com.fathzer.games.util.UncheckedException;

import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.ThrowingRunnable;
import com.fathzer.jchess.uci.extended.ExtendedUCI;

public class InstrumentedUCI extends ExtendedUCI {
	private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedUCI.class);
	private static final int TIME_OU_MS = 1000;

	public static class UnknownCommandException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private UnknownCommandException(String command) {
			super(command);
		}
	}
	
	public static InstrumentedUCI start(Engine engine) {
		final InstrumentedUCI uci = new InstrumentedUCI(engine);
		final Thread uciThread = new Thread(uci);
		uciThread.setDaemon(true);
		uciThread.start();
		return uci;
	}

	
	private final BlockingQueue<String> input;
	private final List<String> output;
	private final Map<String, Throwable> exceptions;
	private final AtomicBoolean backgroundRunning = new AtomicBoolean();
	private final AtomicBoolean backgroundStarted = new AtomicBoolean();
	private final AtomicReference<String> lastCommandCompleted = new AtomicReference<>();
	private final Supplier<String> in;
	private Collection<PerfTTestData> testData;
	
	public InstrumentedUCI(Engine defaultEngine) {
		super(defaultEngine);
		input = new LinkedBlockingQueue<String>();
		output = Collections.synchronizedList(new LinkedList<String>());
		this.exceptions = new ConcurrentHashMap<>();
		testData = Collections.emptyList();
		in = () -> {
			try {
				return input.take();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new UncheckedIOException(new EOFException());
			}
		};
	}

	@Override
	protected Supplier<String> getInputSupplier() {
		return in;
	}

	@Override
	protected void out(CharSequence message) {
		output.add(message.toString());
	}

	@Override
	protected void err(String tag, Throwable e) {
		exceptions.put(tag, e);
	}

	@Override
	protected boolean doCommand(String command) {
		boolean known;
		try {
			if (!"q".equals(command) && !"debug on".equals(command)) LOGGER.debug("doCommand computes {}", command);
			known = super.doCommand(command);
			if (!known) {
				exceptions.put(command, new UnknownCommandException(command));
			}
		} catch (Exception e) {
			known = true;
			exceptions.put(command, e);
		}
		synchronized(this) {
			if (!"q".equals(command) && !"debug on".equals(command)) LOGGER.debug("doCommand notifies threads waiting for {} to complete", command);
			lastCommandCompleted.set(command);
			this.notifyAll();
		}
		return known;
	}

	private static class TimeoutException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		TimeoutException(String message) {
			super(message);
		}
	}
	
	public boolean post(String command) {
		if (!"q".equals(command) && !"debug on".equals(command)) LOGGER.debug("{} posted", command);
		lastCommandCompleted.set(null);
		input.add(command);
		try {
			synchronized (this) {
				if (!"q".equals(command) && !"debug on".equals(command)) LOGGER.debug("post is waiting for {} to complete", command);
				this.wait(TIME_OU_MS);
				while (!command.equals(lastCommandCompleted.get())) {
					throw new TimeoutException("Timeout after " + TIME_OU_MS + "ms waiting for " + command + " to complete");
				}
				if (!"q".equals(command) && !"debug on".equals(command)) LOGGER.debug("post received completion notification for {}", command);
				if (exceptions.get(command) instanceof UnknownCommandException) {
					exceptions.remove(command);
						return false;
					}
				}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UncheckedException(e);
		}
		return true;
	}
	
	public void clear() {
		backgroundRunning.set(false);
		backgroundStarted.set(false);
		output.clear();
		exceptions.clear();
	}

	public List<String> out() {
		return output;
	}
	
	public Map<String, Throwable> getExceptions() {
		return exceptions;
	}

	@Override
	public boolean doBackground(ThrowingRunnable task, Runnable stopper, Consumer<Exception> logger) {
		backgroundRunning.set(true);
		backgroundStarted.set(true);
		ThrowingRunnable internalTask = () -> {
			try {
				task.run();
			} finally {
				backgroundRunning.set(false);
			}
		};
		return super.doBackground(internalTask, stopper, logger);
	}
	
	public boolean isBackgroundRunning() {
		return backgroundRunning.get();
	}

	public boolean isBackgroundCompleted() {
		return backgroundStarted.get() && !backgroundRunning.get();
	}

	public void setTestData(Collection<PerfTTestData> testData) {
		this.testData = testData;
	}
	@Override
	protected Collection<PerfTTestData> readTestData() {
		return testData;
	}

	public void assertDebug(String string) {
		assertTrue(isDebug(string), "expected \""+string+"\" started with \"info string \"");
	}

	private boolean isDebug(String string) {
		return string.startsWith("info string ");
	}

	public void assertDebug() {
		output.stream().forEach(this::assertDebug);
	}

}