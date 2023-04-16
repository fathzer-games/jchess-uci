package com.fathzer.jchess.uci;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

class BackgroundTaskManager implements AutoCloseable {
	private final ExecutorService exec = Executors.newFixedThreadPool(1);
	private final AtomicReference<Runnable> stopper = new AtomicReference<>();
	
	boolean doBackground(Runnable task, Runnable stopTask) {
		final boolean result = this.stopper.compareAndSet(null, stopTask);
		if (result) {
			exec.submit(() -> {
				task.run();
				this.stopper.set(null);
			});
		}
		return result;
	}
	
	/** Stops the currently executed task
	 * @return true if a task was executed.
	 */
	boolean stop() {
		final Runnable stopTask = stopper.getAndSet(null);
		if (stopTask!=null) {
			stopTask.run();
		}
		return stopTask!=null;
	}
	
	public void close() {
		stop();
		exec.shutdown();
	}
}
