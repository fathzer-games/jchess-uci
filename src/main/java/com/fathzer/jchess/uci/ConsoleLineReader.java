package com.fathzer.jchess.uci;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

class ConsoleLineReader implements Supplier<String> {
    private final BufferedReader in;

    public ConsoleLineReader() {
        this.in = System.console() == null ? new BufferedReader(new InputStreamReader(System.in)) : null;
    }
    
    @Override
	public String get() {
		String line;
	    try {
	        line = in!=null ? in.readLine() : System.console().readLine();
	        if (line==null) {
	        	throw new EOFException("End of system input has been reached");
	        }
	    } catch (IOException e) {
	    	throw new UncheckedIOException(e);
	    }
	    return line;
    }
}

