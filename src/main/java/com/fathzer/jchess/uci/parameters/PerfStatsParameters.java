package com.fathzer.jchess.uci.parameters;

/** The parameters of the <i>test</i> UCI command.
 */
public class PerfStatsParameters extends PerfTParameters {
	public static final Parser<PerfStatsParameters> PARSER;
	
	static {
		PARSER = new PerfTLikeParser<>();
		PARSER.add(new ParamProperties<>((p,tok) -> p.cutTime=Parser.positiveInt(tok), "cut"));
	}

	private int cutTime = Integer.MAX_VALUE;
	
	/** Gets the <i>cutTime</i> option.
	 * @return Integer.MAX_VALUE if the option is not set
	 */
	public int getCutTime() {
		return cutTime;
	}
}
