package com.fathzer.jchess.uci.util;

import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.GoReply;
import com.fathzer.jchess.uci.LongRunningTask;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.parameters.GoParameters;

public class InstrumentedEngine implements Engine {

	@Override
	public String getId() {
		return "My Engine";
	}

	@Override
	public void setStartPosition(String fen) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(UCIMove move) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public LongRunningTask<GoReply> go(GoParameters params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPositionSet() {
		// TODO Auto-generated method stub
		return false;
	}
	
}