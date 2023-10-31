package com.fathzer.jchess.uci;

import java.util.Optional;

public class BestMoveReply {
	private final UCIMove move;
	private final UCIMove ponderMove;
	
	public BestMoveReply(UCIMove move) {
		this(move, null);
	}
	public BestMoveReply(UCIMove move, UCIMove ponderMove) {
		this.move = move;
		this.ponderMove = ponderMove;
	}
	public UCIMove getMove() {
		return move;
	}
	public Optional<UCIMove> getPonderMove() {
		return Optional.ofNullable(ponderMove);
	}
}
