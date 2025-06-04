package com.fathzer.jchess.uci.parameters;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import com.fathzer.games.MoveGenerator;

/** The parameters of the <i>perft</i> UCI command.
 * <br>List of supported parameters:
 * <ul>
 * <li><b>depth</b>: The depth of the search</li>
 * <li><b>threads</b> (shortcut <b>t</b>): The number of threads to use</li>
 * <li><b>legal</b> (shortcut <b>l</b>): Resquest legal moves from the {@link MoveGenerator} instead of pseudo legal moves</li>
 * <li><b>playleaves</b> (shortcut <b>pl</b>): Play the leave moves when used with <i>legal</i> option. With pseudo legal moves, leave moves are always played (it's the only way to know if they are legal)</li>
 * </ul>
 */
public class PerfTParameters {
	/** A {@link Parser} for the <i>perft</i> command. */
	public static final Parser<PerfTParameters> PARSER = new PerfTLikeParser<>();
	
	/** A {@link Parser} for the <i>perft</i> command. */
	public static class PerfTLikeParser<T extends PerfTParameters> extends Parser<T> {
		/** Constructor */
		protected PerfTLikeParser() {
			super(Collections.emptyList());
			add(new ParameterDefinition<>((p,tok) -> p.setParallelism(Parser.positiveInt(tok)), "threads", "t"));
			add(new ParameterDefinition<>((p,tok) -> p.setLegal(true), "legal", "l"));
			add(new ParameterDefinition<>((p,tok) -> p.setPlayLeaves(true), "playleaves", "pl"));
		}

		@Override
		public List<String> parse(T target, Deque<String> tokens) {
			target.setDepth(positiveInt(tokens));
			return super.parse(target, tokens);
		}
	}

	private int depth = -1;
	private int parallelism = 1;
	private boolean legal = false;
	private boolean playLeaves = false;
	
	/** Gets the <i>depth</i> option.
	 * @return -1 if the option is not set
	 */
	public int getDepth() {
		return depth;
	}

	void setDepth(int depth) {
		if (depth<1) {
			throw new IllegalArgumentException();
		}
		this.depth = depth;
	}

	/** Gets the <i>threads</i> option.
	 * @return 1 if the option is not set
	 */
	public int getParallelism() {
		return parallelism;
	}

	void setParallelism(int parallelism) {
		if (parallelism<1) {
			throw new IllegalArgumentException();
		}
		this.parallelism = parallelism;
	}
	
	/** Gets the <i>legal</i> option.
 	 * @return false if the option is not set
	 */
	public boolean isLegal() {
		return legal;
	}

	void setLegal(boolean legal) {
		this.legal = legal;
	}

	/** Gets the <i>playleaves</i> option.
	 * @return false if the option is not set
	 */
	public boolean isPlayLeaves() {
		return playLeaves;
	}
	void setPlayLeaves(boolean playLeaves) {
		this.playLeaves = playLeaves;
	}
}
