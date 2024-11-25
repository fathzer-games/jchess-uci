package com.fathzer.jchess.uci;

import java.util.concurrent.Callable;

/** A task that can be stopped.
 * <br>Please note that stoppable is different from {@link java.util.concurrent.Cancellable}
 * When a task is cancelled, it produces no result (for example, a {@link java.util.concurrent.CancellationException} if the task was cancelled).
 * The typical use case of a StoppableTask is a best move search engine that performs iterative deepening. You may want to stop its deepening and get the current result.  
 * @param <T> The result of the task
 */
public interface StoppableTask<T> extends Callable<T> {
	/** Stops the task.
	 */
	void stop();
}
