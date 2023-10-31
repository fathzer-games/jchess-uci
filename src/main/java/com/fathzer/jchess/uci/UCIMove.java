package com.fathzer.jchess.uci;

public class UCIMove {
	private final String from;
	private final String to;
	private final String promotion;
	
	public UCIMove(String from, String to, String promotion) {
		this.from = from;
		this.to = to;
		this.promotion = promotion;
	}

	public static UCIMove from(String uci) {
		try {
			final String from = uci.substring(0, 2);
			final String to = uci.substring(2, 4);
			return new UCIMove(from, to, uci.length()>4 ? uci.substring(4, 5) : null);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	public String getPromotion() {
		return promotion;
	}

	@Override
	public String toString() {
		return from+to+(promotion==null?"":promotion);
	}
}
