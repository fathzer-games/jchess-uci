package com.fathzer.jchess.uci.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.fathzer.games.util.UncheckedException;

import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.ThrowingRunnable;
import com.fathzer.jchess.uci.UCI;

public class InstrumentedUCI extends UCI {
	private static final int TIME_OU_MS = 1000;

	public static class UnknownCommandException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private UnknownCommandException(String command) {
			super(command);
		}
	}
	
	private final BlockingQueue<String> input;
	private final List<String> output;
	private final Map<String, Throwable> exceptions;
	private final AtomicBoolean backgroundRunning = new AtomicBoolean();
	
	public InstrumentedUCI(Engine defaultEngine) {
		super(defaultEngine);
		input = new LinkedBlockingQueue<String>();
		output = Collections.synchronizedList(new LinkedList<String>());
		this.exceptions = new ConcurrentHashMap<>();
	}

	@Override
	protected String getNextCommand() {
		try {
			return input.take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
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
			known = super.doCommand(command);
			if (!known) {
				exceptions.put(command, new UnknownCommandException(command));
			}
		} catch (Exception e) {
			known = true;
			exceptions.put(command, e);
		}
		synchronized(this) {
			notifyAll();
		}
		return known;
	}
	
	public boolean post(String command) {
		input.add(command);
		try {
			synchronized (this) {
				wait(TIME_OU_MS);
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
	protected boolean doBackground(ThrowingRunnable task, Runnable stopper, Consumer<Exception> logger) {
		backgroundRunning.set(true);
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
}