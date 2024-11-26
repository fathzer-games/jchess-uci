package com.fathzer.jchess.uci;

@FunctionalInterface
/** A runnable that can throw an exception.
 */
public interface ThrowingRunnable {
	void run() throws Exception;
}