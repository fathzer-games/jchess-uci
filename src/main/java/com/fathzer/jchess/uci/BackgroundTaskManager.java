package com.fathzer.jchess.uci;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class BackgroundTaskManager implements AutoCloseable {
	private final ExecutorService exec = Executors.newFixedThreadPool(1);
	private final AtomicReference<Runnable> stopper = new AtomicReference<>();
	private final Consumer<Exception> logger;
	
	public BackgroundTaskManager(Consumer<Exception> logger) {
		this.logger = logger;
	}
	
	boolean doBackground(Runnable task, Runnable stopTask) {
		final boolean result = this.stopper.compareAndSet(null, stopTask);
		if (result) {
			exec.submit(() -> {
				try {
					task.run();
					this.stopper.set(null);
				} catch (Exception e) {
					logger.accept(e);
					stop();
				}
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
			try {
				stopTask.run();
			} catch (Exception e) {
				logger.accept(e);
			}
		}
		return stopTask!=null;
	}
	
	@Override
	public void close() {
		stop();
		exec.shutdown();
	}
}
