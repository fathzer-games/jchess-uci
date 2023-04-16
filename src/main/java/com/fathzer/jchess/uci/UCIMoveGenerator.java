package com.fathzer.jchess.uci;

import com.fathzer.games.MoveGenerator;

public interface UCIMoveGenerator<M> extends MoveGenerator<M> {
	UCIMove toUCI(M move);
}
