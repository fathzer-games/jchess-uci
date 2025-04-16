package com.fathzer.jchess.uci.util;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
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
	private final List<String> output;
	private final Map<String, Throwable> exceptions;
	
	public InstrumentedUCI(Engine defaultEngine) {
		super(defaultEngine);
		input = new LinkedBlockingQueue<String>();
		output = Collections.synchronizedList(new LinkedList<String>());
		this.exceptions = new ConcurrentHashMap<>();
	}

	@Override
	protected String getNextCommand() {
		try {
			final String command = input.take();
			if (debug) System.err.println("InstrumentedUCI.getNextCommand gets "+command);
			return command;
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
		System.err.println("InstrumentedUCI.err is called on thread "+Thread.currentThread());
		exceptions.put(tag, e);
	}

	@Override
	protected boolean doCommand(String command) {
		boolean known;
		try {
			known = super.doCommand(command);
			if (!known) {
				System.err.println("InstrumentedUCI.doCommand adds UnknownCommandException with tag "+command);
				exceptions.put(command, new UnknownCommandException(command));
			}
		} catch (Exception e) {
			known = true;
			if (debug) System.err.println("Exception "+e.getClass().getName()+" caught by doCommand on command "+command+" at "+System.currentTimeMillis());
			exceptions.put(command, e);
		}
		synchronized(this) {
			if (debug) System.err.println("Command "+command+" completed on thread "+Thread.currentThread()+" at "+System.currentTimeMillis());
			notifyAll();
		}
		return known;
	}
	
	@Override
	protected void doGo(Deque<String> tokens) {
		if (debug) System.err.println("doGo invoked");
		super.doGo(tokens);
		if (debug) System.err.println("doGo finished");
	}

	public boolean debug = false;
	
	public boolean post(String command, long timeOutMS) {
		if (!exceptions.isEmpty() && debug) {
			System.err.println("Warning exception is not empty");
		}
		input.add(command);
		try {
			synchronized (this) {
				if (debug) System.err.println("InstrumentedUCI.post is pausing thread to wait for result "+Thread.currentThread()+" for "+timeOutMS+" at "+System.currentTimeMillis());
				wait(timeOutMS);
				if (debug) System.err.println("InstrumentedUCI.post is resuming thread "+Thread.currentThread()+" at "+System.currentTimeMillis());
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
		if (debug && !exceptions.isEmpty()) System.err.println("Clear is called");
		output.clear();
		exceptions.clear();
	}

	public List<String> out() {
		return output;
	}
	
	public Map<String, Throwable> getExceptions() {
		return exceptions;
	}
}