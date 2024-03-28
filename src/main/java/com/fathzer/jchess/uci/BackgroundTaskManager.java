package com.fathzer.jchess.uci;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class BackgroundTaskManager implements AutoCloseable {
	static class Task {
		private final Consumer<Exception> logger;
		private final Runnable task;
		private final Runnable stopTask;
		
		Task(Runnable task, Runnable stopTask, Consumer<Exception> logger) {
			this.task = task;
			this.stopTask = stopTask;
			this.logger = logger;
		}
	}
	
	
	private final ExecutorService exec = Executors.newFixedThreadPool(1);
	private final AtomicReference<Task> current = new AtomicReference<>();
	
	boolean doBackground(Task task) {
		final boolean result = this.current.compareAndSet(null, task);
		if (result) {
			exec.submit(() -> {
				try {
					task.task.run();
					this.current.set(null);
				} catch (Exception e) {
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
		final Task stopTask = current.getAndSet(null);
		if (stopTask!=null) {
			try {
				stopTask.stopTask.run();
			} catch (Exception e) {
				stopTask.logger.accept(e);
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
