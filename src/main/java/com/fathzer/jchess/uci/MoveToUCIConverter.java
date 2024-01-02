package com.fathzer.jchess.uci;

public interface MoveToUCIConverter<M> {
	UCIMove toUCI(M move);
}
