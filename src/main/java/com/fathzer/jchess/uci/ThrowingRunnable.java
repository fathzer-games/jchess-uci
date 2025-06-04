package com.fathzer.jchess.uci;

/** A runnable that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingRunnable {
	/** Runs the task.
	 * @throws Exception if an exception occurs
	 */
	void run() throws Exception;
}