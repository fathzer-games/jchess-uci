package com.fathzer.jchess.uci.util;

import java.util.function.Consumer;
import java.util.function.Function;

import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.GoReply;
import com.fathzer.jchess.uci.LongRunningTask;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.parameters.GoParameters;

public class InstrumentedEngine implements Engine {
	private Consumer<String> positionConsumer;
	private Consumer<UCIMove> moveConsumer;
	private Function<GoParameters, LongRunningTask<GoReply>> goFunction;

	@Override
	public String getId() {
		return "InstrumentedEngine";
	}

	@Override
	public void setStartPosition(String fen) {
		positionConsumer.accept(fen);
	}

	@Override
	public void move(UCIMove move) {
		moveConsumer.accept(move);
	}

	@Override
	public LongRunningTask<GoReply> go(GoParameters params) {
		return goFunction.apply(params);
	}

	public void setPositionConsumer(Consumer<String> positionConsumer) {
		this.positionConsumer = positionConsumer;
	}

	public void setMoveConsumer(Consumer<UCIMove> moveConsumer) {
		this.moveConsumer = moveConsumer;
	}

	public void setGoFunction(Function<GoParameters, LongRunningTask<GoReply>> goFunction) {
		this.goFunction = goFunction;
	}
	
	public void clear() {
		this.goFunction = null;
		this.moveConsumer = null;
		this.positionConsumer = null;
	}
}