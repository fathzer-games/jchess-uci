package com.fathzer.jchess.uci;

import java.util.Objects;

/** The UCI representation of a move. */
public class UCIMove {
	private final String from;
	private final String to;
	private final String promotion;
	
	/**
	 * Constructor of a move that is not a promotion.
	 * @param from the origin square (e.g. "e2")
	 * @param to the destination square (e.g. "e4")
	 */
	public UCIMove(String from, String to) {
		this(from, to, null);
	}

	/**
	 * Constructor.
	 * @param from the origin square (e.g. "e2")
	 * @param to the destination square (e.g. "e4")
	 * @param promotion the promotion piece (e.g. "q"), or null if the move is not a promotion
	 */
	public UCIMove(String from, String to, String promotion) {
		if (from==null || to==null) {
			throw new IllegalArgumentException();
		}
		this.from = from;
		this.to = to;
		this.promotion = promotion;
	}

	/**
	 * Parses a UCI move.
	 * @param uci the UCI move to parse (e.g. "e2e4", "e2e4q")
	 * @return the parsed move
	 * @throws IllegalArgumentException if the move is not a valid UCI move
	 */
	public static UCIMove from(String uci) {
		try {
			final String from = uci.substring(0, 2);
			final String to = uci.substring(2, 4);
			return new UCIMove(from, to, uci.length()>4 ? uci.substring(4, 5) : null);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Returns the origin square of the move.
	 * @return the origin square (e.g. "e2")
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Returns the destination square of the move.
	 * @return the destination square (e.g. "e4")
	 */
	public String getTo() {
		return to;
	}

	/**
	 * Returns the promotion piece of the move.
 	* @return the promotion piece (e.g. "q"), or null if the move is not a promotion
 	*/
	public String getPromotion() {
		return promotion;
	}

	/** {@inheritDoc}
	 * @return the move in UCI format (e.g. "e2e4", "e7e8q")
	 */
	@Override
	public String toString() {
		return from+to+(promotion==null?"":promotion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, promotion, to);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final UCIMove other = (UCIMove) obj;
		return from.equals(other.from) && to.equals(other.to) && Objects.equals(promotion, other.promotion);
	}
}
