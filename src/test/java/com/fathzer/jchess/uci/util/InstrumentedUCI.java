package com.fathzer.jchess.uci.util;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.fathzer.games.util.UncheckedException;

import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.UCI;

public class InstrumentedUCI extends UCI {
	public static class UnknownCommandException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private UnknownCommandException(String command) {
			super(command);
		}
	}
	
	private final BlockingQueue<String> input;
	private final BlockingQueue<String> output;
	private final BlockingQueue<String> debug;
	private final Map<String, Throwable> exceptions;
	
	public InstrumentedUCI(Engine defaultEngine) {
		super(defaultEngine);
		input = new LinkedBlockingQueue<String>();
		output = new LinkedBlockingQueue<String>();
		debug = new LinkedBlockingQueue<String>();
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
	protected void debug(CharSequence message) {
		debug.add(message.toString());
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
	
	public boolean post(String command, long timeOutMS) {
		input.add(command);
		try {
			synchronized (this) {
				wait(timeOutMS);
				final Throwable e = exceptions.get(command);
				if (e!=null) {
					if (e instanceof UnknownCommandException) {
						return false;
					} else {
						throw new UncheckedException(e);
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UncheckedException(e);
		} finally {
			exceptions.remove(command);
		}
		return true;
	}
	
	public void clear() {
		debug.clear();
		output.clear();
		exceptions.clear();
	}

	public BlockingQueue<String> getOutput() {
		return output;
	}

	public BlockingQueue<String> getDebug() {
		return debug;
	}
}