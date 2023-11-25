package com.fathzer.jchess.uci.parameters;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** The parameters of the <i>perft</i> UCI command.
 */
public class PerfTParameters {
	public static final Parser<PerfTParameters> PARSER = new PerfTLikeParser<>();
	
	protected static class PerfTLikeParser<T extends PerfTParameters> extends Parser<T> {
		protected PerfTLikeParser() {
			super(Collections.emptyList());
			add(new ParamProperties<>((p,tok) -> p.setParallelism(Parser.positiveInt(tok)), "threads", "t"));
			add(new ParamProperties<>((p,tok) -> p.setLegal(true), "legal", "l"));
			add(new ParamProperties<>((p,tok) -> p.setPlayLeaves(true), "playleaves", "pl"));
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
		this.depth = depth;
	}

	/** Gets the <i>threads</i> option.
	 * @return 1 if the option is not set
	 */
	public int getParallelism() {
		return parallelism;
	}

	protected void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}
	
	protected void setLegal(boolean legal) {
		this.legal = legal;
	}

	public boolean isPlayLeaves() {
		return playLeaves;
	}

	protected void setPlayLeaves(boolean playLeaves) {
		this.playLeaves = playLeaves;
	}

	public boolean isLegal() {
		return legal;
	}
}
