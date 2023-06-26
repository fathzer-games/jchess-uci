package com.fathzer.jchess.uci;

public interface MoveToUCIConverter<M> {
	String toUCI(M move);
}
