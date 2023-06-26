package com.fathzer.jchess.uci;

import java.util.concurrent.atomic.AtomicBoolean;

/** A task that will be executed in the background of UCI interface.
 * @param <T> The result of the task
 */
public abstract class LongRunningTask<T> {
	private final AtomicBoolean stopped;
	
	protected LongRunningTask() {
		stopped = new AtomicBoolean();
	}

	public abstract T get();

	public boolean isStopped() {
		return stopped.get();
	}
	
	public void stop() {
		stopped.set(true);
	}
}
